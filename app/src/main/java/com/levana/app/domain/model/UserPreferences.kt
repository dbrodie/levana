package com.levana.app.domain.model

data class UserPreferences(
    val location: Location? = null,
    val candleLightingOffset: Double = 18.0,
    val minhag: Minhag = Minhag.ASHKENAZI,
    val isInIsrael: Boolean = false,
    val showModernIsraeliHolidays: Boolean = true,
    val hebrewPrimary: Boolean = false,
    val dynamicHolidayTheme: Boolean = true,
    val selectedCalendarIds: Set<Long> = emptySet(),
    val showDeveloperSettings: Boolean = true,
    val devDateOverride: java.time.LocalDate? = null,
    val devForceHolidayTheme: String? = null
)
