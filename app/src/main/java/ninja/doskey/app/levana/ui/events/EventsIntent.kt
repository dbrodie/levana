package ninja.doskey.app.levana.ui.events

import android.net.Uri
import ninja.doskey.app.levana.data.db.PersonalEvent

sealed interface EventsIntent {
    data object LoadEvents : EventsIntent
    data class DeleteCustomEvent(val event: PersonalEvent) : EventsIntent
    data class DeleteBirthday(val contactLookupKey: String) : EventsIntent
    data object ContactsPermissionGranted : EventsIntent
    data class ExportEvents(val uri: Uri, val isCsv: Boolean) : EventsIntent
    data object DismissMessage : EventsIntent
    data class ImportFromUri(val uri: Uri) : EventsIntent
    data object DismissImportResult : EventsIntent
}
