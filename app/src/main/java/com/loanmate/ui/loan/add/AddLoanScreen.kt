package com.loanmate.ui.loan.add

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch
import java.util.*

import androidx.compose.animation.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    loanId: Long?,
    onBack: () -> Unit,
    viewModel: AddLoanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 3

    LaunchedEffect(loanId) {
        if (loanId != null) viewModel.loadLoan(loanId)
    }

    LaunchedEffect(form.isSaved) {
        if (form.isSaved) onBack()
    }

    // Auto-navigate to first error step if save fails
    LaunchedEffect(form.errors) {
        if (form.errors.isNotEmpty()) {
            val errorKeys = form.errors.keys
            val targetStep = when {
                errorKeys.any { it in listOf("loanName", "bankName") } -> 1
                errorKeys.any { it in listOf("principalAmount", "tenureValue") } -> 2
                else -> currentStep
            }
            if (targetStep != currentStep) {
                currentStep = targetStep
            }
            scope.launch {
                snackbarHostState.showSnackbar("Please correct the errors before saving")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (loanId == null) "New Loan" else "Edit Loan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Step $currentStep of $totalSteps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) currentStep-- else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Previous")
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                if (viewModel.validateStep(currentStep)) {
                                    currentStep++
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please fill all required fields correctly")
                                    }
                                }
                            } else {
                                viewModel.saveLoan(loanId, context)
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !form.isLoading
                    ) {
                        Text(if (currentStep == totalSteps) "Save Loan" else "Continue", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            StepProgressIndicator(currentStep = currentStep, totalSteps = totalSteps)
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    },
                    label = "StepContent"
                ) { step ->
                    when (step) {
                        1 -> BasicDetailsStep(form = form, viewModel = viewModel)
                        2 -> FinancialsStep(form = form, viewModel = viewModel)
                        3 -> AdditionalDetailsStep(form = form, viewModel = viewModel)
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun StepProgressIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { index ->
            val step = index + 1
            val isActive = step <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun BasicDetailsStep(form: com.loanmate.viewmodel.AddLoanFormState, viewModel: AddLoanViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        StepHeader("Loan Basics", "What should we call this loan and who is the lender?")
        
        OutlinedTextField(
            value = form.loanName,
            onValueChange = { v -> viewModel.update { copy(loanName = v) } },
            label = { Text("Loan Name (e.g. Home Loan)") },
            placeholder = { Text("Enter a friendly name") },
            isError = form.errors.containsKey("loanName"),
            supportingText = form.errors["loanName"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        OutlinedTextField(
            value = form.bankName,
            onValueChange = { v -> viewModel.update { copy(bankName = v) } },
            label = { Text("Bank / NBFC Name") },
            placeholder = { Text("Who gave you this loan?") },
            isError = form.errors.containsKey("bankName"),
            supportingText = form.errors["bankName"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        LoanTypeDropdown(
            selected = form.loanType,
            onSelect = { v -> viewModel.update { copy(loanType = v) } }
        )
    }
}

@Composable
private fun FinancialsStep(form: com.loanmate.viewmodel.AddLoanFormState, viewModel: AddLoanViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        StepHeader("Financial Details", "How much did you borrow and at what rate?")

        OutlinedTextField(
            value = form.principalAmount,
            onValueChange = { v ->
                viewModel.update { copy(principalAmount = v) }
                viewModel.recalculateEmi()
            },
            label = { Text("Principal Amount") },
            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = form.errors.containsKey("principalAmount"),
            supportingText = form.errors["principalAmount"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = form.interestRate,
                onValueChange = { v ->
                    viewModel.update { copy(interestRate = v) }
                    viewModel.recalculateEmi()
                },
                label = { Text("Rate %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(16.dp)
            )
            InterestTypeDropdown(
                selected = form.interestType,
                onSelect = { v ->
                    viewModel.update { copy(interestType = v) }
                    viewModel.recalculateEmi()
                },
                modifier = Modifier.weight(1.2f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = form.tenureValue,
                onValueChange = { v ->
                    viewModel.update { copy(tenureValue = v) }
                    viewModel.recalculateEmi()
                },
                label = { Text("Tenure") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = form.errors.containsKey("tenureValue"),
                supportingText = form.errors["tenureValue"]?.let { { Text(it) } },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
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
            PremiumEmiPreview(emi = form.calculatedEmi)
        }

        OutlinedTextField(
            value = form.monthlyEmi,
            onValueChange = { v -> viewModel.update { copy(monthlyEmi = v) } },
            label = { Text("Actual Monthly EMI") },
            placeholder = { if (form.calculatedEmi > 0) Text(CurrencyUtils.format(form.calculatedEmi)) },
            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            supportingText = { Text("Optional: Override if your bank EMI differs from calculation.") }
        )
    }
}

@Composable
private fun AdditionalDetailsStep(form: com.loanmate.viewmodel.AddLoanFormState, viewModel: AddLoanViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        StepHeader("Final Details", "When did this start and any other details?")

        DatePickerField(
            label = "Loan Start Date",
            timestamp = form.loanTakenDate,
            onDateSelected = { ts ->
                viewModel.update { copy(loanTakenDate = ts) }
                viewModel.recalculateEmi()
            }
        )

        DatePickerField(
            label = "First EMI Due Date",
            timestamp = form.firstEmiDate,
            onDateSelected = { ts -> viewModel.update { copy(firstEmiDate = ts) } }
        )

        OutlinedTextField(
            value = form.loanAccountNumber,
            onValueChange = { v -> viewModel.update { copy(loanAccountNumber = v) } },
            label = { Text("Account Number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        OutlinedTextField(
            value = form.notes,
            onValueChange = { v -> viewModel.update { copy(notes = v) } },
            label = { Text("Notes / Remarks") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PremiumEmiPreview(emi: Double) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Estimated EMI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(CurrencyUtils.format(emi), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Calculate, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
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
