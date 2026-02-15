package com.levana.app.ui.events

import com.levana.app.data.db.PersonalEvent

data class EventsState(
    val events: List<PersonalEvent> = emptyList(),
    val isLoading: Boolean = true
)
