package com.loanmate.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanmate.ui.components.loanTypeColor
import com.loanmate.utils.CurrencyUtils
import com.loanmate.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { OverviewSection(uiState = uiState) }
            item { LoanDistributionSection(uiState = uiState) }
            item { MonthlyEmiSection(uiState = uiState) }
        }
    }
}

@Composable
private fun OverviewSection(uiState: com.loanmate.viewmodel.AnalyticsUiState) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Financial Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            AnalyticsRow("Total Principal Borrowed", CurrencyUtils.format(uiState.totalPrincipal), MaterialTheme.colorScheme.primary)
            AnalyticsRow("Total Amount Paid", CurrencyUtils.format(uiState.totalPaid), Color(0xFF2E7D32))
            AnalyticsRow("Total Outstanding", CurrencyUtils.format(uiState.totalOutstanding), Color(0xFFD32F2F))
            AnalyticsRow("Monthly EMI Commitment", CurrencyUtils.format(uiState.totalMonthlyEmi), Color(0xFFE65100))

            Spacer(Modifier.height(4.dp))

            if (uiState.totalPrincipal > 0) {
                val paidPercent = (uiState.totalPaid / uiState.totalPrincipal * 100).toInt()
                Text("Overall Repayment: $paidPercent%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(
                    progress = { uiState.totalPaid.toFloat() / uiState.totalPrincipal.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
private fun LoanDistributionSection(uiState: com.loanmate.viewmodel.AnalyticsUiState) {
    if (uiState.loansByType.isEmpty()) return

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Loan Category Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            uiState.loansByType.forEach { (type, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(type.emoji, style = MaterialTheme.typography.bodyMedium)
                        Text(type.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = loanTypeColor(type).copy(alpha = 0.15f)
                    ) {
                        Text(
                            "$count loan${if (count > 1) "s" else ""}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = loanTypeColor(type),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyEmiSection(uiState: com.loanmate.viewmodel.AnalyticsUiState) {
    val activeLoans = uiState.loans.filter { it.status == com.loanmate.data.model.LoanStatus.ACTIVE }
    if (activeLoans.isEmpty()) return

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Active Loan EMI Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            activeLoans.forEach { loan ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(loan.loanName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(loan.bankName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(CurrencyUtils.format(loan.monthlyEmi), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                if (uiState.totalMonthlyEmi > 0) {
                    LinearProgressIndicator(
                        progress = { (loan.monthlyEmi / uiState.totalMonthlyEmi).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = loanTypeColor(loan.loanType)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
