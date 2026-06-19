package com.loanmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.repository.LoanRepository
import com.loanmate.utils.EmiCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val loans: List<LoanEntity> = emptyList(),
    val activeLoanCount: Int = 0,
    val totalOutstanding: Double = 0.0,
    val totalMonthlyEmi: Double = 0.0,
    val completedLoansCount: Int = 0,
    val searchQuery: String = "",
    val debtFreeDate: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val summaryFlow = combine(
        loanRepository.getActiveLoanCount(),
        loanRepository.getTotalOutstanding(),
        loanRepository.getTotalMonthlyEmi()
    ) { count, outstanding, monthly ->
        Triple(count, outstanding ?: 0.0, monthly ?: 0.0)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        loanRepository.getAllLoans(),
        summaryFlow,
        _searchQuery
    ) { loans, (activeCount, outstanding, monthlyEmi), query ->
        val filtered = if (query.isBlank()) loans
        else loans.filter {
            it.loanName.contains(query, ignoreCase = true) ||
                    it.bankName.contains(query, ignoreCase = true)
        }
        val debtFreeDate = loans
            .filter { it.status == LoanStatus.ACTIVE }
            .maxOfOrNull { EmiCalculator.projectLoanEndDate(it.firstEmiDate, it.completedEmis, it.totalEmis) }
        DashboardUiState(
            loans = filtered,
            activeLoanCount = activeCount,
            totalOutstanding = outstanding,
            totalMonthlyEmi = monthlyEmi,
            completedLoansCount = loans.count { it.status == LoanStatus.COMPLETED },
            searchQuery = query,
            debtFreeDate = debtFreeDate,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun restoreLoan(loanId: Long) {
        viewModelScope.launch { loanRepository.restoreLoan(loanId) }
    }
}
