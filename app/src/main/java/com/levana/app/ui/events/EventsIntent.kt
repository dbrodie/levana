package com.levana.app.ui.events

import com.levana.app.data.db.PersonalEvent

sealed interface EventsIntent {
    data object LoadEvents : EventsIntent
    data class DeleteCustomEvent(val event: PersonalEvent) : EventsIntent
    data class DeleteBirthday(val contactLookupKey: String) : EventsIntent
    data object ContactsPermissionGranted : EventsIntent
}
