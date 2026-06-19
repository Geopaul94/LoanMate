package com.loanmate.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val hasSeenOnboarding: StateFlow<Boolean?> = dataStore.data
        .map { it[KEY_SEEN] ?: false }
        // null = still loading from disk → keeps splash on initial frame
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun complete() {
        viewModelScope.launch {
            dataStore.edit { it[KEY_SEEN] = true }
        }
    }

    companion object {
        private val KEY_SEEN = booleanPreferencesKey("has_seen_onboarding")
    }
}
