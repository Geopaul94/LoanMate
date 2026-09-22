package com.loanmate.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.drive.ProductionBackupManager
import com.loanmate.data.repository.LoanRepository
import com.loanmate.utils.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class BackupEvent {
    data class SharePdf(val authority: String, val file: File) : BackupEvent()
    data class ShareBackup(val authority: String, val file: File) : BackupEvent()
    data class Toast(val message: String) : BackupEvent()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val productionBackup: ProductionBackupManager,
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<BackupEvent>()
    val events: SharedFlow<BackupEvent> = _events.asSharedFlow()

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            try {
                val loans = loanRepository.getAllLoans().first()
                if (loans.isEmpty()) {
                    _events.emit(BackupEvent.Toast("No loans to export"))
                    return@launch
                }
                val file = PdfExporter.export(context, loans)
                _events.emit(BackupEvent.SharePdf(authority(context), file))
            } catch (e: Exception) {
                _events.emit(BackupEvent.Toast("Export failed: ${e.message ?: "unknown"}"))
            }
        }
    }

    fun exportAmortization(context: Context, loan: com.loanmate.data.local.LoanEntity, schedule: List<com.loanmate.utils.AmortizationCalculator.AmortizationMonth>) {
        viewModelScope.launch {
            try {
                val file = PdfExporter.exportAmortization(context, loan, schedule)
                _events.emit(BackupEvent.SharePdf(authority(context), file))
            } catch (e: Exception) {
                _events.emit(BackupEvent.Toast("Export failed: ${e.message ?: "unknown"}"))
            }
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch {
            try {
                val file = productionBackup.prepareBackupPackage()
                _events.emit(BackupEvent.Toast("Backup package created successfully"))
                _events.emit(BackupEvent.ShareBackup(authority(context), file))
            } catch (e: Exception) {
                _events.emit(BackupEvent.Toast("Backup failed: ${e.message ?: "unknown"}"))
            }
        }
    }

    fun restoreBackup(zipFile: File) {
        viewModelScope.launch {
            try {
                productionBackup.restoreFromPackage(zipFile)
                _events.emit(BackupEvent.Toast("Data restored successfully!"))
            } catch (e: Exception) {
                _events.emit(BackupEvent.Toast("Restore failed: ${e.message ?: "unknown"}"))
            }
        }
    }

    private fun authority(context: Context) = "${context.packageName}.fileprovider"
}
