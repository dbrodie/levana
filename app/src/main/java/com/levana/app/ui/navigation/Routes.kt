package com.levana.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object CalendarRoute

@Serializable
data class DayDetailRoute(val dateEpochDay: Long)

@Serializable
data object OnboardingRoute

@Serializable
data object CityPickerRoute

@Serializable
data object ManualLocationRoute
