package com.levana.app.ui.settings

import com.levana.app.domain.model.Minhag

sealed interface SettingsIntent {
    data class SetMinhag(val minhag: Minhag) : SettingsIntent
    data class SetIsInIsrael(val inIsrael: Boolean) : SettingsIntent
    data class SetShowModernIsraeli(val show: Boolean) : SettingsIntent
    data class SetHebrewPrimary(val enabled: Boolean) : SettingsIntent
    data class SetCandleLightingOffset(val offset: Double) : SettingsIntent
    data class SetDynamicHolidayTheme(val enabled: Boolean) : SettingsIntent
}
