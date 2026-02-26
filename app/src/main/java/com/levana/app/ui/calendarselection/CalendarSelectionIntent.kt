package com.levana.app.ui.calendarselection

sealed interface CalendarSelectionIntent {
    data object Load : CalendarSelectionIntent
    data class ToggleCalendar(val calendarId: Long) : CalendarSelectionIntent
    data object PermissionGranted : CalendarSelectionIntent
}
