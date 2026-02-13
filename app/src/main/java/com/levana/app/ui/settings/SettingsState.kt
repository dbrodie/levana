package com.levana.app.ui.settings

import com.levana.app.domain.model.Minhag

data class SettingsState(
    val locationName: String = "",
    val minhag: Minhag = Minhag.ASHKENAZI,
    val isInIsrael: Boolean = false,
    val showModernIsraeliHolidays: Boolean = true,
    val hebrewPrimary: Boolean = false,
    val candleLightingOffset: Double = 18.0
)
