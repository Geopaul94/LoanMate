package com.loanmate.ui.loan.add

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanmate.data.model.InterestType
import com.loanmate.data.model.LoanType
import com.loanmate.data.model.TenureUnit
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.DateUtils
import com.loanmate.viewmodel.AddLoanViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    loanId: Long?,
    onBack: () -> Unit,
    viewModel: AddLoanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val form by viewModel.form.collectAsStateWithLifecycle()

    LaunchedEffect(loanId) {
        if (loanId != null) viewModel.loadLoan(loanId)
    }

    LaunchedEffect(form.isSaved) {
        if (form.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (loanId == null) "Add Loan" else "Edit Loan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveLoan(loanId, context) },
                        enabled = !form.isLoading
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Basic Information")

            OutlinedTextField(
                value = form.loanName,
                onValueChange = { v -> viewModel.update { copy(loanName = v) } },
                label = { Text("Loan Name *") },
                isError = form.errors.containsKey("loanName"),
                supportingText = form.errors["loanName"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.bankName,
                onValueChange = { v -> viewModel.update { copy(bankName = v) } },
                label = { Text("Bank / NBFC Name *") },
                isError = form.errors.containsKey("bankName"),
                supportingText = form.errors["bankName"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            LoanTypeDropdown(
                selected = form.loanType,
                onSelect = { v -> viewModel.update { copy(loanType = v) } }
            )

            SectionHeader("Loan Details")

            OutlinedTextField(
                value = form.principalAmount,
                onValueChange = { v ->
                    viewModel.update { copy(principalAmount = v) }
                    viewModel.recalculateEmi()
                },
                label = { Text("Principal Amount *") },
                leadingIcon = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = form.errors.containsKey("principalAmount"),
                supportingText = form.errors["principalAmount"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.interestRate,
                    onValueChange = { v ->
                        viewModel.update { copy(interestRate = v) }
                        viewModel.recalculateEmi()
                    },
                    label = { Text("Interest Rate %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                InterestTypeDropdown(
                    selected = form.interestType,
                    onSelect = { v ->
                        viewModel.update { copy(interestType = v) }
                        viewModel.recalculateEmi()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.tenureValue,
                    onValueChange = { v ->
                        viewModel.update { copy(tenureValue = v) }
                        viewModel.recalculateEmi()
                    },
                    label = { Text("Tenure *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = form.errors.containsKey("tenureValue"),
                    supportingText = form.errors["tenureValue"]?.let { { Text(it) } },
                    modifier = Modifier.weight(1f)
                )
                TenureUnitDropdown(
                    selected = form.tenureUnit,
                    onSelect = { v ->
                        viewModel.update { copy(tenureUnit = v) }
                        viewModel.recalculateEmi()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (form.calculatedEmi > 0) {
                EmiPreviewCard(emi = form.calculatedEmi)
            }

            OutlinedTextField(
                value = form.monthlyEmi,
                onValueChange = { v -> viewModel.update { copy(monthlyEmi = v) } },
                label = { Text("Monthly EMI (override)") },
                leadingIcon = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = {
                    if (form.calculatedEmi > 0) Text(CurrencyUtils.format(form.calculatedEmi))
                },
                modifier = Modifier.fillMaxWidth()
            )

            SectionHeader("Dates")

            DatePickerField(
                label = "Loan Taken Date",
                timestamp = form.loanTakenDate,
                onDateSelected = { ts ->
                    viewModel.update { copy(loanTakenDate = ts) }
                    viewModel.recalculateEmi()
                }
            )

            DatePickerField(
                label = "First EMI Date",
                timestamp = form.firstEmiDate,
                onDateSelected = { ts -> viewModel.update { copy(firstEmiDate = ts) } }
            )

            if (form.loanEndDate > 0) {
                Text(
                    text = "Loan End Date: ${DateUtils.formatDate(form.loanEndDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionHeader("Additional Details")

            OutlinedTextField(
                value = form.outstandingAmount,
                onValueChange = { v -> viewModel.update { copy(outstandingAmount = v) } },
                label = { Text("Outstanding Amount") },
                leadingIcon = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.processingFee,
                    onValueChange = { v -> viewModel.update { copy(processingFee = v) } },
                    label = { Text("Processing Fee") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.insuranceCharges,
                    onValueChange = { v -> viewModel.update { copy(insuranceCharges = v) } },
                    label = { Text("Insurance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = form.loanAccountNumber,
                onValueChange = { v -> viewModel.update { copy(loanAccountNumber = v) } },
                label = { Text("Account Number (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.notes,
                onValueChange = { v -> viewModel.update { copy(notes = v) } },
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanTypeDropdown(selected: LoanType, onSelect: (LoanType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "${selected.emoji} ${selected.displayName}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Loan Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LoanType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text("${type.emoji} ${type.displayName}") },
                    onClick = { onSelect(type); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterestTypeDropdown(
    selected: InterestType,
    onSelect: (InterestType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Interest Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            InterestType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = { onSelect(type); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TenureUnitDropdown(
    selected: TenureUnit,
    onSelect: (TenureUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TenureUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.displayName) },
                    onClick = { onSelect(unit); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun EmiPreviewCard(emi: Double) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Calculated EMI", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = CurrencyUtils.format(emi),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DatePickerField(
    label: String,
    timestamp: Long,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }

    OutlinedTextField(
        value = DateUtils.formatDate(timestamp),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val cal = Calendar.getInstance()
                        cal.set(year, month, day)
                        onDateSelected(cal.timeInMillis)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
