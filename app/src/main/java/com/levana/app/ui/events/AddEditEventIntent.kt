package com.levana.app.ui.events

import com.levana.app.domain.model.EventType

sealed interface AddEditEventIntent {
    data class LoadEvent(val eventId: Long) : AddEditEventIntent
    data class SetEventType(val eventType: EventType) : AddEditEventIntent
    data class SetName(val name: String) : AddEditEventIntent
    data class SetCustomTitle(val title: String) : AddEditEventIntent
    data class SetHebrewDay(val day: Int) : AddEditEventIntent
    data class SetHebrewMonth(val month: Int) : AddEditEventIntent
    data class SetHebrewYear(val year: Int) : AddEditEventIntent
    data class SetNotes(val notes: String) : AddEditEventIntent
    data class PreFillDate(
        val hebrewDay: Int,
        val hebrewMonth: Int,
        val hebrewYear: Int
    ) : AddEditEventIntent
    data object Save : AddEditEventIntent
}
