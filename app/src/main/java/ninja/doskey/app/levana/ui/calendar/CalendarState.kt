package ninja.doskey.app.levana.ui.calendar

import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
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
