package com.loanmate.ui.loan.details

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.local.PaymentHistoryEntity
import com.loanmate.data.model.LoanStatus
import com.loanmate.ui.components.LoanTypeIcon
import com.loanmate.ui.components.MilestoneCard
import com.loanmate.ui.components.loanTypeColor
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.DateUtils
import com.loanmate.utils.EmiCalculator
import com.loanmate.viewmodel.LoanDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailsScreen(
    loanId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: LoanDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loanId) { viewModel.loadLoan(loanId) }
    LaunchedEffect(Unit) {
        viewModel.deleteEvent.collect { onBack() }
    }

    val loan = uiState.loan

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loan?.loanName ?: "Loan Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (loan == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AnimatedVisibility(visible = uiState.showMilestone) {
                    uiState.milestoneMessage?.let { msg ->
                        MilestoneCard(message = msg, onDismiss = viewModel::dismissMilestone)
                    }
                }
            }

            item { LoanHeaderCard(loan = loan) }

            item { LoanProgressSection(loan = loan) }

            item { LoanStatsSection(loan = loan) }

            if (loan.status == LoanStatus.ACTIVE) {
                item {
                    Button(
                        onClick = { viewModel.markEmiPaid(loan) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Mark EMI Paid", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            if (uiState.payments.isNotEmpty()) {
                item {
                    Text(
                        "Payment History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.payments.reversed(), key = { it.id }) { payment ->
                    PaymentHistoryItem(payment = payment)
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Your payment history will appear here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Loan") },
            text = { Text("Are you sure you want to delete \"${loan?.loanName}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        loan?.let { viewModel.deleteLoan(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LoanHeaderCard(loan: LoanEntity) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LoanTypeIcon(loanType = loan.loanType, size = 56.dp)
            Column {
                Text(loan.loanName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(loan.bankName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(loan.loanType.displayName) }
                )
            }
        }
    }
}

@Composable
private fun LoanProgressSection(loan: LoanEntity) {
    val progress = EmiCalculator.getProgressPercent(loan.completedEmis, loan.totalEmis)
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Repayment Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${progress.toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = loanTypeColor(loan.loanType)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${loan.completedEmis} of ${loan.totalEmis} EMIs paid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (loan.status == LoanStatus.COMPLETED) {
                    Text("COMPLETED ✅", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                }
            }
        }
    }
}

@Composable
private fun LoanStatsSection(loan: LoanEntity) {
    val nextDueDate = DateUtils.nextEmiDate(loan.firstEmiDate, loan.completedEmis)
    val totalInterest = EmiCalculator.calculateTotalInterest(loan.principalAmount, loan.monthlyEmi, loan.tenureValue, loan.tenureUnit)

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Loan Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            StatRow("Principal Amount", CurrencyUtils.format(loan.principalAmount))
            StatRow("Monthly EMI", CurrencyUtils.format(loan.monthlyEmi))
            StatRow("Outstanding Balance", CurrencyUtils.format(loan.outstandingAmount))
            StatRow("Total Interest", CurrencyUtils.format(totalInterest))
            StatRow("Interest Rate", "${loan.interestRate}% (${loan.interestType.displayName})")
            StatRow("Loan Tenure", "${loan.tenureValue} ${loan.tenureUnit.displayName}")
            StatRow("Next EMI Date", DateUtils.formatDate(nextDueDate))
            StatRow("Loan End Date", DateUtils.formatDate(loan.loanEndDate))
            if (loan.loanAccountNumber.isNotBlank()) {
                StatRow("Account Number", loan.loanAccountNumber)
            }
            if (loan.notes.isNotBlank()) {
                StatRow("Notes", loan.notes)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PaymentHistoryItem(payment: PaymentHistoryEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                ) {
                    Text(
                        "EMI ${payment.emiNumber}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(DateUtils.formatDate(payment.paidDate), style = MaterialTheme.typography.bodySmall)
                    Text("Balance: ${CurrencyUtils.formatShort(payment.remainingBalance)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(CurrencyUtils.format(payment.amountPaid), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
