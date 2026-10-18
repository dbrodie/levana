package ninja.doskey.app.levana.domain.model

import ninja.doskey.app.levana.data.db.PersonalEvent

/** A personal event observed on a given day; [label] is set when it occurs twice that year. */
data class EventOccurrence(
    val event: PersonalEvent,
    val label: OccurrenceLabel? = null
)
