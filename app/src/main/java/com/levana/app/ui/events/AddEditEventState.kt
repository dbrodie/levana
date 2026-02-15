package com.levana.app.ui.events

import com.levana.app.domain.model.EventType

data class AddEditEventState(
    val isEditing: Boolean = false,
    val eventId: Long = 0,
    val eventType: EventType = EventType.BIRTHDAY,
    val name: String = "",
    val customTitle: String = "",
    val hebrewDay: Int = 1,
    // Tishrei
    val hebrewMonth: Int = 7,
    val hebrewYear: Int = 5786,
    val notes: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false
)
