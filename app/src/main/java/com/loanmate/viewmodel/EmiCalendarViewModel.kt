package com.loanmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.repository.LoanRepository
import com.loanmate.utils.EmiOccurrenceGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class EmiCalendarUiState(
    val occurrencesByDay: Map<DayKey, List<EmiOccurrenceGenerator.Occurrence>> = emptyMap(),
    val isLoading: Boolean = true
)

data class DayKey(val year: Int, val month: Int, val day: Int)

@HiltViewModel
class EmiCalendarViewModel @Inject constructor(
    loanRepository: LoanRepository
) : ViewModel() {

    val uiState: StateFlow<EmiCalendarUiState> = loanRepository.getAllLoans()
        .map { loans ->
            val occurrences = EmiOccurrenceGenerator.forActiveLoans(loans)
            EmiCalendarUiState(
                occurrencesByDay = occurrences.groupBy { DayKey(it.year, it.month, it.day) },
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EmiCalendarUiState())
}
