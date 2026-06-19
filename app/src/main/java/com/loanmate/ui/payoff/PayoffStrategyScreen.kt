package com.loanmate.ui.payoff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.PayoffStrategyCalculator
import com.loanmate.utils.PayoffStrategyCalculator.Strategy
import com.loanmate.viewmodel.PayoffStrategyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayoffStrategyScreen(
    onBack: () -> Unit,
    viewModel: PayoffStrategyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var extraText by rememberSaveable { mutableStateOf("5000") }
    val extra = extraText.toDoubleOrNull() ?: 0.0

    val avalanche = remember(uiState.activeLoans, extra) {
        if (uiState.activeLoans.isEmpty()) null
        else PayoffStrategyCalculator.simulate(uiState.activeLoans, extra, Strategy.AVALANCHE)
    }
    val snowball = remember(uiState.activeLoans, extra) {
        if (uiState.activeLoans.isEmpty()) null
        else PayoffStrategyCalculator.simulate(uiState.activeLoans, extra, Strategy.SNOWBALL)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payoff Strategy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.activeLoans.size < 2) {
                EmptyState(loanCount = uiState.activeLoans.size)
                return@Column
            }

            Text(
                "How much extra can you pay each month?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = extraText,
                onValueChange = { extraText = it.filter { c -> c.isDigit() } },
                label = { Text("Extra cash per month") },
                leadingIcon = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (avalanche != null && snowball != null) {
                if (avalanche.capReached || snowball.capReached ||
                    avalanche.nonAmortizingLoans.isNotEmpty()) {
                    WarningBanner(avalanche.nonAmortizingLoans, avalanche.capReached || snowball.capReached)
                }
                Comparison(avalanche = avalanche, snowball = snowball)
                StrategyCard(
                    title = "🏔️ Avalanche",
                    subtitle = "Pay highest interest rate first",
                    plan = avalanche
                )
                StrategyCard(
                    title = "❄️ Snowball",
                    subtitle = "Pay smallest balance first",
                    plan = snowball
                )
            }
        }
    }
}

@Composable
private fun EmptyState(loanCount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📊", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (loanCount == 0) "Add a loan to see your payoff strategy."
                   else "Add another active loan to compare strategies.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WarningBanner(nonAmortizing: List<String>, capReached: Boolean) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(8.dp))
            Column {
                if (nonAmortizing.isNotEmpty()) {
                    Text(
                        "These loans never finish at the current EMI: ${nonAmortizing.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                if (capReached) {
                    Text(
                        "Simulation capped at 50 years. Numbers below may understate true payoff time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun Comparison(
    avalanche: PayoffStrategyCalculator.PayoffPlan,
    snowball: PayoffStrategyCalculator.PayoffPlan
) {
    val moneyDelta = snowball.totalInterestPaid - avalanche.totalInterestPaid
    val firstWinDelta = snowball.timeline.firstOrNull()?.finishedInMonth?.let { snowFirst ->
        avalanche.timeline.firstOrNull()?.finishedInMonth?.let { avaFirst ->
            avaFirst - snowFirst
        }
    } ?: 0
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("💡 Quick comparison", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(8.dp))
            if (moneyDelta > 0) {
                Text("• Avalanche saves ${CurrencyUtils.format(moneyDelta)} in interest.",
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            if (firstWinDelta > 0) {
                Text("• Snowball clears your first loan $firstWinDelta months sooner.",
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(
                "Pick what motivates you — both work.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StrategyCard(
    title: String,
    subtitle: String,
    plan: PayoffStrategyCalculator.PayoffPlan
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatBlock("Debt-free in", "${plan.totalMonths} mo")
                StatBlock("Total interest", CurrencyUtils.formatShort(plan.totalInterestPaid))
                StatBlock("Total paid", CurrencyUtils.formatShort(plan.totalPaid))
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("Payoff order", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            plan.timeline.forEachIndexed { i, item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${i + 1}. ${item.loanName}", style = MaterialTheme.typography.bodyMedium)
                    Text("Month ${item.finishedInMonth}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold)
    }
}
