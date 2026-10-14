package ninja.doskey.app.levana.domain.model

import ninja.doskey.app.levana.data.db.PersonalEvent

sealed interface CalendarEvent {
    val title: String
    val hebrewDay: Int
    val hebrewMonth: Int
    val hebrewYear: Int

    /** [label] is set when the birthday was moved from a day 30 the month lacks. */
    data class Birthday(
        val birthday: ContactBirthday,
        val label: OccurrenceLabel? = null
    ) : CalendarEvent {
        override val title = birthday.contactName
        override val hebrewDay = birthday.hebrewDay
        override val hebrewMonth = birthday.hebrewMonth
        override val hebrewYear = birthday.hebrewYear
    }

    /** [label] is set when the event is observed twice this year (e.g. both Adars). */
    data class CustomEvent(
        val event: PersonalEvent,
        val label: OccurrenceLabel? = null
    ) : CalendarEvent {
        override val title = event.title
        override val hebrewDay = event.hebrewDay
        override val hebrewMonth = event.hebrewMonth
        override val hebrewYear = event.hebrewYear
    }
}
