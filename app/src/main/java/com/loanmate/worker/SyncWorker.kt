package com.loanmate.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.loanmate.data.drive.BackupOutcome
import com.loanmate.data.drive.DriveBackupManager
import com.loanmate.data.drive.GoogleDriveAuthManager
import com.loanmate.data.repository.LoanRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authManager: GoogleDriveAuthManager,
    private val driveBackupManager: DriveBackupManager,
    private val loanRepository: LoanRepository,
    private val dataStore: DataStore<Preferences>
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "google_drive_sync_worker"
        private val KEY_DRIVE_ENABLED = booleanPreferencesKey("drive_backup_enabled")
        private val KEY_LAST_DRIVE_SYNC = longPreferencesKey("last_drive_sync_timestamp")

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun runOnce(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override suspend fun doWork(): Result {
        val driveEnabled = dataStore.data.first()[KEY_DRIVE_ENABLED] ?: false
        if (!driveEnabled) return Result.success()

        val account = authManager.lastSignedInAccount ?: return Result.failure()

        // Auto-purge soft-deleted items older than 30 days
        try {
            loanRepository.purgeOldDeleted()
        } catch (e: Exception) {
            Log.w("SyncWorker", "Purge old deleted loans failed", e)
        }

        val driveService = authManager.getDriveService(account)

        return when (driveBackupManager.createOrReplaceBackup(driveService)) {
            BackupOutcome.SUCCESS -> {
                dataStore.edit { prefs ->
                    prefs[KEY_LAST_DRIVE_SYNC] = System.currentTimeMillis()
                }
                Result.success()
            }
            BackupOutcome.SKIPPED_EMPTY -> Result.success()
            BackupOutcome.SKIPPED_SHRINK -> Result.success()
            BackupOutcome.FAILED -> Result.retry()
        }
    }
}
