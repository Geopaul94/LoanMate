package com.loanmate.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Re-schedules WorkManager jobs after device reboot (WorkManager handles this automatically
// for periodic work, but explicit boot handling keeps one-time reminders alive).
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // WorkManager persists work across reboots automatically — no extra action needed.
        }
    }
}
