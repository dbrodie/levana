package ninja.doskey.app.levana.ui.calendar

import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import java.time.LocalDate
import java.time.YearMonth

/**
 * [today] is null until the view model has read the device date; [currentMonth] and
 * [selectedDate] are placeholders until then.
 */
data class CalendarState(
    val currentMonth: YearMonth = YearMonth.of(2000, 1),
    val monthDays: List<HebrewDay> = emptyList(),
    val today: LocalDate? = null,
    val isLoading: Boolean = true,
    val locationName: String = "",
    val calendarHebrewMode: Boolean = false,
    val hebrewYearMonth: HebrewYearMonth? = null,
    val selectedDate: LocalDate = LocalDate.of(2000, 1, 1),
    val showGoToDateDialog: Boolean = false,
    val gregorianMonthCache: Map<YearMonth, List<HebrewDay>> = emptyMap(),
    val hebrewMonthCache: Map<HebrewYearMonth, List<HebrewDay>> = emptyMap()
)
