package com.loanmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class PayoffUiState(
    val activeLoans: List<LoanEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PayoffStrategyViewModel @Inject constructor(
    loanRepository: LoanRepository
) : ViewModel() {

    val uiState: StateFlow<PayoffUiState> = loanRepository.getAllLoans()
        .map { loans ->
            PayoffUiState(
                activeLoans = loans.filter { it.status == LoanStatus.ACTIVE && it.outstandingAmount > 0 },
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PayoffUiState())
}
