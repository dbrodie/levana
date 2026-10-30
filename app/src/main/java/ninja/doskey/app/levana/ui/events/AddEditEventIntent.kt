package ninja.doskey.app.levana.ui.events

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate

sealed interface AddEditEventIntent {
    data class LoadEvent(val eventId: Long) : AddEditEventIntent
    data class SetTitle(val title: String) : AddEditEventIntent
    data object OpenDatePicker : AddEditEventIntent
    data object DismissDatePicker : AddEditEventIntent

    /** A date picked in the date picker, which closes it. */
    data class SetHebrewDate(val day: Int, val month: Int, val year: Int) : AddEditEventIntent {
        constructor(date: LocalDate) : this(JewishDate(date))
        private constructor(date: JewishDate) : this(date.jewishDayOfMonth, date.jewishMonth, date.jewishYear)
    }
    data class SetNotes(val notes: String) : AddEditEventIntent
    data class SetUseYahrzeitRules(
        val enabled: Boolean
    ) : AddEditEventIntent
    data class PreFillDate(
        val day: Int,
        val month: Int,
        val year: Int
    ) : AddEditEventIntent
    data object Save : AddEditEventIntent
}
