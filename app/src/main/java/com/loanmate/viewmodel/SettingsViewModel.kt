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

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val hideValues: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
    private val KEY_HIDE_VALUES = booleanPreferencesKey("hide_values")

    val uiState: StateFlow<SettingsUiState> = dataStore.data.map { prefs ->
        SettingsUiState(
            isDarkMode = prefs[KEY_DARK_MODE] ?: false,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
            biometricEnabled = prefs[KEY_BIOMETRIC] ?: false,
            hideValues = prefs[KEY_HIDE_VALUES] ?: false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setDarkMode(value: Boolean) = set(KEY_DARK_MODE, value)
    fun setNotifications(value: Boolean) = set(KEY_NOTIFICATIONS, value)
    fun setBiometric(value: Boolean) = set(KEY_BIOMETRIC, value)
    fun setHideValues(value: Boolean) = set(KEY_HIDE_VALUES, value)

    private fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[key] = value } }
    }
}
