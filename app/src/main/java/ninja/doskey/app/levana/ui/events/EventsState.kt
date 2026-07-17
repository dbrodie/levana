package ninja.doskey.app.levana.ui.events

import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.domain.model.ContactBirthday

data class EventsState(
    val birthdays: List<ContactBirthday> = emptyList(),
    val customEvents: List<PersonalEvent> = emptyList(),
    val hasContactsPermission: Boolean = false,
    val isLoading: Boolean = true,
    val exportMessage: String? = null,
    val importResult: ImportResult? = null
)

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val error: String? = null
)
