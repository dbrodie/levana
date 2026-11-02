package ninja.doskey.app.levana.ui.agenda

import java.time.LocalDate

sealed interface AgendaIntent {
    /** The list is near its first loaded day. */
    data object LoadEarlier : AgendaIntent
    /** The list is near its last loaded day. */
    data object LoadLater : AgendaIntent
    data class SetHebrewLeading(val hebrew: Boolean) : AgendaIntent
    data object GoToToday : AgendaIntent
    /** Opens the date picker on [date], the day shown at the top of the list. */
    data class OpenDatePicker(val date: LocalDate) : AgendaIntent
    data object CloseDatePicker : AgendaIntent
    data class GoToDate(val date: LocalDate) : AgendaIntent
    data class ScrollHandled(val id: Long) : AgendaIntent
    /** [date] is the day at the top of the list; kept to reopen there after the process is killed. */
    data class TopDayShown(val date: LocalDate) : AgendaIntent
    data object OpenSearch : AgendaIntent
    data object CloseSearch : AgendaIntent
    data class SetSearchQuery(val query: String) : AgendaIntent
    data class SelectSearchResult(val date: LocalDate) : AgendaIntent
    /** Shows [date] in the day panel beside the list (wide windows). */
    data class SelectDay(val date: LocalDate) : AgendaIntent
    data object Resume : AgendaIntent
    data object Pause : AgendaIntent
}
