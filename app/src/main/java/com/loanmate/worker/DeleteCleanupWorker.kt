package com.loanmate.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.loanmate.data.repository.LoanRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Runs after the undo window expires.
 * Hard-deletes the loan if it's still marked isDeleted = 1.
 * If the user tapped Undo, the row was already restored (isDeleted = 0)
 * and the hard-delete query becomes a no-op.
 */
@HiltWorker
class DeleteCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val loanRepository: LoanRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val loanId = inputData.getLong(KEY_LOAN_ID, -1L)
        if (loanId <= 0) return Result.success()
        loanRepository.hardDeleteLoan(loanId)
        // Also cancel any pending EMI reminders for this loan
        EmiReminderWorker.cancelReminders(applicationContext, loanId)
        return Result.success()
    }

    companion object {
        const val KEY_LOAN_ID = "loan_id"
        const val UNDO_WINDOW_SECONDS = 6L

        fun schedule(context: Context, loanId: Long) {
            val request = OneTimeWorkRequestBuilder<DeleteCleanupWorker>()
                .setInitialDelay(UNDO_WINDOW_SECONDS, TimeUnit.SECONDS)
                .setInputData(workDataOf(KEY_LOAN_ID to loanId))
                .addTag("delete_cleanup_$loanId")
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun cancel(context: Context, loanId: Long) {
            WorkManager.getInstance(context).cancelAllWorkByTag("delete_cleanup_$loanId")
        }
    }
}
