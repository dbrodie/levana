package ninja.doskey.app.levana.ui.datepicker

import java.time.LocalDate

sealed interface DatePickerIntent {
    /** Starts picking session [session] on [initialDate]; the previous one is discarded. */
    data class Open(
        val initialDate: LocalDate,
        val leading: LeadingCalendar,
        val session: Long = 0
    ) : DatePickerIntent
    data class SetLeading(val leading: LeadingCalendar) : DatePickerIntent
    data object PreviousMonth : DatePickerIntent
    data object NextMonth : DatePickerIntent
    data class SelectDay(val date: LocalDate) : DatePickerIntent
    data object ToggleYearList : DatePickerIntent

    /** A year of the leading calendar, from the year list. */
    data class PickYear(val year: Int) : DatePickerIntent
}
