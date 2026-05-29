package com.loanmate.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.loanmate.LoanMateApp
import com.loanmate.MainActivity
import com.loanmate.data.repository.LoanRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

@HiltWorker
class EmiReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val loanRepository: LoanRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val loanId = inputData.getLong(KEY_LOAN_ID, -1L)
        val daysBeforeDue = inputData.getInt(KEY_DAYS_BEFORE, 1)

        val loan = loanRepository.getLoanById(loanId).firstOrNull() ?: return Result.success()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("loanId", loanId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, loanId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when (daysBeforeDue) {
            0 -> "Your ${loan.loanName} EMI is due today!"
            1 -> "Your ${loan.loanName} EMI is due tomorrow. Don't miss it!"
            else -> "Your ${loan.loanName} EMI is due in $daysBeforeDue days."
        }

        val notification = NotificationCompat.Builder(applicationContext, LoanMateApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("EMI Reminder — ${loan.bankName}")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(loanId.toInt() * 10 + daysBeforeDue, notification)

        return Result.success()
    }

    companion object {
        const val KEY_LOAN_ID = "loan_id"
        const val KEY_DAYS_BEFORE = "days_before"

        fun scheduleReminders(context: Context, loanId: Long, dueDateMs: Long) {
            val workManager = WorkManager.getInstance(context)
            val now = System.currentTimeMillis()

            listOf(7, 3, 1, 0).forEach { daysBefore ->
                val delayMs = dueDateMs - (daysBefore * 24 * 60 * 60 * 1000L) - now
                if (delayMs > 0) {
                    val data = workDataOf(KEY_LOAN_ID to loanId, KEY_DAYS_BEFORE to daysBefore)
                    val request = OneTimeWorkRequestBuilder<EmiReminderWorker>()
                        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                        .setInputData(data)
                        .addTag("loan_$loanId")
                        .build()
                    workManager.enqueue(request)
                }
            }
        }

        fun cancelReminders(context: Context, loanId: Long) {
            WorkManager.getInstance(context).cancelAllWorkByTag("loan_$loanId")
        }
    }
}
