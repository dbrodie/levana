package ninja.doskey.app.levana.ui.settings

import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.YahrzeitRules
import java.time.LocalDate


data class SettingsState(
    val locationName: String = "",
    val currentLocationName: String = "",
    val savedLocations: List<SavedLocation> = emptyList(),
    val activeLocationId: String? = null,
    val isInIsrael: Boolean = false,
    val showModernIsraeliHolidays: Boolean = true,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val candleLightingOffset: Double = 18.0,
    val dynamicHolidayTheme: Boolean = true,
    val showDeveloperSettings: Boolean = false,
    val devDateOverride: LocalDate? = null,
    val devForceHolidayTheme: String? = null,
    val notifyCandleLighting: Boolean = false,
    val candleLightingNotifyMode: String = "morning",
    val candleLightingMorningTime: Int = 480,
    val candleLightingHoursBefore: Int = 2,
    val notifyHolidays: Boolean = false,
    val holidayNotifyDaysBefore: Int = 1,
    val notifyFasts: Boolean = false,
    val notifyPersonalEvents: Boolean = false,
    val notifyOmer: Boolean = false,
    val notifyOmerTzait: Boolean = true,
    val notifyOmerMorning: Boolean = false,
    val notifyOmerMorningTime: Int = 420,
    val selectedZmanim: Set<String> = setOf("Sunrise", "Sunset", "Nightfall"),
    val yahrzeitRules: YahrzeitRules = YahrzeitRules()
)
