package com.levana.app.ui.birthday

sealed interface ContactBirthdayIntent {
    data class LoadBirthday(
        val contactLookupKey: String
    ) : ContactBirthdayIntent
    data class ContactSelected(
        val lookupKey: String,
        val name: String,
        val photoUri: String?
    ) : ContactBirthdayIntent
    data class SetHebrewDay(val day: Int) : ContactBirthdayIntent
    data class SetHebrewMonth(val month: Int) : ContactBirthdayIntent
    data class SetHebrewYear(val year: Int) : ContactBirthdayIntent
    data object Save : ContactBirthdayIntent
}
