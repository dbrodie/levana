package ninja.doskey.app.levana.ui.events

import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.domain.model.ContactBirthday

data class EventsState(
    val birthdays: List<ContactBirthday> = emptyList(),
    val customEvents: List<PersonalEvent> = emptyList(),
    val hasContactsPermission: Boolean = false,
    val isLoading: Boolean = true,
    val message: EventsMessage? = null,
    val importResult: ImportResult? = null
)

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val error: String? = null
)

/** One-off snackbar messages; the screen maps them to localized text. */
sealed interface EventsMessage {
    data class Exported(val count: Int) : EventsMessage
    data class ExportFailed(val detail: String) : EventsMessage
    data object ImportReadFailed : EventsMessage
    data class ImportFailed(val detail: String) : EventsMessage
    data class ImportTooLarge(val maxKilobytes: Int) : EventsMessage
    data class RemoveBirthdayFailed(val detail: String) : EventsMessage
}
