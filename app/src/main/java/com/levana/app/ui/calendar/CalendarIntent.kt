package com.levana.app.ui.calendar

import com.levana.app.domain.model.HebrewYearMonth
import java.time.LocalDate
import java.time.YearMonth

sealed interface CalendarIntent {
    data object LoadToday : CalendarIntent
    data class LoadMonth(val yearMonth: YearMonth) : CalendarIntent
    data object NextMonth : CalendarIntent
    data object PreviousMonth : CalendarIntent
    data class LoadHebrewMonth(val hebrewYearMonth: HebrewYearMonth) : CalendarIntent
    data object NextHebrewMonth : CalendarIntent
    data object PreviousHebrewMonth : CalendarIntent
    data object GoToToday : CalendarIntent
    data class SelectDay(val date: LocalDate) : CalendarIntent
    data object ToggleCalendarHebrewMode : CalendarIntent
    data object OpenGoToDateDialog : CalendarIntent
    data object CloseGoToDateDialog : CalendarIntent
    data class GoToDate(val date: LocalDate) : CalendarIntent
}
