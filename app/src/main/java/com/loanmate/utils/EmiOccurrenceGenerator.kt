package com.loanmate.utils

import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.LoanStatus
import java.util.Calendar

/**
 * Pre-computes upcoming EMI events for the calendar.
 * Generates only the remaining (unpaid) EMIs from now until loan end,
 * so already-paid months don't clutter the calendar.
 */
object EmiOccurrenceGenerator {

    data class Occurrence(
        val loanId: Long,
        val loanName: String,
        val bankName: String,
        val amount: Double,
        val year: Int,
        val month: Int,   // 0-11 (Calendar.MONTH convention)
        val day: Int      // 1-31
    )

    /**
     * Returns all UPCOMING EMI occurrences (skipping already-paid ones).
     * Limits to the next [horizonMonths] months from today for performance.
     */
    fun forActiveLoans(
        loans: List<LoanEntity>,
        horizonMonths: Int = 24
    ): List<Occurrence> {
        val now = System.currentTimeMillis()
        val horizonEnd = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, horizonMonths)
        }.timeInMillis

        val result = mutableListOf<Occurrence>()
        for (loan in loans) {
            if (loan.status != LoanStatus.ACTIVE) continue
            val remaining = (loan.totalEmis - loan.completedEmis).coerceAtLeast(0)
            if (remaining == 0) continue

            val cal = Calendar.getInstance().apply { timeInMillis = loan.firstEmiDate }
            // Fast-forward past already-paid EMIs
            cal.add(Calendar.MONTH, loan.completedEmis)

            repeat(remaining) {
                val ts = cal.timeInMillis
                if (ts in now..horizonEnd) {
                    result.add(
                        Occurrence(
                            loanId = loan.id,
                            loanName = loan.loanName,
                            bankName = loan.bankName,
                            amount = loan.monthlyEmi,
                            year = cal.get(Calendar.YEAR),
                            month = cal.get(Calendar.MONTH),
                            day = cal.get(Calendar.DAY_OF_MONTH)
                        )
                    )
                }
                cal.add(Calendar.MONTH, 1)
            }
        }
        return result
    }
}
