package com.levana.app.domain.model

import com.levana.app.data.db.PersonalEvent

sealed interface CalendarEvent {
    val title: String
    val hebrewDay: Int
    val hebrewMonth: Int
    val hebrewYear: Int

    data class Birthday(val birthday: ContactBirthday) : CalendarEvent {
        override val title = birthday.contactName
        override val hebrewDay = birthday.hebrewDay
        override val hebrewMonth = birthday.hebrewMonth
        override val hebrewYear = birthday.hebrewYear
    }

    data class CustomEvent(val event: PersonalEvent) : CalendarEvent {
        override val title = event.title
        override val hebrewDay = event.hebrewDay
        override val hebrewMonth = event.hebrewMonth
        override val hebrewYear = event.hebrewYear
    }
}
