package com.loanmate.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanmate.data.model.LoanStatus
import com.loanmate.navigation.SAVED_STATE_DELETED_LOAN_ID
import com.loanmate.ui.components.DebtFreeCountdownCard
import com.loanmate.ui.components.EmptyState
import com.loanmate.ui.components.LoanProgressCard
import com.loanmate.ui.components.StreakChip
import com.loanmate.ui.components.SummaryCard
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.DateUtils
import com.loanmate.viewmodel.DashboardViewModel
import com.loanmate.worker.DeleteCleanupWorker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddLoan: () -> Unit,
    onLoanClick: (Long) -> Unit,
    onSettings: () -> Unit,
    savedStateHandle: SavedStateHandle? = null,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Watch for a deleted-loan handoff from LoanDetailsScreen
    LaunchedEffect(savedStateHandle) {
        savedStateHandle
            ?.getStateFlow<Long?>(SAVED_STATE_DELETED_LOAN_ID, null)
            ?.collect { deletedId ->
                if (deletedId != null && deletedId > 0) {
                    savedStateHandle[SAVED_STATE_DELETED_LOAN_ID] = null
                    DeleteCleanupWorker.schedule(context, deletedId)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Loan deleted",
                            actionLabel = "UNDO",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreLoan(deletedId)
                            DeleteCleanupWorker.cancel(context, deletedId)
                        }
                    }
                }
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LoanMate",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddLoan,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
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
            }

            if (uiState.currentStreak > 0 || uiState.longestStreak > 0) {
                item {
                    StreakChip(current = uiState.currentStreak, longest = uiState.longestStreak)
                }
            }

            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange
                )
            }

            uiState.debtFreeDate?.let { dfDate ->
                item {
                    PremiumDebtFreeCard(debtFreeDateMs = dfDate)
                }
            }

            uiState.prepaymentInsight?.let { insight ->
                item {
                    com.loanmate.ui.components.SmartInsightCard(
                        insight = insight,
                        onAmountChange = viewModel::onExtraAmountChange
                    )
                }
            }

            item {
                SummarySection(uiState = uiState)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Loans",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    TextButton(onClick = { /* View all */ }) {
                        Text("View All", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            val activeLoans = uiState.loans.filter { it.status == LoanStatus.ACTIVE }
            val completedLoans = uiState.loans.filter { it.status == LoanStatus.COMPLETED }
            
            if (uiState.loans.isEmpty() && !uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Always show the nudge if loans are empty, even if not configured yet
                        // This ensures the user sees the option on a fresh install
                        com.loanmate.ui.components.BackupNudgeCard(
                            onRestore = onSettings,
                            onSettings = onSettings
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 40.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        EmptyState(
                            emoji = "🏦",
                            title = "Your journey starts here",
                            message = "Add your first loan to start tracking your path to financial freedom.",
                            ctaLabel = "Add Loan",
                            onCta = onAddLoan
                        )
                    }
                }
            } else if (activeLoans.isNotEmpty()) {
                items(activeLoans, key = { it.id }) { loan ->
                    PremiumLoanCard(
                        loan = loan,
                        hideValues = uiState.hideValues,
                        onClick = { onLoanClick(loan.id) }
                    )
                }
            }
            
            if (completedLoans.isNotEmpty()) {
                item {
                    Text(
                        text = "Completed Loans",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(completedLoans, key = { "completed_${it.id}" }) { loan ->
                    PremiumLoanCard(
                        loan = loan,
                        hideValues = uiState.hideValues,
                        onClick = { onLoanClick(loan.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun PremiumDebtFreeCard(debtFreeDateMs: Long) {
    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎉", fontSize = 20.sp)
                        }
                    }
                    Text(
                        "Freedom Countdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                DebtFreeCountdownCard(debtFreeDateMs = debtFreeDateMs)
            }
        }
    }
}

@Composable
private fun PremiumLoanCard(
    loan: com.loanmate.data.local.LoanEntity,
    hideValues: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = com.loanmate.ui.components.loanTypeColor(loan.loanType).copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(loan.loanType.emoji, fontSize = 24.sp)
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(loan.loanName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text(loan.bankName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    CurrencyUtils.formatShort(loan.outstandingAmount, hideValues),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Outstanding",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummarySection(uiState: com.loanmate.viewmodel.DashboardUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumSummaryCard(
            title = "Outstanding",
            value = CurrencyUtils.formatShort(uiState.totalOutstanding, uiState.hideValues),
            icon = Icons.Default.Payments,
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            contentColor = com.loanmate.ui.theme.DangerRed
        )
        PremiumSummaryCard(
            title = "Monthly EMI",
            value = CurrencyUtils.formatShort(uiState.totalMonthlyEmi, uiState.hideValues),
            icon = Icons.Default.EventRepeat,
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PremiumSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(24.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = contentColor)
                Text(title, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.7f))
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


