package com.loanmate.ui.loan.details

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.loanmate.ui.theme.SuccessGreen
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.DateUtils
import com.loanmate.utils.EmiCalculator
import com.loanmate.viewmodel.LoanDetailsViewModel

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.sp
import com.loanmate.ui.components.IconChip
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import com.loanmate.viewmodel.DocumentViewModel
import com.loanmate.data.local.DocumentEntity
import com.loanmate.data.local.DocumentType
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.ui.platform.LocalContext
import android.database.Cursor

import androidx.compose.ui.text.style.TextAlign
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailsScreen(
    loanId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCalculators: () -> Unit,
    onAmortization: (Long) -> Unit,
    onDeleted: (Long) -> Unit,
    viewModel: LoanDetailsViewModel = hiltViewModel(),
    docViewModel: DocumentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val documents by docViewModel.getDocuments(loanId).collectAsState(emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val docLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val name = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    cursor.getString(index)
                } else null
            } ?: "document"
            docViewModel.addDocument(context, loanId, it, name)
        }
    }

    LaunchedEffect(loanId) { viewModel.loadLoan(loanId) }
    LaunchedEffect(Unit) {
        viewModel.deleteEvent.collect { id -> onDeleted(id) }
    }

    val loan = uiState.loan

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loan?.loanName ?: "Loan Details", fontWeight = FontWeight.Bold) },
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
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                AnimatedVisibility(visible = uiState.showMilestone) {
                    uiState.milestoneMessage?.let { msg ->
                        MilestoneCard(message = msg, onDismiss = viewModel::dismissMilestone)
                    }
                }
            }

            item { PremiumLoanHero(loan = loan, hideValues = uiState.hideValues) }

            if (uiState.showCelebration) {
                item {
                    DebtFreeCelebration(
                        loanName = loan.loanName,
                        onDismiss = viewModel::dismissCelebration
                    )
                }
            }

            if (loan.status == LoanStatus.ACTIVE) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.markEmiPaid(loan) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Default.Payments, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Pay EMI", fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = onCalculators,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Calculate, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Strategies", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { onAmortization(loan.id) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.ListAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("View Full Amortization Schedule", fontWeight = FontWeight.Bold)
                }
            }

            item { PremiumLoanStats(loan = loan, hideValues = uiState.hideValues) }

            item {
                Text(
                    "Payment History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (uiState.payments.isNotEmpty()) {
                items(uiState.payments.reversed(), key = { it.id }) { payment ->
                    PremiumPaymentItem(payment = payment, hideValues = uiState.hideValues)
                }
            } else {
                item {
                    com.loanmate.ui.components.EmptyState(
                        emoji = "🌱",
                        title = "No payments yet",
                        message = "Your progress story starts with the first payment.",
                        compact = true
                    )
                }
            }

            item {
                Text(
                    "Documents Vault",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
                )
            }

            if (documents.isNotEmpty()) {
                items(documents, key = { it.id }) { doc ->
                    PremiumDocumentItem(
                        document = doc,
                        onOpen = {
                            try {
                                val file = File(doc.filePath)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, context.contentResolver.getType(uri))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open Document"))
                            } catch (_: Exception) {
                                // handle error
                            }
                        },
                        onDelete = { docViewModel.deleteDocument(doc) }
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = { docLauncher.launch(arrayOf("application/pdf", "image/*")) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.UploadFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Document", fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Loan") },
            text = { Text("Are you sure you want to delete this loan? This will also remove all payment history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        loan?.let { viewModel.deleteLoan(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PremiumDocumentItem(
    document: DocumentEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (document.documentType) {
                            DocumentType.PDF -> Icons.Default.Description
                            DocumentType.IMAGE -> Icons.Default.Image
                            else -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    document.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    DateUtils.formatDate(document.addedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PremiumLoanHero(loan: LoanEntity, hideValues: Boolean) {
    val progress = EmiCalculator.getProgressPercent(loan.completedEmis, loan.totalEmis) / 100f
    
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            loan.loanName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            loan.bankName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(loan.loanType.emoji, fontSize = 24.sp)
                        }
                    }
                }

                Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.15f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            "Paid",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HeroStat("Outstanding", CurrencyUtils.formatShort(loan.outstandingAmount, hideValues))
                    HeroStat("EMIs Left", "${loan.totalEmis - loan.completedEmis}")
                    HeroStat("Rate", "${loan.interestRate}%")
                }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun PremiumLoanStats(loan: LoanEntity, hideValues: Boolean) {
    val nextDueDate = DateUtils.nextEmiDate(loan.firstEmiDate, loan.completedEmis)
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Monthly EMI",
                value = CurrencyUtils.formatShort(loan.monthlyEmi, hideValues),
                icon = Icons.Default.EventRepeat,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Principal",
                value = CurrencyUtils.formatShort(loan.principalAmount, hideValues),
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Next Due",
                value = DateUtils.formatDate(nextDueDate),
                icon = Icons.Default.CalendarToday,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "End Date",
                value = DateUtils.formatDate(loan.loanEndDate),
                icon = Icons.Default.EventAvailable,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DebtFreeCelebration(loanName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Share Success")
            }
        },
        title = {
            Text(
                "Congratulations! 🎉",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "You have successfully paid off your \"$loanName\" loan. You are one step closer to complete financial freedom!",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("DEBT FREE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text(loanName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Text("Successfully Closed", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        shape = RoundedCornerShape(32.dp)
    )

    KonfettiView(
        modifier = Modifier.fillMaxSize(),
        parties = listOf(
            Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xbdaead),
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                position = nl.dionsegijn.konfetti.core.Position.Relative(0.5, 0.3)
            )
        )
    )
}

@Composable
private fun PremiumPaymentItem(payment: PaymentHistoryEntity, hideValues: Boolean) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = SuccessGreen.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "EMI #${payment.emiNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    DateUtils.formatDate(payment.paidDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    CurrencyUtils.format(payment.amountPaid, hideValues),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Balance: ${CurrencyUtils.formatShort(payment.remainingBalance, hideValues)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
