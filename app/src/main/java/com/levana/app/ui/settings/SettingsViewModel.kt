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
                is SettingsIntent.SetDevDateOverride ->
                    preferencesRepository.saveDevDateOverride(intent.date)
                is SettingsIntent.SetDevForceHolidayTheme ->
                    preferencesRepository.saveDevForceHolidayTheme(intent.theme)
                is SettingsIntent.SetNotifyCandleLighting ->
                    preferencesRepository.saveNotifyCandleLighting(intent.enabled)
                is SettingsIntent.SetCandleLightingNotifyMode ->
                    preferencesRepository.saveCandleLightingNotifyMode(intent.mode)
                is SettingsIntent.SetCandleLightingMorningTime ->
                    preferencesRepository.saveCandleLightingMorningTime(intent.minutes)
                is SettingsIntent.SetCandleLightingHoursBefore ->
                    preferencesRepository.saveCandleLightingHoursBefore(intent.hours)
                is SettingsIntent.SetNotifyHolidays ->
                    preferencesRepository.saveNotifyHolidays(intent.enabled)
                is SettingsIntent.SetHolidayNotifyDaysBefore ->
                    preferencesRepository.saveHolidayNotifyDaysBefore(intent.days)
                is SettingsIntent.SetNotifyFasts ->
                    preferencesRepository.saveNotifyFasts(intent.enabled)
                is SettingsIntent.SetNotifyPersonalEvents ->
                    preferencesRepository.saveNotifyPersonalEvents(intent.enabled)
                is SettingsIntent.SetNotifyOmer ->
                    preferencesRepository.saveNotifyOmer(intent.enabled)
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
                    dynamicHolidayTheme = prefs.dynamicHolidayTheme,
                    showDeveloperSettings = prefs.showDeveloperSettings,
                    devDateOverride = prefs.devDateOverride,
                    devForceHolidayTheme = prefs.devForceHolidayTheme,
                    notifyCandleLighting = prefs.notifyCandleLighting,
                    candleLightingNotifyMode = prefs.candleLightingNotifyMode,
                    candleLightingMorningTime = prefs.candleLightingMorningTime,
                    candleLightingHoursBefore = prefs.candleLightingHoursBefore,
                    notifyHolidays = prefs.notifyHolidays,
                    holidayNotifyDaysBefore = prefs.holidayNotifyDaysBefore,
                    notifyFasts = prefs.notifyFasts,
                    notifyPersonalEvents = prefs.notifyPersonalEvents,
                    notifyOmer = prefs.notifyOmer
                )
            }
        }
    }
}
