package com.levana.app.ui.settings

import com.levana.app.domain.model.Minhag
import java.time.LocalDate

sealed interface SettingsIntent {
    data class SetMinhag(val minhag: Minhag) : SettingsIntent
    data class SetIsInIsrael(val inIsrael: Boolean) : SettingsIntent
    data class SetShowModernIsraeli(val show: Boolean) : SettingsIntent
    data class SetHebrewPrimary(val enabled: Boolean) : SettingsIntent
    data class SetCandleLightingOffset(val offset: Double) : SettingsIntent
    data class SetDynamicHolidayTheme(val enabled: Boolean) : SettingsIntent
    data class SetDevDateOverride(val date: LocalDate?) : SettingsIntent
    data class SetDevForceHolidayTheme(val theme: String?) : SettingsIntent
    data class SetNotifyCandleLighting(val enabled: Boolean) : SettingsIntent
    data class SetCandleLightingNotifyMode(val mode: String) : SettingsIntent
    data class SetCandleLightingMorningTime(val minutes: Int) : SettingsIntent
    data class SetCandleLightingHoursBefore(val hours: Int) : SettingsIntent
    data class SetNotifyHolidays(val enabled: Boolean) : SettingsIntent
    data class SetHolidayNotifyDaysBefore(val days: Int) : SettingsIntent
    data class SetNotifyFasts(val enabled: Boolean) : SettingsIntent
    data class SetNotifyPersonalEvents(val enabled: Boolean) : SettingsIntent
    data class SetNotifyOmer(val enabled: Boolean) : SettingsIntent
}
