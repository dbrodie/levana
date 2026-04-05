package com.levana.app.ui.events

import com.levana.app.data.db.PersonalEvent
import com.levana.app.domain.model.ContactBirthday

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
