package com.levana.app.ui.calendar

import java.time.YearMonth

sealed interface CalendarIntent {
    data object LoadToday : CalendarIntent
    data class LoadMonth(val yearMonth: YearMonth) : CalendarIntent
    data object NextMonth : CalendarIntent
    data object PreviousMonth : CalendarIntent
}
