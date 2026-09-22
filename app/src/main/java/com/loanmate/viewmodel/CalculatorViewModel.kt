package com.loanmate.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class CalculatorUiState(
    val loan: LoanEntity? = null,
    val hideValues: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _loanId = MutableStateFlow<Long?>(null)
    private val KEY_HIDE_VALUES = booleanPreferencesKey("hide_values")
    private val hideValuesFlow = dataStore.data.map { it[KEY_HIDE_VALUES] ?: false }

    val uiState: StateFlow<CalculatorUiState> = _loanId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(loanRepository.getLoanById(id), hideValuesFlow) { loan, hide ->
                CalculatorUiState(loan, hide)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalculatorUiState())

    fun loadLoan(loanId: Long) {
        _loanId.value = loanId
    }
}
