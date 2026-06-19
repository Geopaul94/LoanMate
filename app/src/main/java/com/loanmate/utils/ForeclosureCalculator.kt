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
        require(outstanding.isFinite() && outstanding >= 0) { "outstanding must be non-negative" }
        require(currentEmi.isFinite() && currentEmi >= 0) { "currentEmi must be non-negative" }
        require(remainingMonths >= 0) { "remainingMonths must be non-negative" }
        require(foreclosureChargePercent.isFinite() && foreclosureChargePercent in 0.0..100.0) {
            "foreclosureChargePercent must be 0..100"
        }

        val charges = outstanding * foreclosureChargePercent / 100.0
        val totalPayable = outstanding + charges
        val futurePayments = currentEmi * remainingMonths
        val interestSaved = (futurePayments - outstanding).coerceAtLeast(0.0)
        val netBenefit = interestSaved - charges

        // Threshold: net benefit must exceed BOTH 5% of charges AND ₹1000 absolute.
        // Avoids "worth it" verdicts on penny-shaved-off-zero loans.
        val worthThreshold = maxOf(charges * 0.05, 1000.0)
        val isWorthIt = outstanding > 0 && netBenefit > worthThreshold

        return Result(
            outstanding = outstanding,
            foreclosureCharges = charges,
            totalPayable = totalPayable,
            futureTotalPayments = futurePayments,
            interestSaved = interestSaved,
            netBenefit = netBenefit,
            isWorthIt = isWorthIt
        )
    }
}
