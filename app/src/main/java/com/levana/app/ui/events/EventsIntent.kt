package com.levana.app.ui.events

import com.levana.app.data.db.PersonalEvent

sealed interface EventsIntent {
    data object LoadEvents : EventsIntent
    data class DeleteCustomEvent(val event: PersonalEvent) : EventsIntent
    data class DeleteBirthday(val contactLookupKey: String) : EventsIntent
    data object ContactsPermissionGranted : EventsIntent
    data class ShowExportResult(val message: String) : EventsIntent
    data object DismissExportResult : EventsIntent
    data class ImportEvents(val content: String, val isCsv: Boolean) : EventsIntent
    data object DismissImportResult : EventsIntent
}
