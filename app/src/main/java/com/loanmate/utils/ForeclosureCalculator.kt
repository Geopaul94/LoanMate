package com.loanmate.utils

/**
 * Foreclosure = close the loan today by paying outstanding + foreclosure charges.
 * Compare to letting the loan run its course at the current EMI.
 */
object ForeclosureCalculator {

    data class Result(
        val outstanding: Double,
        val foreclosureCharges: Double,
        val totalPayable: Double,            // outstanding + charges (what you pay today)
        val futureTotalPayments: Double,     // EMI * remaining months (what you'd pay otherwise)
        val interestSaved: Double,           // how much interest you avoid
        val netBenefit: Double,              // interestSaved - charges
        val isWorthIt: Boolean               // benefit > 0 and meaningful (> 5% of charges)
    )

    /**
     * @param outstanding current principal owed
     * @param currentEmi monthly EMI today
     * @param remainingMonths months left on the loan
     * @param foreclosureChargePercent percent of outstanding (typical 2-5%)
     */
    fun calculate(
        outstanding: Double,
        currentEmi: Double,
        remainingMonths: Int,
        foreclosureChargePercent: Double
    ): Result {
        val charges = outstanding * foreclosureChargePercent / 100.0
        val totalPayable = outstanding + charges
        val futurePayments = currentEmi * remainingMonths
        // Interest you avoid paying = future payments - the outstanding you'd have repaid anyway
        val interestSaved = (futurePayments - outstanding).coerceAtLeast(0.0)
        val netBenefit = interestSaved - charges
        return Result(
            outstanding = outstanding,
            foreclosureCharges = charges,
            totalPayable = totalPayable,
            futureTotalPayments = futurePayments,
            interestSaved = interestSaved,
            netBenefit = netBenefit,
            isWorthIt = netBenefit > charges * 0.05
        )
    }
}
