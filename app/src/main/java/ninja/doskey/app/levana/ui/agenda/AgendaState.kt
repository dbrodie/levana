package ninja.doskey.app.levana.ui.agenda

import ninja.doskey.app.levana.domain.agenda.AgendaDay
import java.time.LocalDate

/**
 * The agenda over the loaded window [start]..[end]. [today] is null until the view model has
 * read the device date. [scrollRequest] asks the list to bring a day into view once its rows
 * are shown; the screen reports it handled.
 */
data class AgendaState(
    val today: LocalDate? = null,
    val hebrewLeading: Boolean = false,
    val days: List<AgendaDay> = emptyList(),
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val isLoading: Boolean = true,
    val loadingEarlier: Boolean = false,
    val loadingLater: Boolean = false,
    /** False when the device calendars cannot be read; the screen offers to allow them. */
    val calendarPermission: Boolean = true,
    /** Set when the selected city's clock differs from the device's (TASK-5). */
    val otherClockCityName: String? = null,
    val datePickerDate: LocalDate? = null,
    val scrollRequest: ScrollRequest? = null,
    /** Non-null while searching. */
    val search: AgendaSearchState? = null
)

/** Scroll to the first listed day on or after [date]; [id] tells repeated requests apart. */
data class ScrollRequest(val date: LocalDate, val id: Long)

data class AgendaSearchState(
    val query: String = "",
    val results: List<AgendaDay> = emptyList(),
    val isLoading: Boolean = true
)
