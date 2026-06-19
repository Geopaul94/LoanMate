package com.loanmate.utils

import com.loanmate.data.model.InterestType
import com.loanmate.data.model.TenureUnit
import kotlin.math.pow

object EmiCalculator {

    fun calculateEmi(
        principal: Double,
        annualRatePercent: Double,
        tenureValue: Int,
        tenureUnit: TenureUnit,
        interestType: InterestType
    ): Double {
        val months = if (tenureUnit == TenureUnit.YEARS) tenureValue * 12 else tenureValue
        if (annualRatePercent == 0.0) return principal / months

        return when (interestType) {
            InterestType.FIXED -> {
                val totalInterest = principal * (annualRatePercent / 100) * (months / 12.0)
                (principal + totalInterest) / months
            }
            InterestType.FLOATING,
            InterestType.REDUCING_BALANCE -> {
                val r = annualRatePercent / (12 * 100)
                principal * r * (1 + r).pow(months) / ((1 + r).pow(months) - 1)
            }
        }
    }

    fun calculateTotalInterest(
        principal: Double,
        emi: Double,
        tenureValue: Int,
        tenureUnit: TenureUnit
    ): Double {
        val months = if (tenureUnit == TenureUnit.YEARS) tenureValue * 12 else tenureValue
        return (emi * months) - principal
    }

    fun calculateLoanEndDate(startDateMs: Long, tenureValue: Int, tenureUnit: TenureUnit): Long {
        val months = if (tenureUnit == TenureUnit.YEARS) tenureValue * 12 else tenureValue
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = startDateMs
        cal.add(java.util.Calendar.MONTH, months)
        return cal.timeInMillis
    }

    fun getTotalMonths(tenureValue: Int, tenureUnit: TenureUnit): Int =
        if (tenureUnit == TenureUnit.YEARS) tenureValue * 12 else tenureValue

    fun getProgressPercent(completedEmis: Int, totalEmis: Int): Float =
        if (totalEmis == 0) 0f else (completedEmis.toFloat() / totalEmis * 100)

    /**
     * Calculates remaining months on a loan based on already-paid EMIs.
     * Adds remainingMonths to firstEmiDate to get true future end date.
     */
    fun projectLoanEndDate(firstEmiDate: Long, completedEmis: Int, totalEmis: Int): Long {
        val remaining = (totalEmis - completedEmis).coerceAtLeast(0)
        if (remaining == 0) return System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = firstEmiDate
        // We've already paid completedEmis EMIs since firstEmiDate, so jump forward
        // to the next EMI then add remaining-1 more months
        cal.add(java.util.Calendar.MONTH, completedEmis + remaining - 1)
        return cal.timeInMillis
    }

    /**
     * Breaks down a duration in milliseconds into years + months + days.
     * Uses calendar arithmetic for accuracy (handles leap years, varying month lengths).
     */
    data class TimeRemaining(val years: Int, val months: Int, val days: Int, val totalDays: Long)

    fun timeUntil(targetMs: Long, fromMs: Long = System.currentTimeMillis()): TimeRemaining {
        if (targetMs <= fromMs) return TimeRemaining(0, 0, 0, 0)
        val totalDays = (targetMs - fromMs) / (24L * 60 * 60 * 1000)
        val from = java.util.Calendar.getInstance().apply { timeInMillis = fromMs }
        val to = java.util.Calendar.getInstance().apply { timeInMillis = targetMs }
        var years = to.get(java.util.Calendar.YEAR) - from.get(java.util.Calendar.YEAR)
        var months = to.get(java.util.Calendar.MONTH) - from.get(java.util.Calendar.MONTH)
        var days = to.get(java.util.Calendar.DAY_OF_MONTH) - from.get(java.util.Calendar.DAY_OF_MONTH)
        if (days < 0) {
            months -= 1
            // borrow days from previous month
            val prev = to.clone() as java.util.Calendar
            prev.add(java.util.Calendar.MONTH, -1)
            days += prev.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        }
        if (months < 0) {
            years -= 1
            months += 12
        }
        return TimeRemaining(years.coerceAtLeast(0), months.coerceAtLeast(0), days.coerceAtLeast(0), totalDays)
    }

    fun getMilestoneMessage(progressPercent: Float): String? = when {
        progressPercent >= 100f -> "Congratulations! You completed your loan! 🎉"
        progressPercent >= 90f -> "Final stretch. You're almost there! 💪"
        progressPercent >= 75f -> "Only a little left. You're doing amazing! 🔥"
        progressPercent >= 50f -> "Halfway there. Keep going! ⭐"
        progressPercent >= 25f -> "Great start! You're already moving toward financial freedom! 🚀"
        else -> null
    }
}
