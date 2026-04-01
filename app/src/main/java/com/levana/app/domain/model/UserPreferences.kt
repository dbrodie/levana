package com.levana.app.domain.model

data class UserPreferences(
    val savedLocations: List<SavedLocation> = emptyList(),
    val locationMode: LocationMode? = null,
    val gpsLocation: Location? = null,
    val candleLightingOffset: Double = 18.0,
    val minhag: Minhag = Minhag.ASHKENAZI,
    val isInIsrael: Boolean = false,
    val showModernIsraeliHolidays: Boolean = true,
    val calendarHebrewMode: Boolean = false,
    val dynamicHolidayTheme: Boolean = true,
    val selectedCalendarIds: Set<Long> = emptySet(),
    val showDeveloperSettings: Boolean = true,
    val devDateOverride: java.time.LocalDate? = null,
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
    val selectedZmanim: Set<String> = setOf("Sunrise", "Sunset", "Nightfall")
)

val UserPreferences.activeLocation: Location?
    get() = when (val mode = locationMode) {
        is LocationMode.Gps -> gpsLocation
        is LocationMode.Saved -> savedLocations.find { it.id == mode.id }?.location
            ?: savedLocations.firstOrNull()?.location  // orphaned id → fall back to first
        null -> null  // no mode set → onboarding
    }
