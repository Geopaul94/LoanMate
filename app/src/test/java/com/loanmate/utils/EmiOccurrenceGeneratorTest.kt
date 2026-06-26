package com.loanmate.utils

import com.google.common.truth.Truth.assertThat
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.InterestType
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.model.LoanType
import com.loanmate.data.model.TenureUnit
import org.junit.Test
import java.util.Calendar

class EmiOccurrenceGeneratorTest {

    private fun loan(
        id: Long, name: String, firstEmiDate: Long,
        totalEmis: Int, completedEmis: Int,
        status: LoanStatus = LoanStatus.ACTIVE
    ) = LoanEntity(
        id = id, loanName = name, bankName = "B",
        loanType = LoanType.PERSONAL,
        principalAmount = 100_000.0, interestRate = 10.0,
        interestType = InterestType.REDUCING_BALANCE,
        tenureValue = totalEmis, tenureUnit = TenureUnit.MONTHS,
        monthlyEmi = 5_000.0, firstEmiDate = firstEmiDate,
        loanTakenDate = firstEmiDate, loanEndDate = 0,
        outstandingAmount = 100_000.0, completedEmis = completedEmis,
        totalEmis = totalEmis, status = status
    )

    private fun nextMonthSameDay(now: Long): Long =
        Calendar.getInstance().apply { timeInMillis = now; add(Calendar.MONTH, 1) }.timeInMillis

    @Test fun `inactive loans excluded`() {
        val now = System.currentTimeMillis()
        val loan = loan(1, "Done", now, 12, 12, status = LoanStatus.COMPLETED)
        val occurrences = EmiOccurrenceGenerator.forActiveLoans(listOf(loan))
        assertThat(occurrences).isEmpty()
    }

    @Test fun `fully-paid active loan excluded`() {
        val now = System.currentTimeMillis()
        val loan = loan(1, "AllPaid", now - 365L * 86400_000, totalEmis = 12, completedEmis = 12)
        val occurrences = EmiOccurrenceGenerator.forActiveLoans(listOf(loan))
        assertThat(occurrences).isEmpty()
    }

    @Test fun `generates occurrences for unpaid future EMIs`() {
        val firstEmi = nextMonthSameDay(System.currentTimeMillis())
        val loan = loan(1, "Active", firstEmi, totalEmis = 12, completedEmis = 0)
        val occurrences = EmiOccurrenceGenerator.forActiveLoans(listOf(loan), horizonMonths = 24)
        assertThat(occurrences).isNotEmpty()
        // All belong to this loan
        assertThat(occurrences.map { it.loanId }.distinct()).containsExactly(1L)
    }

    @Test fun `respects horizon limit`() {
        val firstEmi = System.currentTimeMillis() + 86400_000L
        val loan = loan(1, "Big", firstEmi, totalEmis = 240, completedEmis = 0)
        val occ24 = EmiOccurrenceGenerator.forActiveLoans(listOf(loan), horizonMonths = 24)
        val occ6 = EmiOccurrenceGenerator.forActiveLoans(listOf(loan), horizonMonths = 6)
        assertThat(occ6.size).isLessThan(occ24.size)
    }

    @Test fun `multiple loans interleaved`() {
        val firstEmi = System.currentTimeMillis() + 86400_000L
        val a = loan(1, "A", firstEmi, 12, 0)
        val b = loan(2, "B", firstEmi + 86400_000L, 12, 0)
        val occ = EmiOccurrenceGenerator.forActiveLoans(listOf(a, b))
        val byLoan = occ.groupBy { it.loanId }
        assertThat(byLoan).containsKey(1L)
        assertThat(byLoan).containsKey(2L)
    }
}
