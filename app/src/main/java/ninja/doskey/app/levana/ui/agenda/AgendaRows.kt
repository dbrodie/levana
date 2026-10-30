package ninja.doskey.app.levana.ui.agenda

import ninja.doskey.app.levana.domain.agenda.AgendaDay

/**
 * A listed day; [header] is its leading-calendar month when the day is the first listed one of
 * that month. The list's key is the day alone, so it is the same in either leading calendar.
 */
internal data class AgendaRow(val day: AgendaDay, val header: AgendaMonth?) {
    val key: String get() = "day-${day.date.toEpochDay()}"
}

/**
 * [days] (in date order) with a header on each day where the leading calendar's month changes.
 * Computed over the whole list, so it does not depend on which loaded chunk a day came from.
 */
internal fun agendaRows(days: List<AgendaDay>, hebrewLeading: Boolean): List<AgendaRow> {
    var month: AgendaMonth? = null
    return days.map { day ->
        val dayMonth = AgendaMonth.of(day.date, hebrewLeading)
        AgendaRow(day, dayMonth.takeIf { it != month }).also { month = dayMonth }
    }
}
