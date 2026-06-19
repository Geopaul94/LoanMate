package com.loanmate.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loanmate.data.local.LoanEntity
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.ForeclosureCalculator

@Composable
fun ForeclosureTab(loan: LoanEntity) {
    var chargePercentText by remember { mutableStateOf("2") }
    val remainingMonths = (loan.totalEmis - loan.completedEmis).coerceAtLeast(0)
    val chargePercent = chargePercentText.toDoubleOrNull() ?: 0.0

    val result = remember(chargePercent, loan) {
        ForeclosureCalculator.calculate(
            outstanding = loan.outstandingAmount,
            currentEmi = loan.monthlyEmi,
            remainingMonths = remainingMonths,
            foreclosureChargePercent = chargePercent
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Close the loan today?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Most banks charge 2-5% as foreclosure fee on the outstanding amount. " +
                    "Floating-rate home loans (after 2014) are exempt by RBI rules.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PercentField(
            value = chargePercentText,
            onChange = { chargePercentText = it },
            label = "Foreclosure charges"
        )

        VerdictCard(result)

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                ResultRow("Outstanding amount", CurrencyUtils.format(result.outstanding))
                ResultRow("Foreclosure charges", "+ ${CurrencyUtils.format(result.foreclosureCharges)}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ResultRow("Pay today (lump sum)", CurrencyUtils.format(result.totalPayable), isHighlight = true)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                ResultRow("If you continue paying EMIs", CurrencyUtils.format(result.futureTotalPayments))
                ResultRow("Total interest you avoid", CurrencyUtils.format(result.interestSaved))
                ResultRow(
                    "Net benefit",
                    (if (result.netBenefit >= 0) "" else "−") + CurrencyUtils.format(kotlin.math.abs(result.netBenefit)),
                    isHighlight = true
                )
            }
        }
    }
}

@Composable
private fun VerdictCard(result: ForeclosureCalculator.Result) {
    val isWorth = result.isWorthIt
    val containerColor = if (isWorth) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val iconTint = if (isWorth) Color(0xFF2E7D32) else Color(0xFFE65100)
    val verdict = if (isWorth) "Foreclosing saves you money"
                  else "Foreclosure may not be worth it"
    val detail = if (isWorth)
        "You save ${CurrencyUtils.format(result.netBenefit)} overall."
    else
        "Charges nearly cancel out the interest savings. Compare with investment returns first."

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isWorth) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(verdict, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
