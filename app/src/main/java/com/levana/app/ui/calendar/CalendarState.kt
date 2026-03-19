package com.levana.app.ui.calendar

import com.levana.app.domain.model.HebrewDay
import com.levana.app.domain.model.HebrewYearMonth
import java.time.LocalDate
import java.time.YearMonth

data class CalendarState(
    val currentMonth: YearMonth = YearMonth.now(),
    val monthDays: List<HebrewDay> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val hebrewMonthHeader: String = "",
    val gregorianHeader: String = "",
    val isLoading: Boolean = true,
    val locationName: String = "",
    val calendarHebrewMode: Boolean = false,
    val hebrewYearMonth: HebrewYearMonth? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val showGoToDateDialog: Boolean = false,
    val gregorianMonthCache: Map<YearMonth, List<HebrewDay>> = emptyMap(),
    val hebrewMonthCache: Map<HebrewYearMonth, List<HebrewDay>> = emptyMap()
)
