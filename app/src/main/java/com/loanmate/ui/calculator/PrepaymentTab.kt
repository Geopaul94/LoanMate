package com.loanmate.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loanmate.data.local.LoanEntity
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.PrepaymentCalculator

@Composable
fun PrepaymentTab(loan: LoanEntity, hideValues: Boolean = false) {
    var prepaymentValue by rememberSaveable { mutableStateOf(0f) }
    var mode by rememberSaveable { mutableStateOf(PrepaymentCalculator.Mode.REDUCE_TENURE) }
    val remainingMonths = (loan.totalEmis - loan.completedEmis).coerceAtLeast(0)

    val prepayment = prepaymentValue.toDouble()
    val result = remember(prepayment, mode, loan) {
        if (prepayment <= 0 || remainingMonths <= 0) null
        else PrepaymentCalculator.calculate(
            outstanding = loan.outstandingAmount,
            annualRatePercent = loan.interestRate,
            currentEmi = loan.monthlyEmi,
            remainingMonths = remainingMonths,
            prepaymentAmount = prepayment.coerceAtMost(loan.outstandingAmount),
            mode = mode
        )
    }

    Column(modifier = Modifier.padding(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Simulate Prepayment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Paying extra today significantly reduces your long-term interest burden.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("Prepayment Amount", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(CurrencyUtils.format(prepayment), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }

                Slider(
                    value = prepaymentValue,
                    onValueChange = { prepaymentValue = it },
                    valueRange = 0f..loan.outstandingAmount.toFloat(),
                    steps = 20,
                    modifier = Modifier.fillMaxWidth()
                )

                QuickAmountChips(
                    outstanding = loan.outstandingAmount,
                    onPick = { prepaymentValue = it.toFloat() }
                )
            }
        }

        Text("Select Your Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StrategyCard(
                title = "Finish Earlier",
                subtitle = "Reduce tenure",
                icon = Icons.Default.Timer,
                selected = mode == PrepaymentCalculator.Mode.REDUCE_TENURE,
                onClick = { mode = PrepaymentCalculator.Mode.REDUCE_TENURE },
                modifier = Modifier.weight(1f)
            )
            StrategyCard(
                title = "Lower EMI",
                subtitle = "Reduce monthly",
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                selected = mode == PrepaymentCalculator.Mode.REDUCE_EMI,
                onClick = { mode = PrepaymentCalculator.Mode.REDUCE_EMI },
                modifier = Modifier.weight(1f)
            )
        }

        result?.let { PremiumResultCard(it, prepayment, loan.monthlyEmi, hideValues) }
    }
}

@Composable
private fun StrategyCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon, null, 
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title, 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle, 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickAmountChips(outstanding: Double, onPick: (Double) -> Unit) {
    val standard = listOf(25000.0, 50000.0, 100000.0, 500000.0).filter { it <= outstanding }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        standard.forEach { amount ->
            SuggestChip(
                label = CurrencyUtils.formatShort(amount),
                onClick = { onPick(amount) }
            )
        }
    }
}

@Composable
private fun SuggestChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        modifier = Modifier.height(32.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PremiumResultCard(result: PrepaymentCalculator.Result, prepayment: Double, currentEmi: Double, hideValues: Boolean) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ElectricBolt, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(20.dp))
                    }
                }
                Text("Impact of Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (result.mode) {
                    PrepaymentCalculator.Mode.REDUCE_TENURE -> {
                        ImpactRow("Time Saved", monthsLabel(result.monthsSaved), MaterialTheme.colorScheme.primary)
                        ImpactRow("Interest Saved", CurrencyUtils.format(result.interestSaved), com.loanmate.ui.theme.SuccessGreen)
                        ImpactRow("New End Date", "${result.newRemainingMonths} months left", MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    PrepaymentCalculator.Mode.REDUCE_EMI -> {
                        ImpactRow("EMI Reduction", CurrencyUtils.format(currentEmi - result.newEmi, hideValues), MaterialTheme.colorScheme.primary)
                        ImpactRow("New Monthly EMI", CurrencyUtils.format(result.newEmi, hideValues), com.loanmate.ui.theme.SuccessGreen)
                        ImpactRow("Total Benefit", CurrencyUtils.format(result.totalSavings, hideValues), MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.05f))
                    .padding(16.dp)
            ) {
                Text(
                    text = "By paying ${CurrencyUtils.format(prepayment, hideValues)} now, you effectively save ${CurrencyUtils.format(result.totalSavings, hideValues)} over the remaining loan period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ImpactRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black, color = valueColor)
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
