package com.loanmate.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    val hideValues: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    dataStore: DataStore<Preferences>
) : ViewModel() {

    private val KEY_HIDE_VALUES = booleanPreferencesKey("hide_values")
    private val hideValuesFlow = dataStore.data.map { it[KEY_HIDE_VALUES] ?: false }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        loanRepository.getAllLoans(),
        hideValuesFlow
    ) { loans, hide ->
        val totalPrincipal = loans.sumOf { it.principalAmount }
        val totalOutstanding = loans.sumOf { it.outstandingAmount }
        val totalMonthlyEmi = loans.filter { it.status == LoanStatus.ACTIVE }.sumOf { it.monthlyEmi }
        val loansByType = loans.groupBy { it.loanType }.mapValues { it.value.size }
        AnalyticsUiState(
            loans = loans,
            totalPrincipal = totalPrincipal,
            totalOutstanding = totalOutstanding,
            totalPaid = (totalPrincipal - totalOutstanding).coerceAtLeast(0.0),
            totalMonthlyEmi = totalMonthlyEmi,
            loansByType = loansByType,
            hideValues = hide,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())
}
