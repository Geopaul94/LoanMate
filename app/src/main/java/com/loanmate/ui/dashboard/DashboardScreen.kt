package com.loanmate.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanmate.data.model.LoanStatus
import com.loanmate.ui.components.LoanProgressCard
import com.loanmate.ui.components.SummaryCard
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.DateUtils
import com.loanmate.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddLoan: () -> Unit,
    onLoanClick: (Long) -> Unit,
    onAnalytics: () -> Unit,
    onSettings: () -> Unit,
    onAchievements: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddLoan,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Loan") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DashboardHeader(
                    onAnalytics = onAnalytics,
                    onSettings = onSettings,
                    onAchievements = onAchievements
                )
            }

            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange
                )
            }

            item {
                SummarySection(uiState = uiState)
            }

            item {
                Text(
                    text = "Active Loans",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val activeLoans = uiState.loans.filter { it.status == LoanStatus.ACTIVE }
            if (activeLoans.isEmpty()) {
                item { EmptyLoansState() }
            } else {
                items(activeLoans, key = { it.id }) { loan ->
                    LoanProgressCard(
                        loan = loan,
                        onClick = { onLoanClick(loan.id) }
                    )
                }
            }

            val completedLoans = uiState.loans.filter { it.status == LoanStatus.COMPLETED }
            if (completedLoans.isNotEmpty()) {
                item {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(completedLoans, key = { "completed_${it.id}" }) { loan ->
                    LoanProgressCard(loan = loan, onClick = { onLoanClick(loan.id) })
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun DashboardHeader(
    onAnalytics: () -> Unit,
    onSettings: () -> Unit,
    onAchievements: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${DateUtils.getGreeting()}, 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Small payments today create big freedom tomorrow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row {
            IconButton(onClick = onAchievements) {
                Icon(Icons.Default.EmojiEvents, contentDescription = "Achievements")
            }
            IconButton(onClick = onAnalytics) {
                Icon(Icons.Default.BarChart, contentDescription = "Analytics")
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search loans or banks...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SummarySection(uiState: com.loanmate.viewmodel.DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(
                title = "Active Loans",
                value = uiState.activeLoanCount.toString(),
                icon = Icons.Default.AccountBalance,
                iconTint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Completed",
                value = uiState.completedLoansCount.toString(),
                icon = Icons.Default.CheckCircle,
                iconTint = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(
                title = "Total Outstanding",
                value = CurrencyUtils.formatShort(uiState.totalOutstanding),
                icon = Icons.Default.MoneyOff,
                iconTint = Color(0xFFD32F2F),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Monthly EMI",
                value = CurrencyUtils.formatShort(uiState.totalMonthlyEmi),
                icon = Icons.Default.Schedule,
                iconTint = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EmptyLoansState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "🏦", style = MaterialTheme.typography.displayMedium)
        Text(
            text = "Add your first loan and start your journey.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
