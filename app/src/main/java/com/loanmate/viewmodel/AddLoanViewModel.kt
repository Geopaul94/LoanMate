package com.loanmate.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.InterestType
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.model.LoanType
import com.loanmate.data.model.TenureUnit
import com.loanmate.data.repository.LoanRepository
import com.loanmate.utils.EmiCalculator
import com.loanmate.worker.EmiReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddLoanFormState(
    val loanName: String = "",
    val bankName: String = "",
    val loanType: LoanType = LoanType.PERSONAL,
    val principalAmount: String = "",
    val interestRate: String = "",
    val interestType: InterestType = InterestType.REDUCING_BALANCE,
    val tenureValue: String = "",
    val tenureUnit: TenureUnit = TenureUnit.MONTHS,
    val monthlyEmi: String = "",
    val firstEmiDate: Long = System.currentTimeMillis(),
    val loanTakenDate: Long = System.currentTimeMillis(),
    val processingFee: String = "",
    val insuranceCharges: String = "",
    val outstandingAmount: String = "",
    val loanAccountNumber: String = "",
    val notes: String = "",
    val calculatedEmi: Double = 0.0,
    val loanEndDate: Long = 0L,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errors: Map<String, String> = emptyMap()
)

@HiltViewModel
class AddLoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _form = MutableStateFlow(AddLoanFormState())
    val form = _form.asStateFlow()

    fun loadLoan(loanId: Long) {
        viewModelScope.launch {
            val loan = loanRepository.getLoanById(loanId).firstOrNull() ?: return@launch
            _form.update {
                it.copy(
                    loanName = loan.loanName,
                    bankName = loan.bankName,
                    loanType = loan.loanType,
                    principalAmount = loan.principalAmount.toString(),
                    interestRate = loan.interestRate.toString(),
                    interestType = loan.interestType,
                    tenureValue = loan.tenureValue.toString(),
                    tenureUnit = loan.tenureUnit,
                    monthlyEmi = loan.monthlyEmi.toString(),
                    firstEmiDate = loan.firstEmiDate,
                    loanTakenDate = loan.loanTakenDate,
                    processingFee = loan.processingFee.toString(),
                    insuranceCharges = loan.insuranceCharges.toString(),
                    outstandingAmount = loan.outstandingAmount.toString(),
                    loanAccountNumber = loan.loanAccountNumber,
                    notes = loan.notes
                )
            }
        }
    }

    fun update(block: AddLoanFormState.() -> AddLoanFormState) = _form.update { it.block() }

    fun recalculateEmi() {
        val f = _form.value
        val principal = f.principalAmount.toDoubleOrNull() ?: return
        val rate = f.interestRate.toDoubleOrNull() ?: return
        val tenure = f.tenureValue.toIntOrNull() ?: return
        val emi = EmiCalculator.calculateEmi(principal, rate, tenure, f.tenureUnit, f.interestType)
        val endDate = EmiCalculator.calculateLoanEndDate(f.loanTakenDate, tenure, f.tenureUnit)
        _form.update { it.copy(calculatedEmi = emi, loanEndDate = endDate) }
    }

    fun saveLoan(editingLoanId: Long?, context: Context) {
        val f = _form.value
        val errors = validate(f)
        if (errors.isNotEmpty()) {
            _form.update { it.copy(errors = errors) }
            return
        }

        val principal = f.principalAmount.toDouble()
        val rate = f.interestRate.toDoubleOrNull() ?: 0.0
        val tenure = f.tenureValue.toInt()
        val emi = if (f.monthlyEmi.isNotBlank()) f.monthlyEmi.toDouble()
        else f.calculatedEmi
        val totalEmis = EmiCalculator.getTotalMonths(tenure, f.tenureUnit)
        val endDate = EmiCalculator.calculateLoanEndDate(f.loanTakenDate, tenure, f.tenureUnit)

        viewModelScope.launch {
            _form.update { it.copy(isLoading = true) }
            val existing = editingLoanId?.let { loanRepository.getLoanById(it).firstOrNull() }
            val loan = LoanEntity(
                id = existing?.id ?: 0L,
                loanName = f.loanName.trim(),
                bankName = f.bankName.trim(),
                loanType = f.loanType,
                principalAmount = principal,
                interestRate = rate,
                interestType = f.interestType,
                tenureValue = tenure,
                tenureUnit = f.tenureUnit,
                monthlyEmi = emi,
                firstEmiDate = f.firstEmiDate,
                loanTakenDate = f.loanTakenDate,
                loanEndDate = endDate,
                processingFee = f.processingFee.toDoubleOrNull() ?: 0.0,
                insuranceCharges = f.insuranceCharges.toDoubleOrNull() ?: 0.0,
                outstandingAmount = f.outstandingAmount.toDoubleOrNull() ?: principal,
                loanAccountNumber = f.loanAccountNumber.trim(),
                notes = f.notes.trim(),
                completedEmis = existing?.completedEmis ?: 0,
                totalEmis = totalEmis,
                status = existing?.status ?: LoanStatus.ACTIVE,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            val id = if (editingLoanId == null) loanRepository.insertLoan(loan)
            else { loanRepository.updateLoan(loan); editingLoanId }

            EmiReminderWorker.cancelReminders(context, id)
            EmiReminderWorker.scheduleReminders(context, id, f.firstEmiDate)

            _form.update { it.copy(isLoading = false, isSaved = true) }
        }
    }

    private fun validate(f: AddLoanFormState): Map<String, String> = buildMap {
        if (f.loanName.isBlank()) put("loanName", "Loan name is required")
        if (f.bankName.isBlank()) put("bankName", "Bank name is required")
        if (f.principalAmount.toDoubleOrNull() == null) put("principalAmount", "Enter a valid amount")
        if (f.tenureValue.toIntOrNull() == null) put("tenureValue", "Enter a valid tenure")
    }
}
