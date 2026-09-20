package com.loanmate.ui.loan.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanmate.utils.AmortizationCalculator
import com.loanmate.utils.CurrencyUtils
import com.loanmate.viewmodel.BackupEvent
import com.loanmate.viewmodel.BackupViewModel
import com.loanmate.viewmodel.LoanDetailsViewModel
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmortizationScheduleScreen(
    loanId: Long,
    onBack: () -> Unit,
    viewModel: LoanDetailsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(loanId) {
        viewModel.loadLoan(loanId)
    }

    LaunchedEffect(Unit) {
        backupViewModel.events.collect { event ->
            when (event) {
                is BackupEvent.SharePdf -> {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", event.file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Schedule"))
                }
                is BackupEvent.Toast -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    val loan = uiState.loan
    val schedule = remember(loan) {
        loan?.let { AmortizationCalculator.calculate(it) } ?: emptyList()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Amortization Schedule") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            loan?.let { backupViewModel.exportAmortization(context, it, schedule) }
                        },
                        enabled = loan != null
                    ) {
                        Icon(Icons.Default.PictureAsPdf, "Export")
                    }
                }
            )
        }
    ) { padding ->
        if (loan == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding)) {
                ScheduleHeader()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(schedule) { month ->
                        ScheduleRow(month = month)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HeaderText("Month", Modifier.weight(0.8f))
        HeaderText("Principal", Modifier.weight(1.2f))
        HeaderText("Interest", Modifier.weight(1.2f))
        HeaderText("Balance", Modifier.weight(1.5f), textAlign = TextAlign.End)
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
        textAlign = textAlign
    )
}

@Composable
private fun ScheduleRow(month: AmortizationCalculator.AmortizationMonth) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                month.monthNumber.toString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                CurrencyUtils.formatShort(month.principal),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                CurrencyUtils.formatShort(month.interest),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                CurrencyUtils.formatShort(month.remainingBalance),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
