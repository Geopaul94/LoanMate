package com.loanmate.utils

import com.google.common.truth.Truth.assertThat
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.InterestType
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.model.LoanType
import com.loanmate.data.model.TenureUnit
import com.loanmate.utils.PayoffStrategyCalculator.Strategy
import org.junit.Test

class PayoffStrategyCalculatorTest {

    private fun loan(
        id: Long, name: String, outstanding: Double, rate: Double, emi: Double
    ) = LoanEntity(
        id = id, loanName = name, bankName = "Bank",
        loanType = LoanType.PERSONAL,
        principalAmount = outstanding, interestRate = rate,
        interestType = InterestType.REDUCING_BALANCE,
        tenureValue = 36, tenureUnit = TenureUnit.MONTHS,
        monthlyEmi = emi, firstEmiDate = 0L, loanTakenDate = 0L, loanEndDate = 0L,
        outstandingAmount = outstanding, totalEmis = 36, status = LoanStatus.ACTIVE
    )

    @Test fun `avalanche prioritises high-rate loan`() {
        val loans = listOf(
            loan(1, "A-HighRate", 100_000.0, 18.0, 5_000.0),
            loan(2, "B-LowRate", 100_000.0, 8.0, 5_000.0)
        )
        val plan = PayoffStrategyCalculator.simulate(loans, 5_000.0, Strategy.AVALANCHE)
        // A finishes first (extra cash always to A because its rate is higher)
        assertThat(plan.timeline.first().loanName).isEqualTo("A-HighRate")
    }

    @Test fun `snowball prioritises low-balance loan`() {
        val loans = listOf(
            loan(1, "A-Big", 200_000.0, 8.0, 5_000.0),
            loan(2, "B-Small", 30_000.0, 18.0, 2_000.0)
        )
        val plan = PayoffStrategyCalculator.simulate(loans, 5_000.0, Strategy.SNOWBALL)
        // B finishes first (smaller balance)
        assertThat(plan.timeline.first().loanName).isEqualTo("B-Small")
    }

    @Test fun `avalanche saves more interest than snowball generally`() {
        val loans = listOf(
            loan(1, "Personal", 100_000.0, 14.0, 5_000.0),
            loan(2, "Bike", 50_000.0, 9.0, 3_000.0),
            loan(3, "Car", 300_000.0, 10.0, 8_000.0)
        )
        val ava = PayoffStrategyCalculator.simulate(loans, 5_000.0, Strategy.AVALANCHE)
        val snow = PayoffStrategyCalculator.simulate(loans, 5_000.0, Strategy.SNOWBALL)
        assertThat(ava.totalInterestPaid).isLessThan(snow.totalInterestPaid)
    }

    @Test fun `non-amortizing loan flagged`() {
        // EMI 100 on a 100K loan at 36% → monthly interest = 3000 → never amortizes
        val loans = listOf(loan(1, "Broken", 100_000.0, 36.0, 100.0))
        val plan = PayoffStrategyCalculator.simulate(loans, 0.0, Strategy.AVALANCHE)
        assertThat(plan.nonAmortizingLoans).contains("Broken")
        // With no extra cash, should hit the 600-month cap
        assertThat(plan.capReached).isTrue()
    }

    @Test fun `extra cash on non-amortizing loan eventually clears it`() {
        val loans = listOf(loan(1, "Broken", 100_000.0, 36.0, 100.0))
        val plan = PayoffStrategyCalculator.simulate(loans, 10_000.0, Strategy.AVALANCHE)
        assertThat(plan.timeline).hasSize(1)
        assertThat(plan.capReached).isFalse()
    }

    @Test fun `negative extra cash treated as zero`() {
        val loans = listOf(loan(1, "Std", 100_000.0, 10.0, 5_000.0))
        val plan = PayoffStrategyCalculator.simulate(loans, -1000.0, Strategy.AVALANCHE)
        assertThat(plan.timeline).hasSize(1)   // still finishes via base EMI
    }

    @Test fun `empty loans returns empty plan`() {
        val plan = PayoffStrategyCalculator.simulate(emptyList(), 5_000.0, Strategy.AVALANCHE)
        assertThat(plan.totalMonths).isEqualTo(0)
        assertThat(plan.timeline).isEmpty()
    }

    @Test fun `garbage loans filtered out`() {
        val loans = listOf(
            loan(1, "Good", 100_000.0, 10.0, 5_000.0),
            loan(2, "ZeroEmi", 50_000.0, 10.0, 0.0),
            loan(3, "NegRate", 50_000.0, -5.0, 2_000.0)
        )
        val plan = PayoffStrategyCalculator.simulate(loans, 0.0, Strategy.AVALANCHE)
        assertThat(plan.timeline.map { it.loanName }).containsExactly("Good")
    }
}
