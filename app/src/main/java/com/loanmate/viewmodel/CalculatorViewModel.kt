package com.loanmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _loanId = MutableStateFlow<Long?>(null)

    val loan: StateFlow<LoanEntity?> = _loanId
        .filterNotNull()
        .flatMapLatest { id -> loanRepository.getLoanById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadLoan(loanId: Long) {
        _loanId.value = loanId
    }
}
