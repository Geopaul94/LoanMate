package com.loanmate.utils

import com.google.common.truth.Truth.assertThat
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.InterestType
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.model.LoanType
import com.loanmate.data.model.TenureUnit
import org.junit.Test
import java.util.Calendar

class MissedPaymentDetectorTest {

    private fun loan(
        firstEmiDate: Long, completedEmis: Int, total: Int = 24,
        status: LoanStatus = LoanStatus.ACTIVE
    ) = LoanEntity(
        id = 1, loanName = "X", bankName = "Y",
        loanType = LoanType.PERSONAL,
        principalAmount = 100_000.0, interestRate = 10.0,
        interestType = InterestType.REDUCING_BALANCE,
        tenureValue = total, tenureUnit = TenureUnit.MONTHS,
        monthlyEmi = 5_000.0, firstEmiDate = firstEmiDate,
        loanTakenDate = firstEmiDate, loanEndDate = 0,
        outstandingAmount = 100_000.0, completedEmis = completedEmis,
        totalEmis = total, status = status
    )

    private fun calendarMs(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply { set(year, month, day, 12, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

    @Test fun `future loan first EMI not yet due`() {
        val now = calendarMs(2026, 0, 15)
        val firstEmi = calendarMs(2026, 1, 15)   // next month
        val l = loan(firstEmi, completedEmis = 0)
        val m = MissedPaymentDetector.detect(l, now)
        assertThat(m.missedCount).isEqualTo(0)
    }

    @Test fun `first EMI due today and not paid`() {
        val now = calendarMs(2026, 5, 15)
        val firstEmi = calendarMs(2026, 5, 15)   // same day
        val l = loan(firstEmi, completedEmis = 0)
        val m = MissedPaymentDetector.detect(l, now)
        assertThat(m.missedCount).isEqualTo(1)
    }

    @Test fun `three months elapsed two paid means one missed`() {
        val firstEmi = calendarMs(2026, 0, 10)
        val now = calendarMs(2026, 2, 15)   // ~Mar 15 → 3 EMIs should be due
        val l = loan(firstEmi, completedEmis = 2)
        val m = MissedPaymentDetector.detect(l, now)
        assertThat(m.missedCount).isEqualTo(1)
    }

    @Test fun `day before due date means previous month is the latest due`() {
        val firstEmi = calendarMs(2026, 0, 20)   // 20th of every month
        val now = calendarMs(2026, 2, 15)        // Mar 15, BEFORE the 20th
        // So far: EMI #1 (Jan 20) + EMI #2 (Feb 20) due → 2 expected
        val l = loan(firstEmi, completedEmis = 2)
        val m = MissedPaymentDetector.detect(l, now)
        assertThat(m.missedCount).isEqualTo(0)
    }

    @Test fun `cibil warning after 2+ missed`() {
        val firstEmi = calendarMs(2026, 0, 10)
        val now = calendarMs(2026, 3, 15)   // ~4 EMIs due
        val l = loan(firstEmi, completedEmis = 1)
        val m = MissedPaymentDetector.detect(l, now)
        assertThat(m.missedCount).isAtLeast(2)
        assertThat(m.showCibilWarning).isTrue()
    }

    @Test fun `single missed does not warn cibil`() {
        val firstEmi = calendarMs(2026, 0, 10)
        val now = calendarMs(2026, 1, 15)
        val l = loan(firstEmi, completedEmis = 1)
        val m = MissedPaymentDetector.detect(l, now)
        assertThat(m.missedCount).isEqualTo(1)
        assertThat(m.showCibilWarning).isFalse()
    }

    @Test fun `completed loan is never missed`() {
        val firstEmi = calendarMs(2020, 0, 10)
        val now = calendarMs(2026, 0, 15)
        val l = loan(firstEmi, completedEmis = 24, status = LoanStatus.COMPLETED)
        val m = MissedPaymentDetector.detect(l, now)
        assertThat(m.missedCount).isEqualTo(0)
    }

    @Test fun `expected count capped at totalEmis`() {
        val firstEmi = calendarMs(2020, 0, 10)
        val now = calendarMs(2030, 0, 10)   // 10 years later
        val l = loan(firstEmi, completedEmis = 10, total = 24)
        val m = MissedPaymentDetector.detect(l, now)
        // expected capped at 24, completed = 10 → missed = 14
        assertThat(m.missedCount).isEqualTo(14)
    }

    @Test fun `penalty estimate is 2pct of EMI per missed`() {
        val firstEmi = calendarMs(2026, 0, 10)
        val now = calendarMs(2026, 2, 15)   // 3 expected
        val l = loan(firstEmi, completedEmis = 0)
        val m = MissedPaymentDetector.detect(l, now)
        // 3 missed * 5000 * 0.02 = 300
        assertThat(m.estimatedPenalty).isEqualTo(300.0)
    }
}
