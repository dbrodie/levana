package com.levana.app.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val currentLocales = localeManager.applicationLocales
        val initialLanguage = if (!currentLocales.isEmpty &&
            currentLocales[0]?.language == "iw"
        ) {
            AppLanguage.HEBREW
        } else {
            AppLanguage.SYSTEM
        }
        _state.value = _state.value.copy(appLanguage = initialLanguage)
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
                is SettingsIntent.SetAppLanguage -> {
                    val localeManager = context.getSystemService(LocaleManager::class.java)
                    if (intent.language == AppLanguage.HEBREW) {
                        localeManager.applicationLocales =
                            LocaleList.forLanguageTags("iw")
                    } else {
                        localeManager.applicationLocales =
                            LocaleList.getEmptyLocaleList()
                    }
                    _state.value = _state.value.copy(appLanguage = intent.language)
                }
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
                _state.value = _state.value.copy(
                    locationName = prefs.location?.name ?: "Not set",
                    minhag = prefs.minhag,
                    isInIsrael = prefs.isInIsrael,
                    showModernIsraeliHolidays = prefs.showModernIsraeliHolidays,
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
