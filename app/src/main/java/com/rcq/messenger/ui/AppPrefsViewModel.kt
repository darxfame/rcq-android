package com.rcq.messenger.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.di.PreferencesKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppPrefsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val retroMode: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.RETRO_MODE] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val darkTheme: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.DARK_THEME] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val amoledTheme: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.AMOLED_THEME] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val highContrast: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.HIGH_CONTRAST] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setRetroMode(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.RETRO_MODE] = enabled } }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.DARK_THEME] = enabled } }
    }

    fun setAmoledTheme(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.AMOLED_THEME] = enabled }
        }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.HIGH_CONTRAST] = enabled }
        }
    }
}
