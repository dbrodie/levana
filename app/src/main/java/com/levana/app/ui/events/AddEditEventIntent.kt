package com.levana.app.ui.events

sealed interface AddEditEventIntent {
    data class LoadEvent(val eventId: Long) : AddEditEventIntent
    data class SetTitle(val title: String) : AddEditEventIntent
    data class SetHebrewDay(val day: Int) : AddEditEventIntent
    data class SetHebrewMonth(val month: Int) : AddEditEventIntent
    data class SetHebrewYear(val year: Int) : AddEditEventIntent
    data class SetNotes(val notes: String) : AddEditEventIntent
    data class SetUseYahrzeitRules(
        val enabled: Boolean
    ) : AddEditEventIntent
    data class PreFillDate(
        val day: Int,
        val month: Int,
        val year: Int
    ) : AddEditEventIntent
    data object Save : AddEditEventIntent
}
