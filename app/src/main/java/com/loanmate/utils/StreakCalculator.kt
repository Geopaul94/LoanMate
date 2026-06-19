package com.loanmate.utils

import com.loanmate.data.local.PaymentHistoryEntity

/**
 * Pure streak math — no I/O, easy to test.
 *
 * Streak rules:
 *   - "On time" means payment.paidDate <= payment.dueDate + GRACE_MS
 *   - Iterate payments newest-first.
 *   - currentStreak counts CONSECUTIVE on-time payments from the most recent
 *     payment backward. Stops at the first late payment.
 *   - longestStreak is the max run of consecutive on-time payments in history.
 */
object StreakCalculator {

    // 24-hour grace window — paying on the due date but after midnight still counts
    private const val GRACE_MS = 24L * 60 * 60 * 1000

    data class Streak(val current: Int, val longest: Int)

    fun calculate(payments: List<PaymentHistoryEntity>): Streak {
        if (payments.isEmpty()) return Streak(0, 0)

        // Defensive sort — assume input is newest-first but guard against caller mistakes
        val sorted = payments.sortedByDescending { it.paidDate }

        // Current streak: walk from newest backward, stop at first late one
        var current = 0
        for (p in sorted) {
            if (isOnTime(p)) current++ else break
        }

        // Longest streak: scan entire history forward, track max run
        var longest = 0
        var run = 0
        for (p in sorted.asReversed()) {   // oldest-first
            if (isOnTime(p)) {
                run++
                if (run > longest) longest = run
            } else {
                run = 0
            }
        }
        if (current > longest) longest = current

        return Streak(current, longest)
    }

    private fun isOnTime(p: PaymentHistoryEntity): Boolean =
        p.paidDate <= p.dueDate + GRACE_MS
}
