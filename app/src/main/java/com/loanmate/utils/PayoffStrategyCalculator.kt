package com.loanmate.utils

import com.loanmate.data.local.LoanEntity

/**
 * Multi-loan payoff simulator.
 *
 * Each month:
 *   1. Every loan accrues interest on its outstanding (reducing balance).
 *   2. Pay minimum EMI on every loan -> principal reduces by (EMI - interest).
 *   3. Apply any extra cash to ONE loan based on strategy.
 *   4. Loans that hit ≤ 0 are removed.
 *
 * Cap simulation at 50 years (600 months) to guard against pathological inputs.
 */
object PayoffStrategyCalculator {

    enum class Strategy { AVALANCHE, SNOWBALL }

    data class LoanTimeline(val loanId: Long, val loanName: String, val finishedInMonth: Int)

    data class PayoffPlan(
        val strategy: Strategy,
        val totalMonths: Int,
        val totalInterestPaid: Double,
        val totalPaid: Double,
        val timeline: List<LoanTimeline>
    )

    private data class SimLoan(
        val id: Long,
        val name: String,
        val rate: Double,            // annual rate %
        var outstanding: Double,
        val baseEmi: Double
    )

    fun simulate(
        loans: List<LoanEntity>,
        extraCashPerMonth: Double,
        strategy: Strategy
    ): PayoffPlan {
        val sim = loans.map {
            SimLoan(it.id, it.loanName, it.interestRate, it.outstandingAmount, it.monthlyEmi)
        }.toMutableList()

        val timeline = mutableListOf<LoanTimeline>()
        var month = 0
        var totalInterest = 0.0
        var totalPaid = 0.0
        val cap = 600

        while (sim.isNotEmpty() && month < cap) {
            month++

            // Pay minimum EMI on each loan
            for (loan in sim) {
                val monthlyRate = loan.rate / 1200.0
                val interest = loan.outstanding * monthlyRate
                val principalPart = (loan.baseEmi - interest).coerceAtLeast(0.0)
                val payment = minOf(loan.baseEmi, loan.outstanding + interest)
                totalInterest += interest
                totalPaid += payment
                loan.outstanding = (loan.outstanding + interest - payment).coerceAtLeast(0.0)
                // unused: principalPart - keep for clarity above
                @Suppress("UNUSED_VARIABLE") val _p = principalPart
            }

            // Apply extra cash based on strategy (pick target from REMAINING loans only)
            val activeLoans = sim.filter { it.outstanding > 0 }
            if (extraCashPerMonth > 0 && activeLoans.isNotEmpty()) {
                val target = when (strategy) {
                    Strategy.AVALANCHE -> activeLoans.maxByOrNull { it.rate }!!
                    Strategy.SNOWBALL -> activeLoans.minByOrNull { it.outstanding }!!
                }
                val extra = minOf(extraCashPerMonth, target.outstanding)
                target.outstanding -= extra
                totalPaid += extra
            }

            // Remove finished loans
            val finished = sim.filter { it.outstanding <= 0.01 }
            finished.forEach {
                timeline.add(LoanTimeline(it.id, it.name, month))
            }
            sim.removeAll(finished)
        }

        return PayoffPlan(
            strategy = strategy,
            totalMonths = month,
            totalInterestPaid = totalInterest,
            totalPaid = totalPaid,
            timeline = timeline
        )
    }
}
