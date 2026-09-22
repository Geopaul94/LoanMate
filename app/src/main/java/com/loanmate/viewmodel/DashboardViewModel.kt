package com.loanmate.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.repository.LoanRepository
import com.loanmate.data.repository.PaymentRepository
import com.loanmate.utils.EmiCalculator
import com.loanmate.utils.PrepaymentCalculator
import com.loanmate.utils.StreakCalculator
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
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val prepaymentInsight: PrepaymentInsight? = null,
    val hideValues: Boolean = false,
    val isLoading: Boolean = true
)

data class PrepaymentInsight(
    val loanName: String,
    val extraAmount: Double,
    val monthsSaved: Int,
    val interestSaved: Double
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    paymentRepository: PaymentRepository,
    dataStore: DataStore<Preferences>
) : ViewModel() {

    private val KEY_HIDE_VALUES = booleanPreferencesKey("hide_values")
    private val hideValuesFlow = dataStore.data.map { it[KEY_HIDE_VALUES] ?: false }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _extraAmount = MutableStateFlow<Double?>(null)

    private val summaryFlow = combine(
        loanRepository.getActiveLoanCount(),
        loanRepository.getTotalOutstanding(),
        loanRepository.getTotalMonthlyEmi(),
        paymentRepository.getAllPaymentsNewestFirst()
    ) { count, outstanding, monthly, payments ->
        val streak = StreakCalculator.calculate(payments)
        SummaryBundle(count, outstanding ?: 0.0, monthly ?: 0.0, streak.current, streak.longest)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        loanRepository.getAllLoans(),
        summaryFlow,
        _searchQuery,
        _extraAmount,
        hideValuesFlow
    ) { loans, summary, query, manualExtra, hide ->
        val filtered = if (query.isBlank()) loans
        else loans.filter {
            it.loanName.contains(query, ignoreCase = true) ||
                    it.bankName.contains(query, ignoreCase = true)
        }
        val debtFreeDate = loans
            .filter { it.status == LoanStatus.ACTIVE }
            .maxOfOrNull { EmiCalculator.projectLoanEndDate(it.firstEmiDate, it.completedEmis, it.totalEmis) }

        val insight = loans.filter { it.status == LoanStatus.ACTIVE }
            .maxByOrNull { it.interestRate }
            ?.let { loan ->
                val extra = manualExtra ?: (loan.monthlyEmi * 0.2).coerceAtLeast(1000.0)
                val result = PrepaymentCalculator.calculate(
                    outstanding = loan.outstandingAmount,
                    annualRatePercent = loan.interestRate,
                    currentEmi = loan.monthlyEmi,
                    remainingMonths = loan.totalEmis - loan.completedEmis,
                    prepaymentAmount = extra.coerceAtMost(loan.outstandingAmount),
                    mode = PrepaymentCalculator.Mode.REDUCE_TENURE
                )
                if (result.monthsSaved > 0) {
                    PrepaymentInsight(
                        loanName = loan.loanName,
                        extraAmount = extra,
                        monthsSaved = result.monthsSaved,
                        interestSaved = result.interestSaved
                    )
                } else null
            }

        DashboardUiState(
            loans = filtered,
            activeLoanCount = summary.activeCount,
            totalOutstanding = summary.outstanding,
            totalMonthlyEmi = summary.monthlyEmi,
            completedLoansCount = loans.count { it.status == LoanStatus.COMPLETED },
            searchQuery = query,
            debtFreeDate = debtFreeDate,
            currentStreak = summary.currentStreak,
            longestStreak = summary.longestStreak,
            prepaymentInsight = insight,
            hideValues = hide,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onExtraAmountChange(amount: Double) {
        _extraAmount.value = amount
    }

    fun restoreLoan(loanId: Long) {
        viewModelScope.launch { loanRepository.restoreLoan(loanId) }
    }

    private data class SummaryBundle(
        val activeCount: Int,
        val outstanding: Double,
        val monthlyEmi: Double,
        val currentStreak: Int,
        val longestStreak: Int
    )
}
