package com.loanmate.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.repository.LoanRepository
import com.loanmate.utils.EmiOccurrenceGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class EmiCalendarUiState(
    val occurrencesByDay: Map<DayKey, List<EmiOccurrenceGenerator.Occurrence>> = emptyMap(),
    val hideValues: Boolean = false,
    val isLoading: Boolean = true
)

data class DayKey(val year: Int, val month: Int, val day: Int)

@HiltViewModel
class EmiCalendarViewModel @Inject constructor(
    loanRepository: LoanRepository,
    dataStore: DataStore<Preferences>
) : ViewModel() {

    private val KEY_HIDE_VALUES = booleanPreferencesKey("hide_values")
    private val hideValuesFlow = dataStore.data.map { it[KEY_HIDE_VALUES] ?: false }

    val uiState: StateFlow<EmiCalendarUiState> = combine(
        loanRepository.getAllLoans(),
        hideValuesFlow
    ) { loans, hide ->
        val occurrences = EmiOccurrenceGenerator.forActiveLoans(loans)
        EmiCalendarUiState(
            occurrencesByDay = occurrences.groupBy { DayKey(it.year, it.month, it.day) },
            hideValues = hide,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EmiCalendarUiState())
}
