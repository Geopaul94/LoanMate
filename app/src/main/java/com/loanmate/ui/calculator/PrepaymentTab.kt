package com.loanmate.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loanmate.data.local.LoanEntity
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.PrepaymentCalculator

@Composable
fun PrepaymentTab(loan: LoanEntity) {
    var prepaymentText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(PrepaymentCalculator.Mode.REDUCE_TENURE) }
    val remainingMonths = (loan.totalEmis - loan.completedEmis).coerceAtLeast(0)

    val prepayment = prepaymentText.toDoubleOrNull() ?: 0.0
    val result = remember(prepayment, mode, loan) {
        if (prepayment <= 0) null
        else PrepaymentCalculator.calculate(
            outstanding = loan.outstandingAmount,
            annualRatePercent = loan.interestRate,
            currentEmi = loan.monthlyEmi,
            remainingMonths = remainingMonths,
            prepaymentAmount = prepayment.coerceAtMost(loan.outstandingAmount),
            mode = mode
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "What if you pay extra this month?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        MoneyField(
            value = prepaymentText,
            onChange = { prepaymentText = it },
            label = "Prepayment amount"
        )

        // Quick chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10000.0, 50000.0, 100000.0).forEach { amount ->
                if (amount <= loan.outstandingAmount) {
                    AssistChip(
                        onClick = { prepaymentText = amount.toInt().toString() },
                        label = { Text("+${CurrencyUtils.formatShort(amount)}") }
                    )
                }
            }
        }

        // Mode toggle
        Text("Strategy", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == PrepaymentCalculator.Mode.REDUCE_TENURE,
                onClick = { mode = PrepaymentCalculator.Mode.REDUCE_TENURE },
                label = { Text("Finish earlier") }
            )
            FilterChip(
                selected = mode == PrepaymentCalculator.Mode.REDUCE_EMI,
                onClick = { mode = PrepaymentCalculator.Mode.REDUCE_EMI },
                label = { Text("Lower EMI") }
            )
        }

        result?.let { ResultCard(it, prepayment) }
    }
}

@Composable
private fun ResultCard(result: PrepaymentCalculator.Result, prepayment: Double) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📊 Your savings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Spacer(Modifier.height(12.dp))

            when (result.mode) {
                PrepaymentCalculator.Mode.REDUCE_TENURE -> {
                    ResultRow("Loan finishes in", "${result.newRemainingMonths} months (was ${result.originalRemainingMonths})")
                    ResultRow("You save", "${monthsLabel(result.monthsSaved)}", isHighlight = true)
                    ResultRow("Interest saved", CurrencyUtils.format(result.interestSaved), isHighlight = true)
                }
                PrepaymentCalculator.Mode.REDUCE_EMI -> {
                    ResultRow("New EMI", CurrencyUtils.format(result.newEmi), isHighlight = true)
                    val monthlySaving = (result.originalRemainingPayments / result.originalRemainingMonths) - result.newEmi
                    ResultRow("Monthly saving", CurrencyUtils.format(monthlySaving))
                    ResultRow("Total saving over tenure", CurrencyUtils.format(result.totalSavings), isHighlight = true)
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "You pay ${CurrencyUtils.format(prepayment)} now, save ${CurrencyUtils.format(result.totalSavings)} overall.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun monthsLabel(months: Int): String {
    val years = months / 12
    val rem = months % 12
    return buildString {
        if (years > 0) append("$years yr${if (years > 1) "s" else ""} ")
        if (rem > 0 || years == 0) append("$rem mo${if (rem > 1) "s" else ""}")
    }.trim()
}
