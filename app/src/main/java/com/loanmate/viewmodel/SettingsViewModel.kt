package com.loanmate.viewmodel

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.loanmate.data.drive.BackupOutcome
import com.loanmate.data.drive.DriveBackupManager
import com.loanmate.data.drive.GoogleDriveAuthManager
import com.loanmate.data.drive.RestoreResult
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.repository.LoanRepository
import com.loanmate.utils.BackupService
import com.loanmate.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val hideValues: Boolean = false,
    val isDriveEnabled: Boolean = false,
    val lastDriveSync: Long = 0L,
    val googleAccount: GoogleSignInAccount? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupService: BackupService,
    private val dataStore: DataStore<Preferences>,
    private val repository: LoanRepository,
    val authManager: GoogleDriveAuthManager,
    private val driveBackupManager: DriveBackupManager
) : ViewModel() {

    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
    private val KEY_HIDE_VALUES = booleanPreferencesKey("hide_values")
    private val KEY_DRIVE_ENABLED = booleanPreferencesKey("drive_backup_enabled")
    private val KEY_LAST_DRIVE_SYNC = longPreferencesKey("last_drive_sync_timestamp")

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val trashLoans: StateFlow<List<LoanEntity>> = repository.observeTrashLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreFromTrash(id: Long) {
        viewModelScope.launch {
            repository.restoreLoan(id)
            _uiState.update { it.copy(statusMessage = "Restored loan from trash") }
        }
    }

    fun permanentlyDeleteFromTrash(id: Long) {
        viewModelScope.launch {
            repository.hardDeleteLoan(id)
            _uiState.update { it.copy(statusMessage = "Permanently deleted loan") }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            _uiState.update { it.copy(statusMessage = "Trash bin emptied") }
        }
    }

    fun restoreAllFromTrash() {
        viewModelScope.launch {
            repository.restoreAllFromTrash()
            _uiState.update { it.copy(statusMessage = "Restored all loans from trash") }
        }
    }

    init {
        viewModelScope.launch {
            combine(
                dataStore.data.map { it[KEY_DARK_MODE] ?: false },
                dataStore.data.map { it[KEY_NOTIFICATIONS] ?: true },
                dataStore.data.map { it[KEY_BIOMETRIC] ?: false },
                combine(
                    dataStore.data.map { it[KEY_HIDE_VALUES] ?: false },
                    dataStore.data.map { it[KEY_DRIVE_ENABLED] ?: false },
                    dataStore.data.map { it[KEY_LAST_DRIVE_SYNC] ?: 0L }
                ) { hideValues, driveEnabled, lastSync ->
                    Triple(hideValues, driveEnabled, lastSync)
                }
            ) { darkMode, notifications, biometric, extra ->
                val (hideValues, driveEnabled, lastSync) = extra
                SettingsUiState(
                    isDarkMode = darkMode,
                    notificationsEnabled = notifications,
                    biometricEnabled = biometric,
                    hideValues = hideValues,
                    isDriveEnabled = driveEnabled,
                    lastDriveSync = lastSync,
                    googleAccount = authManager.lastSignedInAccount
                )
            }.collect { newState ->
                _uiState.update { current ->
                    newState.copy(
                        isProcessing = current.isProcessing,
                        statusMessage = current.statusMessage
                    )
                }
            }
        }
    }

    fun setDarkMode(value: Boolean) = setBoolean(KEY_DARK_MODE, value)
    fun setNotifications(value: Boolean) = setBoolean(KEY_NOTIFICATIONS, value)
    fun setBiometric(value: Boolean) = setBoolean(KEY_BIOMETRIC, value)
    fun setHideValues(value: Boolean) = setBoolean(KEY_HIDE_VALUES, value)

    private fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[key] = value } }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun exportData(uri: Uri) {
        _uiState.update { it.copy(isProcessing = true, statusMessage = null) }
        viewModelScope.launch {
            backupService.exportToUri(uri).fold(
                onSuccess = { count ->
                    _uiState.update {
                        it.copy(isProcessing = false, statusMessage = "Exported $count loans")
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isProcessing = false, statusMessage = "Export failed: ${e.message}")
                    }
                }
            )
        }
    }

    fun importData(uri: Uri) {
        _uiState.update { it.copy(isProcessing = true, statusMessage = null) }
        viewModelScope.launch {
            backupService.importFromUri(uri).fold(
                onSuccess = { count ->
                    _uiState.update {
                        it.copy(isProcessing = false, statusMessage = "Imported $count loans")
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isProcessing = false, statusMessage = "Import failed: ${e.message}")
                    }
                }
            )
        }
    }

    fun setGoogleAccount(context: Context, account: GoogleSignInAccount?) {
        viewModelScope.launch {
            if (account != null) {
                dataStore.edit { it[KEY_DRIVE_ENABLED] = true }
                SyncWorker.enqueue(context)
                SyncWorker.runOnce(context)
                _uiState.update { it.copy(googleAccount = account, isDriveEnabled = true) }
            } else {
                dataStore.edit { it[KEY_DRIVE_ENABLED] = false }
                _uiState.update { it.copy(googleAccount = null, isDriveEnabled = false) }
            }
        }
    }

    fun toggleDriveBackup(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_DRIVE_ENABLED] = enabled }
            _uiState.update { it.copy(isDriveEnabled = enabled) }
            if (enabled) {
                SyncWorker.enqueue(context)
                SyncWorker.runOnce(context)
            }
        }
    }

    fun triggerDriveBackup(context: Context) {
        val account = uiState.value.googleAccount
        if (account == null) {
            _uiState.update { it.copy(statusMessage = "Please connect your Google account first") }
            return
        }
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val driveService = authManager.getDriveService(account)
            val result = driveBackupManager.createOrReplaceBackup(driveService, allowShrink = true)

            val message = when (result) {
                BackupOutcome.SUCCESS -> "Cloud backup saved successfully"
                BackupOutcome.SKIPPED_EMPTY -> "Add at least 1 loan before creating a backup"
                BackupOutcome.SKIPPED_SHRINK -> "Cloud backup is newer than local data. Use Restore to download it"
                BackupOutcome.FAILED -> "Cloud backup failed. Check your connection."
            }

            if (result == BackupOutcome.SUCCESS) {
                dataStore.edit { it[KEY_LAST_DRIVE_SYNC] = System.currentTimeMillis() }
            }

            _uiState.update { it.copy(isProcessing = false, statusMessage = message) }
        }
    }

    fun triggerDriveRestore(context: Context) {
        val account = uiState.value.googleAccount
        if (account == null) {
            _uiState.update { it.copy(statusMessage = "Please connect your Google account first") }
            return
        }
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val driveService = authManager.getDriveService(account)
            val result = driveBackupManager.restoreLatestBackup(driveService)

            val message = when (result) {
                RestoreResult.SUCCESS -> "Restored successfully from cloud backup"
                RestoreResult.NO_BACKUP_FOUND -> "No backup file found in Google Drive for this account"
                RestoreResult.FAILED -> "Cloud restore failed. Check your connection."
            }
            _uiState.update { it.copy(isProcessing = false, statusMessage = message) }
        }
    }
}
