package com.levana.app.domain.model

data class ContactBirthday(
    val contactLookupKey: String,
    val contactName: String,
    val contactPhotoUri: String?,
    val hebrewDay: Int,
    val hebrewMonth: Int,
    // 0 = unknown year
    val hebrewYear: Int
)
