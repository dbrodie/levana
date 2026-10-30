package ninja.doskey.app.levana.ui.settings

import ninja.doskey.app.levana.domain.model.IsraelMode
import ninja.doskey.app.levana.domain.model.ThemeMode
import ninja.doskey.app.levana.domain.model.YahrzeitFirstYear
import ninja.doskey.app.levana.domain.model.YahrzeitLeapYearAdar
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import java.time.LocalDate


sealed interface SettingsIntent {
    data class SetIsraelMode(val mode: IsraelMode) : SettingsIntent
    data class SetShowModernIsraeli(val show: Boolean) : SettingsIntent
    data class SetAppLanguage(val language: AppLanguage) : SettingsIntent
    data class SetCandleLightingOffset(val offset: Double) : SettingsIntent
    data class SetDynamicHolidayTheme(val enabled: Boolean) : SettingsIntent
    data class SetThemeMode(val mode: ThemeMode) : SettingsIntent
    data class SetMaterialYou(val enabled: Boolean) : SettingsIntent
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
    data class SetNotifyOmerTzait(val enabled: Boolean) : SettingsIntent
    data class SetNotifyOmerMorning(val enabled: Boolean) : SettingsIntent
    data class SetNotifyOmerMorningTime(val minutes: Int) : SettingsIntent
    data class SetYahrzeitLeapYearAdar(val value: YahrzeitLeapYearAdar) : SettingsIntent
    data class SetYahrzeitFirstYear(val value: YahrzeitFirstYear) : SettingsIntent
    data class SetYahrzeitAdarI30AlsoNisan1(val enabled: Boolean) : SettingsIntent
    data class SetYahrzeitMissingDay(val value: YahrzeitMissingDay) : SettingsIntent
    data class ToggleZman(val name: String, val enabled: Boolean) : SettingsIntent
}
