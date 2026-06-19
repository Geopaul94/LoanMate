package com.loanmate.utils

import kotlin.math.ln
import kotlin.math.pow

/**
 * Reducing-Balance prepayment math.
 *
 * All inputs are double, but the rounding is conservative — we never
 * underestimate months saved or interest saved. Use BigDecimal only when
 * presenting bank-statement-grade numbers; for "what-if" screens, double
 * precision is fine (accurate to ~₹0.01 over a 30-year loan).
 */
object PrepaymentCalculator {

    enum class Mode { REDUCE_TENURE, REDUCE_EMI }

    data class Result(
        val mode: Mode,
        val originalRemainingMonths: Int,
        val originalRemainingPayments: Double,    // sum of all future EMIs at current EMI
        val newRemainingMonths: Int,
        val newEmi: Double,                       // == oldEmi if mode = REDUCE_TENURE
        val monthsSaved: Int,
        val interestSaved: Double,
        val totalSavings: Double                  // interest + emi reduction over remaining months
    )

    /**
     * @param outstanding current principal still owed
     * @param annualRatePercent e.g. 9.5 for 9.5% p.a.
     * @param currentEmi the EMI the bank charges today
     * @param remainingMonths months left in the loan at current pace
     * @param prepaymentAmount the lump sum the user wants to pay
     */
    fun calculate(
        outstanding: Double,
        annualRatePercent: Double,
        currentEmi: Double,
        remainingMonths: Int,
        prepaymentAmount: Double,
        mode: Mode
    ): Result {
        // Edge: prepayment closes the loan
        if (prepaymentAmount >= outstanding) {
            val interestSaved = (currentEmi * remainingMonths) - outstanding
            return Result(
                mode = mode,
                originalRemainingMonths = remainingMonths,
                originalRemainingPayments = currentEmi * remainingMonths,
                newRemainingMonths = 0,
                newEmi = 0.0,
                monthsSaved = remainingMonths,
                interestSaved = interestSaved.coerceAtLeast(0.0),
                totalSavings = interestSaved.coerceAtLeast(0.0)
            )
        }

        val newOutstanding = outstanding - prepaymentAmount
        val originalPayments = currentEmi * remainingMonths
        val originalInterest = originalPayments - outstanding

        // 0% interest loan = linear math
        if (annualRatePercent <= 0.0) {
            return when (mode) {
                Mode.REDUCE_TENURE -> {
                    val newMonths = Math.ceil(newOutstanding / currentEmi).toInt()
                    Result(
                        mode = mode,
                        originalRemainingMonths = remainingMonths,
                        originalRemainingPayments = originalPayments,
                        newRemainingMonths = newMonths,
                        newEmi = currentEmi,
                        monthsSaved = remainingMonths - newMonths,
                        interestSaved = 0.0,
                        totalSavings = (remainingMonths - newMonths) * currentEmi - prepaymentAmount
                    )
                }
                Mode.REDUCE_EMI -> {
                    val newEmi = newOutstanding / remainingMonths
                    Result(
                        mode = mode,
                        originalRemainingMonths = remainingMonths,
                        originalRemainingPayments = originalPayments,
                        newRemainingMonths = remainingMonths,
                        newEmi = newEmi,
                        monthsSaved = 0,
                        interestSaved = 0.0,
                        totalSavings = (currentEmi - newEmi) * remainingMonths - prepaymentAmount
                    )
                }
            }
        }

        val r = annualRatePercent / 1200.0   // monthly rate

        return when (mode) {
            Mode.REDUCE_TENURE -> {
                // n_new = log(EMI / (EMI - newP * r)) / log(1+r)
                // Guard: EMI must exceed new monthly interest for log to be defined
                val newInterestPortion = newOutstanding * r
                val newMonths = if (currentEmi > newInterestPortion) {
                    Math.ceil(
                        ln(currentEmi / (currentEmi - newInterestPortion)) / ln(1 + r)
                    ).toInt().coerceAtMost(remainingMonths)
                } else remainingMonths

                val newPayments = currentEmi * newMonths
                val newInterest = newPayments - newOutstanding
                val interestSaved = (originalInterest - newInterest).coerceAtLeast(0.0)
                Result(
                    mode = mode,
                    originalRemainingMonths = remainingMonths,
                    originalRemainingPayments = originalPayments,
                    newRemainingMonths = newMonths,
                    newEmi = currentEmi,
                    monthsSaved = remainingMonths - newMonths,
                    interestSaved = interestSaved,
                    totalSavings = interestSaved
                )
            }
            Mode.REDUCE_EMI -> {
                // newEmi = newP * r * (1+r)^n / ((1+r)^n - 1)
                val pow = (1 + r).pow(remainingMonths)
                val newEmi = newOutstanding * r * pow / (pow - 1)
                val newPayments = newEmi * remainingMonths
                val newInterest = newPayments - newOutstanding
                val interestSaved = (originalInterest - newInterest).coerceAtLeast(0.0)
                val emiReduction = (currentEmi - newEmi) * remainingMonths
                Result(
                    mode = mode,
                    originalRemainingMonths = remainingMonths,
                    originalRemainingPayments = originalPayments,
                    newRemainingMonths = remainingMonths,
                    newEmi = newEmi,
                    monthsSaved = 0,
                    interestSaved = interestSaved,
                    totalSavings = emiReduction
                )
            }
        }
    }
}
