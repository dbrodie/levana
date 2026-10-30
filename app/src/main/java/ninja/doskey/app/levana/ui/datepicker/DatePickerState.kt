package ninja.doskey.app.levana.ui.datepicker

import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import java.time.LocalDate
import java.time.YearMonth

/** The calendar whose months the picker's grid steps through. */
enum class LeadingCalendar { HEBREW, GREGORIAN }

/**
 * The shared date picker (TASK-45, design 1b). [selection] is the picked day; nothing is
 * committed until OK. The grid shows [hebrewMonth] while the Hebrew calendar leads and
 * [gregorianMonth] while the Gregorian one does.
 */
data class DatePickerState(
    val selection: LocalDate,
    val leading: LeadingCalendar,
    val hebrewMonth: HebrewYearMonth,
    val gregorianMonth: YearMonth,
    val yearListOpen: Boolean = false,
    /**
     * The Hebrew year of an initial date outside SupportedHebrewYears (a stored date from
     * before TASK-24's range); its days stay selectable so the date opens unchanged.
     */
    val extraHebrewYear: Int? = null,
    /** The device's date (developer override included), for the today marker. */
    val today: LocalDate? = null,
    /** The shown month's days with their holidays, from the app's calendar repository. */
    val days: List<HebrewDay> = emptyList(),
    /** The selected day with its holiday, for the footer. */
    val selectedDay: HebrewDay? = null,
    /** Identifies the picking session that opened this state. */
    val session: Long = 0
)
