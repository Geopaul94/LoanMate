package com.loanmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.local.PaymentHistoryEntity
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.repository.LoanRepository
import com.loanmate.data.repository.PaymentRepository
import com.loanmate.utils.DateUtils
import com.loanmate.utils.EmiCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoanDetailsUiState(
    val loan: LoanEntity? = null,
    val payments: List<PaymentHistoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val milestoneMessage: String? = null,
    val showMilestone: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LoanDetailsViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _loanId = MutableStateFlow<Long?>(null)
    private val _milestone = MutableStateFlow<String?>(null)
    private val _showMilestone = MutableStateFlow(false)

    // One-shot event: emit deleted loanId so Dashboard can show Undo snackbar
    private val _deleteEvent = MutableSharedFlow<Long>()
    val deleteEvent: SharedFlow<Long> = _deleteEvent.asSharedFlow()

    val uiState: StateFlow<LoanDetailsUiState> = _loanId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                loanRepository.getLoanById(id),
                paymentRepository.getPaymentsByLoanId(id),
                _milestone,
                _showMilestone
            ) { loan, payments, milestone, showMilestone ->
                LoanDetailsUiState(
                    loan = loan,
                    payments = payments,
                    isLoading = false,
                    milestoneMessage = milestone,
                    showMilestone = showMilestone
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoanDetailsUiState())

    fun loadLoan(loanId: Long) {
        _loanId.value = loanId
    }

    fun markEmiPaid(loan: LoanEntity) {
        viewModelScope.launch {
            val nextDueDate = DateUtils.nextEmiDate(loan.firstEmiDate, loan.completedEmis)
            val principalPerEmi = loan.principalAmount / loan.totalEmis
            val interestPerEmi = loan.monthlyEmi - principalPerEmi
            val newCompleted = loan.completedEmis + 1
            val newOutstanding = (loan.outstandingAmount - principalPerEmi).coerceAtLeast(0.0)
            val isCompleted = newCompleted >= loan.totalEmis

            val payment = PaymentHistoryEntity(
                loanId = loan.id,
                emiNumber = newCompleted,
                amountPaid = loan.monthlyEmi,
                principalComponent = principalPerEmi,
                interestComponent = interestPerEmi,
                remainingBalance = newOutstanding,
                paidDate = System.currentTimeMillis(),
                dueDate = nextDueDate
            )
            paymentRepository.insertPayment(payment)

            loanRepository.updateLoan(
                loan.copy(
                    completedEmis = newCompleted,
                    outstandingAmount = newOutstanding,
                    status = if (isCompleted) LoanStatus.COMPLETED else LoanStatus.ACTIVE
                )
            )

            val progress = EmiCalculator.getProgressPercent(newCompleted, loan.totalEmis)
            val message = EmiCalculator.getMilestoneMessage(progress)
            if (message != null) {
                _milestone.value = message
                _showMilestone.value = true
            }
        }
    }

    fun dismissMilestone() {
        _showMilestone.value = false
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            loanRepository.softDeleteLoan(loan.id)
            _deleteEvent.emit(loan.id)
        }
    }
}
