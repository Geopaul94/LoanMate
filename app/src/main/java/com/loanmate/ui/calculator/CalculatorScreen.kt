package com.loanmate.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanmate.data.local.LoanEntity
import com.loanmate.viewmodel.CalculatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    loanId: Long,
    onBack: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val loan by viewModel.loan.collectAsStateWithLifecycle()

    LaunchedEffect(loanId) { viewModel.loadLoan(loanId) }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Prepayment", "Foreclosure")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculators") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            val currentLoan = loan
            if (currentLoan == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    LoanContextCard(currentLoan)
                    Spacer(Modifier.height(16.dp))
                    when (selectedTab) {
                        0 -> PrepaymentTab(currentLoan)
                        1 -> ForeclosureTab(currentLoan)
                    }
                }
            }
        }
    }
}

internal fun sanitizeDecimal(input: String): String? {
    val filtered = input.filter { it.isDigit() || it == '.' }
    if (filtered.count { it == '.' } > 1) return null
    return filtered
}

@Composable
internal fun MoneyField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> sanitizeDecimal(input)?.let(onChange) },
        label = { Text(label) },
        leadingIcon = { Text("₹") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun PercentField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> sanitizeDecimal(input)?.let(onChange) },
        label = { Text(label) },
        trailingIcon = { Text("%") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun ResultRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isHighlight) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodyMedium,
            color = if (isHighlight) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isHighlight) androidx.compose.ui.text.font.FontWeight.Bold
                         else androidx.compose.ui.text.font.FontWeight.Normal
        )
    }
}

@Composable
private fun LoanContextCard(loan: LoanEntity) {
    val remainingMonths = (loan.totalEmis - loan.completedEmis).coerceAtLeast(0)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(loan.loanName, style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(loan.bankName, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            ResultRow("Outstanding", com.loanmate.utils.CurrencyUtils.format(loan.outstandingAmount))
            ResultRow("Monthly EMI", com.loanmate.utils.CurrencyUtils.format(loan.monthlyEmi))
            ResultRow("Interest rate", "${loan.interestRate}% p.a.")
            ResultRow("Remaining EMIs", remainingMonths.toString())
        }
    }
}
