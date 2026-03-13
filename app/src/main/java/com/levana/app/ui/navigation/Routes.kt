package com.levana.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object CalendarRoute

@Serializable
data object OnboardingRoute

@Serializable
data object CityPickerRoute

@Serializable
data object ManualLocationRoute

@Serializable
data class ZmanimRoute(val dateEpochDay: Long = 0L)

@Serializable
data object SettingsRoute

@Serializable
data object PersonalEventsRoute

@Serializable
data class AddEditEventRoute(
    val eventId: Long = 0,
    val prefillDay: Int = 0,
    val prefillMonth: Int = 0,
    val prefillYear: Int = 0
)

@Serializable
data class ContactBirthdayRoute(val contactLookupKey: String = "")

@Serializable
data object CalendarSelectionRoute

@Serializable
data object HalachicTimesSettingsRoute

@Serializable
data object LocationsSettingsRoute
