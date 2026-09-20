package com.loanmate.utils

import com.loanmate.data.local.LoanEntity

object AmortizationCalculator {

    data class AmortizationMonth(
        val monthNumber: Int,
        val payment: Double,
        val principal: Double,
        val interest: Double,
        val remainingBalance: Double
    )

    fun calculate(loan: LoanEntity): List<AmortizationMonth> {
        val schedule = mutableListOf<AmortizationMonth>()
        var balance = loan.outstandingAmount
        val monthlyRate = loan.interestRate / 1200.0
        val emi = loan.monthlyEmi
        var month = loan.completedEmis + 1

        while (balance > 0.01 && month <= 600) { // Safety cap
            val interest = balance * monthlyRate
            val principal = minOf(balance, emi - interest)
            val payment = principal + interest
            balance -= principal
            schedule.add(
                AmortizationMonth(
                    monthNumber = month,
                    payment = payment,
                    principal = principal,
                    interest = interest,
                    remainingBalance = balance.coerceAtLeast(0.0)
                )
            )
            month++
        }
        return schedule
    }
}
