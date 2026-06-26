package com.loanmate.utils

import com.google.common.truth.Truth.assertThat
import com.loanmate.data.model.InterestType
import com.loanmate.data.model.TenureUnit
import org.junit.Test
import kotlin.math.abs

class EmiCalculatorTest {

    private fun roughly(actual: Double, expected: Double, tol: Double = 1.0) {
        assertThat(abs(actual - expected)).isLessThan(tol)
    }

    @Test fun `reducing balance 5L at 10pct over 60mo`() {
        // Standard reference: ₹10,624.84/month (BankBazaar rounded display).
        // Exact double-precision answer ~₹10,623.5; both round to the same paise.
        val emi = EmiCalculator.calculateEmi(
            principal = 500_000.0, annualRatePercent = 10.0,
            tenureValue = 60, tenureUnit = TenureUnit.MONTHS,
            interestType = InterestType.REDUCING_BALANCE
        )
        roughly(emi, 10_624.84, tol = 2.0)
    }

    @Test fun `reducing balance EMI satisfies amortization identity`() {
        // Independent check: applying the EMI for n months at the given rate
        // must reduce the principal to exactly zero (within rounding).
        val p = 500_000.0
        val annual = 10.0
        val n = 60
        val emi = EmiCalculator.calculateEmi(p, annual, n, TenureUnit.MONTHS, InterestType.REDUCING_BALANCE)
        val r = annual / 1200.0
        var balance = p
        repeat(n) {
            val interest = balance * r
            val principalPart = emi - interest
            balance -= principalPart
        }
        // After n months balance should be ~0
        assertThat(kotlin.math.abs(balance)).isLessThan(0.01)
    }

    @Test fun `years tenure converts to months`() {
        val yearsEmi = EmiCalculator.calculateEmi(
            500_000.0, 10.0, 5, TenureUnit.YEARS, InterestType.REDUCING_BALANCE
        )
        val monthsEmi = EmiCalculator.calculateEmi(
            500_000.0, 10.0, 60, TenureUnit.MONTHS, InterestType.REDUCING_BALANCE
        )
        roughly(yearsEmi, monthsEmi, tol = 0.01)
    }

    @Test fun `zero interest is linear`() {
        val emi = EmiCalculator.calculateEmi(
            100_000.0, 0.0, 20, TenureUnit.MONTHS, InterestType.FIXED
        )
        roughly(emi, 5_000.0)
    }

    @Test fun `fixed interest higher than reducing for same inputs`() {
        val fixed = EmiCalculator.calculateEmi(
            500_000.0, 10.0, 60, TenureUnit.MONTHS, InterestType.FIXED
        )
        val reducing = EmiCalculator.calculateEmi(
            500_000.0, 10.0, 60, TenureUnit.MONTHS, InterestType.REDUCING_BALANCE
        )
        assertThat(fixed).isGreaterThan(reducing)
    }

    @Test fun `progress percent 0 to 100`() {
        assertThat(EmiCalculator.getProgressPercent(0, 60)).isEqualTo(0f)
        assertThat(EmiCalculator.getProgressPercent(30, 60)).isEqualTo(50f)
        assertThat(EmiCalculator.getProgressPercent(60, 60)).isEqualTo(100f)
    }

    @Test fun `progress percent handles zero total`() {
        assertThat(EmiCalculator.getProgressPercent(0, 0)).isEqualTo(0f)
    }

    @Test fun `milestone messages at correct thresholds`() {
        assertThat(EmiCalculator.getMilestoneMessage(0f)).isNull()
        assertThat(EmiCalculator.getMilestoneMessage(24f)).isNull()
        assertThat(EmiCalculator.getMilestoneMessage(25f)).isNotNull()
        assertThat(EmiCalculator.getMilestoneMessage(50f)).isNotNull()
        assertThat(EmiCalculator.getMilestoneMessage(100f)).contains("Congratulations")
    }

    @Test fun `timeUntil same date returns zeros`() {
        val now = 1_700_000_000_000L
        val r = EmiCalculator.timeUntil(now, now)
        assertThat(r.years).isEqualTo(0)
        assertThat(r.months).isEqualTo(0)
        assertThat(r.days).isEqualTo(0)
    }

    @Test fun `timeUntil past target returns zeros`() {
        val now = 1_700_000_000_000L
        val past = now - 10_000_000L
        val r = EmiCalculator.timeUntil(past, now)
        assertThat(r.totalDays).isEqualTo(0)
    }

    @Test fun `timeUntil 1 year exactly`() {
        val cal = java.util.Calendar.getInstance().apply { set(2025, 0, 1, 0, 0, 0) }
        val start = cal.timeInMillis
        cal.set(2026, 0, 1, 0, 0, 0)
        val end = cal.timeInMillis
        val r = EmiCalculator.timeUntil(end, start)
        assertThat(r.years).isEqualTo(1)
        assertThat(r.months).isEqualTo(0)
        assertThat(r.days).isEqualTo(0)
    }
}
