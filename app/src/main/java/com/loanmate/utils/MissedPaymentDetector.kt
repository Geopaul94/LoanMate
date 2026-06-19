package com.loanmate.utils

import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.LoanStatus
import java.util.Calendar

/**
 * Detects how many EMIs a loan has missed by comparing how many SHOULD have
 * been paid by now vs how many WERE actually paid.
 *
 * The estimate is a heuristic — actual late fees and CIBIL hit depend on
 * the bank. Conservative defaults: 2% of EMI per missed payment, CIBIL warning
 * after 2+ missed.
 */
object MissedPaymentDetector {

    private const val DEFAULT_LATE_FEE_PERCENT = 2.0
    private const val CIBIL_WARNING_THRESHOLD = 2

    data class MissedInfo(
        val missedCount: Int,
        val estimatedPenalty: Double,
        val showCibilWarning: Boolean
    )

    fun detect(loan: LoanEntity, nowMs: Long = System.currentTimeMillis()): MissedInfo {
        if (loan.status != LoanStatus.ACTIVE) return MissedInfo(0, 0.0, false)
        val expected = expectedEmisByNow(loan.firstEmiDate, nowMs, loan.totalEmis)
        val missed = (expected - loan.completedEmis).coerceAtLeast(0)
        val penalty = missed * loan.monthlyEmi * DEFAULT_LATE_FEE_PERCENT / 100
        return MissedInfo(
            missedCount = missed,
            estimatedPenalty = penalty,
            showCibilWarning = missed >= CIBIL_WARNING_THRESHOLD
        )
    }

    /**
     * Counts how many EMI due-dates have ALREADY passed.
     * EMI #1 due on firstEmiDate, EMI #2 due one month after, etc.
     * Uses calendar arithmetic to handle varying month lengths.
     */
    private fun expectedEmisByNow(firstEmiDate: Long, nowMs: Long, totalEmis: Int): Int {
        if (firstEmiDate > nowMs) return 0
        val first = Calendar.getInstance().apply { timeInMillis = firstEmiDate }
        val now = Calendar.getInstance().apply { timeInMillis = nowMs }

        var monthsBetween = (now.get(Calendar.YEAR) - first.get(Calendar.YEAR)) * 12 +
                (now.get(Calendar.MONTH) - first.get(Calendar.MONTH))
        // If "today" is before the day-of-month of firstEmiDate, the most-recent due-date
        // is still in the prior month — subtract one.
        if (now.get(Calendar.DAY_OF_MONTH) < first.get(Calendar.DAY_OF_MONTH)) {
            monthsBetween -= 1
        }
        // monthsBetween = 0 means firstEmiDate has just passed → 1 EMI is due
        return (monthsBetween + 1).coerceIn(0, totalEmis)
    }
}
