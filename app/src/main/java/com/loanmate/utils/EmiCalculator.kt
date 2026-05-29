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

    fun getMilestoneMessage(progressPercent: Float): String? = when {
        progressPercent >= 100f -> "Congratulations! You completed your loan! 🎉"
        progressPercent >= 90f -> "Final stretch. You're almost there! 💪"
        progressPercent >= 75f -> "Only a little left. You're doing amazing! 🔥"
        progressPercent >= 50f -> "Halfway there. Keep going! ⭐"
        progressPercent >= 25f -> "Great start! You're already moving toward financial freedom! 🚀"
        else -> null
    }
}
