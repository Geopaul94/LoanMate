package com.loanmate.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class ForeclosureCalculatorTest {

    private fun roughly(actual: Double, expected: Double, tol: Double = 0.01) {
        assertThat(abs(actual - expected)).isLessThan(tol)
    }

    @Test fun `worth-it scenario - high remaining interest`() {
        val r = ForeclosureCalculator.calculate(
            outstanding = 300_000.0, currentEmi = 10_000.0,
            remainingMonths = 40, foreclosureChargePercent = 2.0
        )
        roughly(r.foreclosureCharges, 6_000.0)
        roughly(r.totalPayable, 306_000.0)
        roughly(r.futureTotalPayments, 400_000.0)
        roughly(r.interestSaved, 100_000.0)
        roughly(r.netBenefit, 94_000.0)
        assertThat(r.isWorthIt).isTrue()
    }

    @Test fun `not-worth-it scenario - charges eat savings`() {
        val r = ForeclosureCalculator.calculate(
            outstanding = 100_000.0, currentEmi = 10_500.0,
            remainingMonths = 10, foreclosureChargePercent = 4.0
        )
        roughly(r.foreclosureCharges, 4_000.0)
        roughly(r.interestSaved, 5_000.0)
        roughly(r.netBenefit, 1_000.0)
        // netBenefit = 1000 NOT > max(0.05*4000=200, 1000) → not worth
        assertThat(r.isWorthIt).isFalse()
    }

    @Test fun `zero charges RBI exemption`() {
        val r = ForeclosureCalculator.calculate(
            outstanding = 500_000.0, currentEmi = 8_000.0,
            remainingMonths = 80, foreclosureChargePercent = 0.0
        )
        roughly(r.foreclosureCharges, 0.0)
        // netBenefit = 140K > max(0, 1000) → worth
        assertThat(r.isWorthIt).isTrue()
    }

    @Test fun `outstanding zero - not worth it`() {
        val r = ForeclosureCalculator.calculate(
            outstanding = 0.0, currentEmi = 10_000.0,
            remainingMonths = 0, foreclosureChargePercent = 2.0
        )
        assertThat(r.totalPayable).isEqualTo(0.0)
        assertThat(r.isWorthIt).isFalse()   // guard: outstanding > 0
    }

    @Test fun `remaining months zero - clean exit`() {
        val r = ForeclosureCalculator.calculate(
            outstanding = 100_000.0, currentEmi = 5_000.0,
            remainingMonths = 0, foreclosureChargePercent = 2.0
        )
        assertThat(r.interestSaved).isEqualTo(0.0)
        assertThat(r.isWorthIt).isFalse()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative outstanding throws`() {
        ForeclosureCalculator.calculate(-100.0, 5_000.0, 24, 2.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `charge over 100 percent throws`() {
        ForeclosureCalculator.calculate(100_000.0, 5_000.0, 24, 150.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `NaN charge throws`() {
        ForeclosureCalculator.calculate(100_000.0, 5_000.0, 24, Double.NaN)
    }
}
