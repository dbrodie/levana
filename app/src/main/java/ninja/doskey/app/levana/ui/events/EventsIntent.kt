package ninja.doskey.app.levana.ui.events

import ninja.doskey.app.levana.data.db.PersonalEvent

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
