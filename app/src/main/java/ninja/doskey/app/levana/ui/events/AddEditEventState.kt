package ninja.doskey.app.levana.ui.events

import java.time.LocalDate
import ninja.doskey.app.levana.data.HebrewDateMatcher

data class AddEditEventState(
    val isEditing: Boolean = false,
    val eventId: Long = 0,
    val title: String = "",
    val hebrewDay: Int = 1,
    // Tishrei
    val hebrewMonth: Int = 7,
    // Starts as the current Hebrew year; the view model replaces it with the
    // app's "today" (date override) year, also used for a stored unknown year
    val hebrewYear: Int = HebrewDateMatcher.hebrewYearOf(LocalDate.now()),
    val notes: String = "",
    val useYahrzeitRules: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null
)
