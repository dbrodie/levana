package com.levana.app.ui.settings

import com.levana.app.domain.model.Minhag
import com.levana.app.domain.model.SavedLocation
import java.time.LocalDate


data class SettingsState(
    val locationName: String = "",
    val savedLocations: List<SavedLocation> = emptyList(),
    val activeLocationId: String? = null,
    val useCurrentLocation: Boolean = false,
    val gpsLocationName: String? = null,
    val minhag: Minhag = Minhag.ASHKENAZI,
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
    val selectedZmanim: Set<String> = setOf("Sunrise", "Sunset", "Nightfall")
)
