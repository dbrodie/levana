package com.levana.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        observePreferences()
    }

    fun onIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.SetMinhag ->
                    preferencesRepository.saveMinhag(intent.minhag)
                is SettingsIntent.SetIsInIsrael ->
                    preferencesRepository.saveIsInIsrael(intent.inIsrael)
                is SettingsIntent.SetShowModernIsraeli ->
                    preferencesRepository.saveShowModernIsraeliHolidays(intent.show)
                is SettingsIntent.SetHebrewPrimary ->
                    preferencesRepository.saveHebrewPrimary(intent.enabled)
                is SettingsIntent.SetCandleLightingOffset ->
                    preferencesRepository.saveCandleLightingOffset(intent.offset)
                is SettingsIntent.SetDynamicHolidayTheme ->
                    preferencesRepository.saveDynamicHolidayTheme(intent.enabled)
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _state.value = SettingsState(
                    locationName = prefs.location?.name ?: "Not set",
                    minhag = prefs.minhag,
                    isInIsrael = prefs.isInIsrael,
                    showModernIsraeliHolidays = prefs.showModernIsraeliHolidays,
                    hebrewPrimary = prefs.hebrewPrimary,
                    candleLightingOffset = prefs.candleLightingOffset,
                    dynamicHolidayTheme = prefs.dynamicHolidayTheme
                )
            }
        }
    }
}
