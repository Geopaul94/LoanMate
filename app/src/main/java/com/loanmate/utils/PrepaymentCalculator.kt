package com.loanmate.utils

import kotlin.math.ln
import kotlin.math.pow

/**
 * Reducing-Balance prepayment math.
 *
 * `totalSavings` is reported as the NET savings over the remaining life of the
 * loan, after subtracting the prepayment outlay. `interestSaved` is the
 * reduction in interest charged by the bank — that comes back to the user as
 * fewer/smaller future EMIs.
 */
object PrepaymentCalculator {

    private const val EPS = 1e-9

    enum class Mode { REDUCE_TENURE, REDUCE_EMI }

    data class Result(
        val mode: Mode,
        val originalRemainingMonths: Int,
        val originalRemainingPayments: Double,
        val newRemainingMonths: Int,
        val newEmi: Double,
        val monthsSaved: Int,
        val interestSaved: Double,
        val totalSavings: Double           // net of prepayment outlay
    )

    fun calculate(
        outstanding: Double,
        annualRatePercent: Double,
        currentEmi: Double,
        remainingMonths: Int,
        prepaymentAmount: Double,
        mode: Mode
    ): Result {
        // Input validation — fail fast on garbage rather than propagate NaN/∞
        require(outstanding.isFinite() && outstanding >= 0) { "outstanding must be a non-negative finite number" }
        require(currentEmi.isFinite() && currentEmi > 0) { "currentEmi must be positive" }
        require(remainingMonths > 0) { "remainingMonths must be positive" }
        require(prepaymentAmount.isFinite() && prepaymentAmount in 0.0..outstanding) {
            "prepaymentAmount must be in 0..outstanding"
        }
        require(annualRatePercent.isFinite() && annualRatePercent >= 0) { "annualRatePercent must be non-negative" }

        // Edge: prepayment closes the loan entirely
        if (prepaymentAmount >= outstanding - EPS) {
            val originalPayments = currentEmi * remainingMonths
            val interestSaved = (originalPayments - outstanding).coerceAtLeast(0.0)
            // Net savings = old future payments - prepayment now
            val net = (originalPayments - prepaymentAmount).coerceAtLeast(0.0)
            return Result(
                mode = mode,
                originalRemainingMonths = remainingMonths,
                originalRemainingPayments = originalPayments,
                newRemainingMonths = 0,
                newEmi = 0.0,
                monthsSaved = remainingMonths,
                interestSaved = interestSaved,
                totalSavings = net
            )
        }

        val newOutstanding = outstanding - prepaymentAmount
        val originalPayments = currentEmi * remainingMonths
        val originalInterest = originalPayments - outstanding

        // 0% interest loan — linear math
        if (annualRatePercent == 0.0) {
            return when (mode) {
                Mode.REDUCE_TENURE -> {
                    val newMonths = Math.ceil(newOutstanding / currentEmi).toInt()
                    val newPayments = newOutstanding   // no interest, total cost = principal
                    Result(
                        mode = mode,
                        originalRemainingMonths = remainingMonths,
                        originalRemainingPayments = originalPayments,
                        newRemainingMonths = newMonths,
                        newEmi = currentEmi,
                        monthsSaved = remainingMonths - newMonths,
                        interestSaved = 0.0,
                        totalSavings = originalPayments - newPayments - prepaymentAmount
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

        val r = annualRatePercent / 1200.0

        return when (mode) {
            Mode.REDUCE_TENURE -> {
                val newInterestPortion = newOutstanding * r
                val canShorten = currentEmi > newInterestPortion + EPS
                val rawN: Double = if (canShorten) {
                    ln(currentEmi / (currentEmi - newInterestPortion)) / ln(1 + r)
                } else remainingMonths.toDouble()
                val newMonths = if (rawN.isFinite())
                    Math.ceil(rawN).toInt().coerceIn(0, remainingMonths)
                else remainingMonths

                // True interest under the new schedule: bank charges (newEMI * newMonths) - newPrincipal,
                // but the last EMI is partial. Use the exact analytic value from the un-ceiled n:
                val newInterest = if (canShorten && rawN.isFinite())
                    (currentEmi * rawN - newOutstanding).coerceAtLeast(0.0)
                else originalInterest * (newOutstanding / outstanding)

                val interestSaved = (originalInterest - newInterest).coerceAtLeast(0.0)
                // Net savings = original future cost - (new future cost + prepayment now)
                // For REDUCE_TENURE, new future cost ≈ newOutstanding + newInterest, so:
                // net = originalPayments - (newOutstanding + newInterest + prepaymentAmount)
                //     = originalPayments - newOutstanding - newInterest - prepayment
                //     = originalInterest + outstanding - newOutstanding - newInterest - prepayment
                //     = originalInterest - newInterest  (since outstanding - newOutstanding = prepayment)
                //     = interestSaved
                // So for REDUCE_TENURE, net == interestSaved (already validated by Sprint 3 auditor).
                val net = interestSaved
                Result(
                    mode = mode,
                    originalRemainingMonths = remainingMonths,
                    originalRemainingPayments = originalPayments,
                    newRemainingMonths = newMonths,
                    newEmi = currentEmi,
                    monthsSaved = remainingMonths - newMonths,
                    interestSaved = interestSaved,
                    totalSavings = net
                )
            }
            Mode.REDUCE_EMI -> {
                val pow = (1 + r).pow(remainingMonths)
                val newEmi = newOutstanding * r * pow / (pow - 1)
                val newPayments = newEmi * remainingMonths
                val newInterest = newPayments - newOutstanding
                val interestSaved = (originalInterest - newInterest).coerceAtLeast(0.0)
                // True net savings = (oldEMI*n) - (prepayment + newEMI*n) = emiReduction - prepayment
                val emiReduction = (currentEmi - newEmi) * remainingMonths
                val net = emiReduction - prepaymentAmount
                Result(
                    mode = mode,
                    originalRemainingMonths = remainingMonths,
                    originalRemainingPayments = originalPayments,
                    newRemainingMonths = remainingMonths,
                    newEmi = newEmi,
                    monthsSaved = 0,
                    interestSaved = interestSaved,
                    totalSavings = net
                )
            }
        }
    }
}
