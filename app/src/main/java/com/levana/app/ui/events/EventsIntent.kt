package com.levana.app.ui.events

import com.levana.app.data.db.PersonalEvent

sealed interface EventsIntent {
    data object LoadEvents : EventsIntent
    data class DeleteEvent(val event: PersonalEvent) : EventsIntent
}
