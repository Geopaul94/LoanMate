package com.loanmate.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.loanmate.data.drive.DriveAuthManager
import com.loanmate.data.drive.DriveBackupRepository
import com.loanmate.data.drive.ProductionBackupManager
import com.loanmate.data.local.LoanDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DriveSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val auth: DriveAuthManager,
    private val drive: DriveBackupRepository,
    private val productionBackup: ProductionBackupManager,
    private val db: LoanDatabase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val account = auth.account.value ?: return Result.success()

        return try {
            val localCount = db.loanDao().getAllLoansOnce().size
            if (localCount == 0) return Result.success()

            // 1. Shrink Guard for background sync
            when (val listResult = drive.listBackups(account)) {
                is DriveBackupRepository.Outcome.Success -> {
                    val latest = listResult.value.firstOrNull()
                    if (latest != null && latest.entryCount != null && latest.entryCount > localCount) {
                        // Safety: don't overwrite a larger cloud backup in the background
                        return Result.success()
                    }
                }
                else -> { /* proceed if list fails */ }
            }

            // 2. Perform background backup
            val backupFile = productionBackup.prepareBackupPackage()
            when (drive.upload(account, backupFile, localCount)) {
                is DriveBackupRepository.Outcome.Success -> Result.success()
                is DriveBackupRepository.Outcome.Failure -> Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
