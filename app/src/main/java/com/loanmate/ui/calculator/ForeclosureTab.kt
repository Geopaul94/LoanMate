package com.loanmate.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loanmate.data.local.LoanEntity
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.ForeclosureCalculator

@Composable
fun ForeclosureTab(loan: LoanEntity, hideValues: Boolean = false) {
    var chargePercentText by rememberSaveable { mutableStateOf("2") }
    val remainingMonths = (loan.totalEmis - loan.completedEmis).coerceAtLeast(0)
    val chargePercent = chargePercentText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0

    val result = remember(chargePercent, loan) {
        ForeclosureCalculator.calculate(
            outstanding = loan.outstandingAmount,
            currentEmi = loan.monthlyEmi,
            remainingMonths = remainingMonths,
            foreclosureChargePercent = chargePercent
        )
    }

    Column(modifier = Modifier.padding(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Full Foreclosure",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Close your loan today and stop paying future interest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PercentField(
                    value = chargePercentText,
                    onChange = { chargePercentText = it },
                    label = "Bank Foreclosure Fee"
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(
                        "RBI rules exempt most floating-rate home loans from these fees.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        VerdictHeroCard(result, hideValues)

        PremiumBreakdownCard(result, hideValues)

        Text(
            text = "Note: Consider the opportunity cost. If you can invest this lump sum at a rate higher than your loan's interest, investing might be better than foreclosing.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun VerdictHeroCard(result: ForeclosureCalculator.Result, hideValues: Boolean) {
    val isWorth = result.isWorthIt
    val primaryColor = if (isWorth) com.loanmate.ui.theme.SuccessGreen else MaterialTheme.colorScheme.error
    
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(2.dp, primaryColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = primaryColor.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isWorth) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isWorth) "Financially Beneficial" else "High Closing Cost",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = primaryColor
                )
                Text(
                    if (isWorth) "You save more on interest than the bank fees." else "Bank fees outweigh your interest savings.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isWorth) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = primaryColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Savings", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        Text(CurrencyUtils.format(result.netBenefit, hideValues), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBreakdownCard(result: ForeclosureCalculator.Result, hideValues: Boolean) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Closing Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BreakdownRow("Outstanding Principal", CurrencyUtils.format(result.outstanding, hideValues))
                BreakdownRow("Foreclosure Fees", "+ ${CurrencyUtils.format(result.foreclosureCharges, hideValues)}", color = MaterialTheme.colorScheme.error)
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Closing Cost", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(CurrencyUtils.format(result.totalPayable, hideValues), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("If you don't close today:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    BreakdownRow("Future EMI Total", CurrencyUtils.format(result.futureTotalPayments, hideValues), small = true)
                    BreakdownRow("Avoidable Interest", CurrencyUtils.format(result.interestSaved, hideValues), small = true, color = com.loanmate.ui.theme.SuccessGreen)
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, color: Color = Color.Unspecified, small: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
