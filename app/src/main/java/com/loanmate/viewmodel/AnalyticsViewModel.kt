package com.loanmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.model.LoanType
import com.loanmate.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AnalyticsUiState(
    val loans: List<LoanEntity> = emptyList(),
    val totalPrincipal: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalMonthlyEmi: Double = 0.0,
    val loansByType: Map<LoanType, Int> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = loanRepository.getAllLoans()
        .map { loans ->
            val totalPrincipal = loans.sumOf { it.principalAmount }
            val totalOutstanding = loans.sumOf { it.outstandingAmount }
            val totalMonthlyEmi = loans.filter { it.status == LoanStatus.ACTIVE }.sumOf { it.monthlyEmi }
            val loansByType = loans.groupBy { it.loanType }.mapValues { it.value.size }
            AnalyticsUiState(
                loans = loans,
                totalPrincipal = totalPrincipal,
                totalOutstanding = totalOutstanding,
                totalPaid = totalPrincipal - totalOutstanding,
                totalMonthlyEmi = totalMonthlyEmi,
                loansByType = loansByType,
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())
}
