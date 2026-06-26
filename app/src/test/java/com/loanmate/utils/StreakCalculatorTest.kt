package com.loanmate.utils

import com.google.common.truth.Truth.assertThat
import com.loanmate.data.local.PaymentHistoryEntity
import org.junit.Test

class StreakCalculatorTest {

    private fun payment(id: Long, dueDate: Long, paidDate: Long) = PaymentHistoryEntity(
        id = id, loanId = 1L, emiNumber = id.toInt(),
        amountPaid = 5_000.0, principalComponent = 4_000.0,
        interestComponent = 1_000.0, remainingBalance = 0.0,
        dueDate = dueDate, paidDate = paidDate
    )

    @Test fun `empty list returns zero streak`() {
        val s = StreakCalculator.calculate(emptyList())
        assertThat(s.current).isEqualTo(0)
        assertThat(s.longest).isEqualTo(0)
    }

    @Test fun `all on-time gives full streak`() {
        val payments = (1L..5L).map { i ->
            payment(i, dueDate = i * 1000, paidDate = i * 1000 - 100)  // paid before due
        }
        val s = StreakCalculator.calculate(payments)
        assertThat(s.current).isEqualTo(5)
        assertThat(s.longest).isEqualTo(5)
    }

    @Test fun `latest payment late breaks current but longest preserved`() {
        val day = 24L * 60 * 60 * 1000
        val payments = listOf(
            payment(1, dueDate = 10 * day, paidDate = 10 * day),       // on time
            payment(2, dueDate = 20 * day, paidDate = 20 * day),       // on time
            payment(3, dueDate = 30 * day, paidDate = 30 * day),       // on time
            payment(4, dueDate = 40 * day, paidDate = 45 * day)        // 5 days late ← newest
        )
        val s = StreakCalculator.calculate(payments)
        assertThat(s.current).isEqualTo(0)
        assertThat(s.longest).isEqualTo(3)
    }

    @Test fun `grace window of 24h allows same-day late payment`() {
        val due = 1_000_000L
        // paid 23 hours after due → within grace
        val grace = payment(1, dueDate = due, paidDate = due + 23 * 60 * 60 * 1000)
        val s = StreakCalculator.calculate(listOf(grace))
        assertThat(s.current).isEqualTo(1)
    }

    @Test fun `unsorted input still produces correct streak`() {
        val day = 24L * 60 * 60 * 1000
        val payments = listOf(
            payment(3, dueDate = 30 * day, paidDate = 30 * day),
            payment(1, dueDate = 10 * day, paidDate = 10 * day),
            payment(2, dueDate = 20 * day, paidDate = 20 * day)
        )
        val s = StreakCalculator.calculate(payments)
        assertThat(s.current).isEqualTo(3)
        assertThat(s.longest).isEqualTo(3)
    }

    @Test fun `late in middle splits into two runs longest wins`() {
        val day = 24L * 60 * 60 * 1000
        val payments = listOf(
            payment(1, dueDate = 10 * day, paidDate = 10 * day),
            payment(2, dueDate = 20 * day, paidDate = 20 * day),
            payment(3, dueDate = 30 * day, paidDate = 40 * day),       // late
            payment(4, dueDate = 50 * day, paidDate = 50 * day),
            payment(5, dueDate = 60 * day, paidDate = 60 * day),
            payment(6, dueDate = 70 * day, paidDate = 70 * day),
            payment(7, dueDate = 80 * day, paidDate = 80 * day)        // newest, on time
        )
        val s = StreakCalculator.calculate(payments)
        assertThat(s.current).isEqualTo(4)   // last 4 on time
        assertThat(s.longest).isEqualTo(4)
    }
}
