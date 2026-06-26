package com.loanmate.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class PrepaymentCalculatorTest {

    // --- Sanity helpers ---

    private fun emi(p: Double, annualPct: Double, n: Int): Double {
        if (annualPct == 0.0) return p / n
        val r = annualPct / 1200.0
        val pw = (1 + r).pow(n)
        return p * r * pw / (pw - 1)
    }

    private fun roughly(actual: Double, expected: Double, tolerance: Double = 1.0) {
        assertThat(abs(actual - expected)).isLessThan(tolerance)
    }

    // --- REDUCE_TENURE ---

    @Test fun `reduce tenure - 5L loan 10pct prepay 1L finishes early`() {
        val emi5L = emi(500_000.0, 10.0, 60)   // ~10,624.84
        val r = PrepaymentCalculator.calculate(
            outstanding = 500_000.0, annualRatePercent = 10.0, currentEmi = emi5L,
            remainingMonths = 60, prepaymentAmount = 100_000.0,
            mode = PrepaymentCalculator.Mode.REDUCE_TENURE
        )
        assertThat(r.newRemainingMonths).isLessThan(60)
        assertThat(r.monthsSaved).isGreaterThan(10)
        assertThat(r.interestSaved).isGreaterThan(0.0)
        // For REDUCE_TENURE, totalSavings should equal interestSaved (verified by Sprint 3 audit)
        roughly(r.totalSavings, r.interestSaved, tolerance = 0.01)
    }

    @Test fun `reduce tenure - 0pct loan uses linear math`() {
        val r = PrepaymentCalculator.calculate(
            outstanding = 200_000.0, annualRatePercent = 0.0, currentEmi = 10_000.0,
            remainingMonths = 20, prepaymentAmount = 50_000.0,
            mode = PrepaymentCalculator.Mode.REDUCE_TENURE
        )
        // 150K / 10K = 15 months
        assertThat(r.newRemainingMonths).isEqualTo(15)
        assertThat(r.monthsSaved).isEqualTo(5)
        assertThat(r.interestSaved).isEqualTo(0.0)
        // Net savings = (10K * 20) - (10K * 15) - 50K = 200K - 150K - 50K = 0
        roughly(r.totalSavings, 0.0)
    }

    @Test fun `reduce tenure - prepay equal to outstanding closes loan`() {
        val r = PrepaymentCalculator.calculate(
            outstanding = 100_000.0, annualRatePercent = 10.0, currentEmi = 5_000.0,
            remainingMonths = 24, prepaymentAmount = 100_000.0,
            mode = PrepaymentCalculator.Mode.REDUCE_TENURE
        )
        assertThat(r.newRemainingMonths).isEqualTo(0)
        assertThat(r.monthsSaved).isEqualTo(24)
        // You pay 100K now, would have paid 5K * 24 = 120K otherwise → save 20K
        roughly(r.totalSavings, 20_000.0)
    }

    // --- REDUCE_EMI ---

    @Test fun `reduce emi - 5L loan 10pct prepay 1L lowers EMI`() {
        val emi5L = emi(500_000.0, 10.0, 60)
        val expectedNewEmi = emi(400_000.0, 10.0, 60)  // ~8,499.87
        val r = PrepaymentCalculator.calculate(
            outstanding = 500_000.0, annualRatePercent = 10.0, currentEmi = emi5L,
            remainingMonths = 60, prepaymentAmount = 100_000.0,
            mode = PrepaymentCalculator.Mode.REDUCE_EMI
        )
        assertThat(r.newRemainingMonths).isEqualTo(60)
        roughly(r.newEmi, expectedNewEmi)
        // Net savings = emiReduction - prepayment
        val emiReduction = (emi5L - expectedNewEmi) * 60
        roughly(r.totalSavings, emiReduction - 100_000.0)
    }

    @Test fun `reduce emi - 0pct loan linear`() {
        val r = PrepaymentCalculator.calculate(
            outstanding = 100_000.0, annualRatePercent = 0.0, currentEmi = 5_000.0,
            remainingMonths = 20, prepaymentAmount = 20_000.0,
            mode = PrepaymentCalculator.Mode.REDUCE_EMI
        )
        roughly(r.newEmi, 4_000.0)   // 80K / 20 mo
        // Net = (5K - 4K) * 20 - 20K = 20K - 20K = 0
        roughly(r.totalSavings, 0.0)
    }

    // --- Input validation ---

    @Test(expected = IllegalArgumentException::class)
    fun `negative outstanding throws`() {
        PrepaymentCalculator.calculate(
            outstanding = -100.0, annualRatePercent = 10.0, currentEmi = 5_000.0,
            remainingMonths = 24, prepaymentAmount = 0.0,
            mode = PrepaymentCalculator.Mode.REDUCE_TENURE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `prepayment exceeding outstanding throws`() {
        PrepaymentCalculator.calculate(
            outstanding = 100_000.0, annualRatePercent = 10.0, currentEmi = 5_000.0,
            remainingMonths = 24, prepaymentAmount = 200_000.0,
            mode = PrepaymentCalculator.Mode.REDUCE_TENURE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero EMI throws`() {
        PrepaymentCalculator.calculate(
            outstanding = 100_000.0, annualRatePercent = 10.0, currentEmi = 0.0,
            remainingMonths = 24, prepaymentAmount = 1_000.0,
            mode = PrepaymentCalculator.Mode.REDUCE_TENURE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `NaN inputs throw`() {
        PrepaymentCalculator.calculate(
            outstanding = Double.NaN, annualRatePercent = 10.0, currentEmi = 5_000.0,
            remainingMonths = 24, prepaymentAmount = 1_000.0,
            mode = PrepaymentCalculator.Mode.REDUCE_TENURE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative interest rate throws`() {
        PrepaymentCalculator.calculate(
            outstanding = 100_000.0, annualRatePercent = -5.0, currentEmi = 5_000.0,
            remainingMonths = 24, prepaymentAmount = 1_000.0,
            mode = PrepaymentCalculator.Mode.REDUCE_TENURE
        )
    }
}
