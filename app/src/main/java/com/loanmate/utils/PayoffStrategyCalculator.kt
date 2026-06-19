package com.loanmate.utils

import com.loanmate.data.local.LoanEntity

/**
 * Multi-loan payoff simulator.
 * Capped at 600 months. Loans whose base EMI cannot cover their monthly
 * interest (non-amortizing) are surfaced via [PayoffPlan.nonAmortizingLoans];
 * the simulator still includes them but the cap will be hit.
 */
object PayoffStrategyCalculator {

    private const val CAP_MONTHS = 600

    enum class Strategy { AVALANCHE, SNOWBALL }

    data class LoanTimeline(val loanId: Long, val loanName: String, val finishedInMonth: Int)

    data class PayoffPlan(
        val strategy: Strategy,
        val totalMonths: Int,
        val totalInterestPaid: Double,
        val totalPaid: Double,
        val timeline: List<LoanTimeline>,
        val capReached: Boolean = false,
        val nonAmortizingLoans: List<String> = emptyList()
    )

    private data class SimLoan(
        val id: Long,
        val name: String,
        val rate: Double,
        var outstanding: Double,
        val baseEmi: Double
    )

    fun simulate(
        loans: List<LoanEntity>,
        extraCashPerMonth: Double,
        strategy: Strategy
    ): PayoffPlan {
        // Sanitize input — drop loans that can't be meaningfully simulated
        val cleanExtra = extraCashPerMonth.takeIf { it.isFinite() && it >= 0 } ?: 0.0
        val sim = loans
            .filter {
                it.outstandingAmount.isFinite() && it.outstandingAmount > 0 &&
                        it.monthlyEmi.isFinite() && it.monthlyEmi > 0 &&
                        it.interestRate.isFinite() && it.interestRate >= 0
            }
            .map { SimLoan(it.id, it.loanName, it.interestRate, it.outstandingAmount, it.monthlyEmi) }
            .toMutableList()

        // Detect non-amortizing loans up-front (EMI ≤ monthly interest)
        val nonAmortizing = sim.filter { it.baseEmi <= it.outstanding * it.rate / 1200.0 + 1e-9 }
            .map { it.name }

        val timeline = mutableListOf<LoanTimeline>()
        var month = 0
        var totalInterest = 0.0
        var totalPaid = 0.0

        while (sim.isNotEmpty() && month < CAP_MONTHS) {
            month++

            // 1. Pay minimum EMI on each loan
            for (loan in sim) {
                val monthlyRate = loan.rate / 1200.0
                val interest = loan.outstanding * monthlyRate
                val payment = minOf(loan.baseEmi, loan.outstanding + interest)
                totalInterest += interest
                totalPaid += payment
                loan.outstanding = (loan.outstanding + interest - payment).coerceAtLeast(0.0)
            }

            // 2. Apply extra cash to ONE target
            val active = sim.filter { it.outstanding > 0 }
            if (cleanExtra > 0 && active.isNotEmpty()) {
                val target = when (strategy) {
                    Strategy.AVALANCHE -> active.maxByOrNull { it.rate }!!
                    Strategy.SNOWBALL -> active.minByOrNull { it.outstanding }!!
                }
                val extra = minOf(cleanExtra, target.outstanding)
                target.outstanding -= extra
                totalPaid += extra
            }

            // 3. Remove finished loans
            val finished = sim.filter { it.outstanding <= 0.01 }
            finished.forEach { timeline.add(LoanTimeline(it.id, it.name, month)) }
            sim.removeAll(finished)
        }

        val capReached = month >= CAP_MONTHS && sim.isNotEmpty()

        return PayoffPlan(
            strategy = strategy,
            totalMonths = month,
            totalInterestPaid = totalInterest,
            totalPaid = totalPaid,
            timeline = timeline,
            capReached = capReached,
            nonAmortizingLoans = nonAmortizing
        )
    }
}
