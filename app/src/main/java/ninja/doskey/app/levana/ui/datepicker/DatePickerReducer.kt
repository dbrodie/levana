package ninja.doskey.app.levana.ui.datepicker

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import ninja.doskey.app.levana.domain.model.SupportedHebrewYears
import java.time.LocalDate
import java.time.YearMonth

/**
 * The picker's rules, free of Compose and Android so they are unit-tested directly.
 *
 * Bounds follow TASK-24's single policy: a day can be picked when its Hebrew year is in
 * [SupportedHebrewYears], or is the year of an initial date outside it, so a stored
 * out-of-range date opens unchanged and can still be adjusted within its own year; the
 * year list then moves it into the range.
 */
object DatePickerReducer {

    fun initial(
        selection: LocalDate,
        leading: LeadingCalendar,
        today: LocalDate? = null,
        session: Long = 0
    ): DatePickerState =
        DatePickerState(
            selection = selection,
            leading = leading,
            hebrewMonth = HebrewYearMonth.from(selection),
            gregorianMonth = YearMonth.from(selection),
            extraHebrewYear = HebrewDateMatcher.hebrewYearOf(selection).takeIf { it !in SupportedHebrewYears },
            today = today,
            session = session
        )

    fun reduce(state: DatePickerState, intent: DatePickerIntent): DatePickerState = when (intent) {
        is DatePickerIntent.Open -> initial(intent.initialDate, intent.leading, state.today, intent.session)
        // The newly leading calendar shows the month holding the selection
        is DatePickerIntent.SetLeading -> showSelection(state.copy(leading = intent.leading))
        is DatePickerIntent.PreviousMonth -> stepped(state, forward = false) ?: state
        is DatePickerIntent.NextMonth -> stepped(state, forward = true) ?: state
        is DatePickerIntent.SelectDay ->
            if (isSelectable(state, intent.date)) state.copy(selection = intent.date) else state
        is DatePickerIntent.ToggleYearList -> state.copy(yearListOpen = !state.yearListOpen)
        is DatePickerIntent.PickYear -> pickYear(state, intent.year)
    }

    fun isAllowedHebrewYear(state: DatePickerState, hebrewYear: Int): Boolean =
        hebrewYear in SupportedHebrewYears || hebrewYear == state.extraHebrewYear

    fun isSelectable(state: DatePickerState, date: LocalDate): Boolean =
        isAllowedHebrewYear(state, HebrewDateMatcher.hebrewYearOf(date))

    fun canGoPrevious(state: DatePickerState): Boolean = stepped(state, forward = false) != null

    fun canGoNext(state: DatePickerState): Boolean = stepped(state, forward = true) != null

    /** The years the year list offers in the leading calendar. */
    fun yearOptions(state: DatePickerState): IntRange = when (state.leading) {
        LeadingCalendar.HEBREW -> SupportedHebrewYears.range
        LeadingCalendar.GREGORIAN -> SupportedHebrewYears.gregorianYears
    }

    /** The shown month's year in the leading calendar; the year list highlights it. */
    fun viewedYear(state: DatePickerState): Int = when (state.leading) {
        LeadingCalendar.HEBREW -> state.hebrewMonth.year
        LeadingCalendar.GREGORIAN -> state.gregorianMonth.year
    }

    /** Today's year in the leading calendar, or null before the device date is known. */
    fun currentYear(state: DatePickerState): Int? = state.today?.let { today ->
        when (state.leading) {
            LeadingCalendar.HEBREW -> HebrewDateMatcher.hebrewYearOf(today)
            LeadingCalendar.GREGORIAN -> today.year
        }
    }

    /** [state] showing the leading calendar's next or previous month, or null past the bounds. */
    private fun stepped(state: DatePickerState, forward: Boolean): DatePickerState? = when (state.leading) {
        LeadingCalendar.HEBREW ->
            (if (forward) state.hebrewMonth.next() else state.hebrewMonth.previous())
                .takeIf { isAllowedHebrewYear(state, it.year) }
                ?.let { state.copy(hebrewMonth = it) }
        LeadingCalendar.GREGORIAN ->
            state.gregorianMonth.plusMonths(if (forward) 1L else -1L)
                .takeIf { hasSelectableDay(state, it) }
                ?.let { state.copy(gregorianMonth = it) }
    }

    // A Gregorian month spans at most two Hebrew years, so its ends tell
    private fun hasSelectableDay(state: DatePickerState, month: YearMonth): Boolean =
        isSelectable(state, month.atDay(1)) || isSelectable(state, month.atEndOfMonth())

    private fun showSelection(state: DatePickerState): DatePickerState = state.copy(
        hebrewMonth = HebrewYearMonth.from(state.selection),
        gregorianMonth = YearMonth.from(state.selection),
        yearListOpen = false
    )

    /**
     * Moves the selection to the same day and month in [year] (owner decision 2026-10-30)
     * and shows it. A Hebrew Adar II becomes Adar in an ordinary year and a day 30 becomes
     * 29 where the month is short, the editors' rules; 29 February becomes the 28th.
     */
    private fun pickYear(state: DatePickerState, year: Int): DatePickerState {
        if (year !in yearOptions(state)) return state
        val selection = when (state.leading) {
            LeadingCalendar.HEBREW -> {
                val current = JewishDate(state.selection)
                val month = HebrewDateMatcher.monthForYear(current.jewishMonth, year)
                val day = HebrewDateMatcher.clampDay(current.jewishDayOfMonth, month, year)
                JewishDate(year, month, day).localDate
            }
            LeadingCalendar.GREGORIAN -> state.selection.withYear(year)
        }
        return showSelection(state.copy(selection = selection))
    }
}
