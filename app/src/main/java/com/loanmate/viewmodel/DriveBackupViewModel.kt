package com.loanmate.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.drive.DriveAuthManager
import com.loanmate.data.drive.DriveBackupRepository
import com.loanmate.data.drive.DriveBackupRepository.Outcome
import com.loanmate.data.drive.ProductionBackupManager
import com.loanmate.data.local.LoanDatabase
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
    val lastError: String? = null,
    val showShrinkWarning: ShrinkWarning? = null
)

data class ShrinkWarning(
    val localCount: Int,
    val remoteCount: Int,
    val fileId: String? = null // if null, it's for the 'upload' action
)

@HiltViewModel
class DriveBackupViewModel @Inject constructor(
    private val auth: DriveAuthManager,
    private val drive: DriveBackupRepository,
    private val productionBackup: ProductionBackupManager,
    private val db: LoanDatabase
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    private val _backups = MutableStateFlow<List<DriveBackupRepository.RemoteBackup>>(emptyList())
    private val _lastError = MutableStateFlow<String?>(null)
    private val _shrinkWarning = MutableStateFlow<ShrinkWarning?>(null)

    val uiState: StateFlow<DriveUiState> = combine(
        auth.account, _isBusy, _backups, _lastError, _shrinkWarning
    ) { account, busy, backups, error, shrink ->
        DriveUiState(
            isConfigured = auth.isConfigured,
            accountEmail = account?.email,
            isBusy = busy,
            backups = backups,
            lastError = error,
            showShrinkWarning = shrink
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriveUiState())

    fun signInIntent() = auth.signInIntent()

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

    fun backupNow(force: Boolean = false) {
        val account = auth.account.value ?: run {
            _lastError.value = "Sign in first"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _lastError.value = null
            _shrinkWarning.value = null
            try {
                val localCount = db.loanDao().getAllLoansOnce().size
                if (localCount == 0) {
                    _lastError.value = "Cannot backup empty data"
                    return@launch
                }

                if (!force) {
                    when (val result = drive.listBackups(account)) {
                        is Outcome.Success -> {
                            val latest = result.value.firstOrNull()
                            if (latest != null && latest.entryCount != null && latest.entryCount > localCount) {
                                _shrinkWarning.value = ShrinkWarning(localCount, latest.entryCount)
                                return@launch
                            }
                        }
                        else -> { /* proceed if list fails */ }
                    }
                }

                val backupFile = productionBackup.prepareBackupPackage()
                when (val result = drive.upload(account, backupFile, localCount)) {
                    is Outcome.Success -> refreshBackups()
                    is Outcome.Failure -> _lastError.value = "Drive upload: ${result.reason}"
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun dismissShrinkWarning() {
        _shrinkWarning.value = null
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
                        productionBackup.restoreFromPackage(result.value)
                        _lastError.value = "Restore successful!"
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
