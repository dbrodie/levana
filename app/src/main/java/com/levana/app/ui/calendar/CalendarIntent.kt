package com.levana.app.ui.calendar

sealed interface CalendarIntent {
    data object LoadToday : CalendarIntent
}
