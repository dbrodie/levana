package com.levana.app.ui.birthday

data class ContactBirthdayState(
    val isEditing: Boolean = false,
    val contactLookupKey: String = "",
    val contactName: String = "",
    val contactPhotoUri: String? = null,
    val hebrewDay: Int = 1,
    // Tishrei
    val hebrewMonth: Int = 7,
    // 0 = unknown
    val hebrewYear: Int = 0,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val errorMessage: String? = null
)
