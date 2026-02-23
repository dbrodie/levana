package com.levana.app.ui.events

data class AddEditEventState(
    val isEditing: Boolean = false,
    val eventId: Long = 0,
    val title: String = "",
    val hebrewDay: Int = 1,
    // Tishrei
    val hebrewMonth: Int = 7,
    val hebrewYear: Int = 5786,
    val notes: String = "",
    val useYahrzeitRules: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null
)
