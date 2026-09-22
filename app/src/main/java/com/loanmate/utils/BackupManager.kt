package com.loanmate.utils

import android.content.Context
import android.net.Uri
import com.loanmate.data.drive.ProductionBackupManager
import com.loanmate.data.repository.LoanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productionBackup: ProductionBackupManager,
    private val loanRepository: LoanRepository
) : BackupService {

    override suspend fun exportToUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val loans = loanRepository.getAllLoansOnce()
            val backupZip = productionBackup.prepareBackupPackage()

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                backupZip.inputStream().use { input ->
                    input.copyTo(stream)
                }
            }
            backupZip.delete()
            Result.success(loans.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "import_temp.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Could not open file"))

            productionBackup.restoreFromPackage(tempFile)
            tempFile.delete()

            val loans = loanRepository.getAllLoansOnce()
            Result.success(loans.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
