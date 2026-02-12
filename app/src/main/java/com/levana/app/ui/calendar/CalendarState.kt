package com.levana.app.ui.calendar

import com.levana.app.domain.model.HebrewDay
import java.time.LocalDate
import java.time.YearMonth

data class CalendarState(
    val currentMonth: YearMonth = YearMonth.now(),
    val monthDays: List<HebrewDay> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val hebrewMonthHeader: String = "",
    val isLoading: Boolean = true
)
