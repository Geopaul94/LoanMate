package com.loanmate.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.drive.DriveAuthManager
import com.loanmate.data.drive.DriveBackupRepository
import com.loanmate.data.drive.DriveBackupRepository.Outcome
import com.loanmate.utils.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class DriveUiState(
    val isConfigured: Boolean = false,
    val accountEmail: String? = null,
    val isBusy: Boolean = false,
    val backups: List<DriveBackupRepository.RemoteBackup> = emptyList(),
    val lastError: String? = null
)

@HiltViewModel
class DriveBackupViewModel @Inject constructor(
    private val auth: DriveAuthManager,
    private val drive: DriveBackupRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    private val _backups = MutableStateFlow<List<DriveBackupRepository.RemoteBackup>>(emptyList())
    private val _lastError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DriveUiState> = combine(
        auth.account, _isBusy, _backups, _lastError
    ) { account, busy, backups, error ->
        DriveUiState(
            isConfigured = auth.isConfigured,
            accountEmail = account?.email,
            isBusy = busy,
            backups = backups,
            lastError = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriveUiState())

    fun signInIntent(): Intent = auth.signInIntent()

    fun handleSignInResult(data: Intent?) {
        val result = auth.onSignInResult(data)
        if (result.isFailure) {
            _lastError.value = "Sign-in failed: ${result.exceptionOrNull()?.message ?: "unknown"}"
        } else {
            _lastError.value = null
            refreshBackups()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            _backups.value = emptyList()
        }
    }

    fun backupNow(context: Context) {
        val account = auth.account.value ?: run {
            _lastError.value = "Sign in first"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _lastError.value = null
            try {
                val local = backupManager.export(context)
                val json = local.file.readBytes()
                when (val result = drive.upload(account, local.file.name, json)) {
                    is Outcome.Success -> refreshBackups()
                    is Outcome.Failure -> _lastError.value = "Drive upload: ${result.reason}"
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun refreshBackups() {
        val account = auth.account.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            when (val result = drive.listBackups(account)) {
                is Outcome.Success -> _backups.value = result.value
                is Outcome.Failure -> _lastError.value = "Couldn't list backups: ${result.reason}"
            }
            _isBusy.value = false
        }
    }

    fun restoreFromDrive(fileId: String) {
        val account = auth.account.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            _lastError.value = null
            try {
                when (val result = drive.download(account, fileId)) {
                    is Outcome.Success -> {
                        when (val outcome = backupManager.restore(result.value)) {
                            is BackupManager.RestoreOutcome.Failure ->
                                _lastError.value = outcome.reason
                            is BackupManager.RestoreOutcome.Success -> { /* snackbar handled by collector */ }
                        }
                    }
                    is Outcome.Failure -> _lastError.value = "Download: ${result.reason}"
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun formatBackupTime(ms: Long): String =
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(java.util.Date(ms))
}
