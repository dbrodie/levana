package ninja.doskey.app.levana.ui.birthday

import android.net.Uri

sealed interface ContactBirthdayIntent {
    data class LoadBirthday(
        val contactLookupKey: String
    ) : ContactBirthdayIntent
    /** A contact returned by the picker; resolved off the main thread. */
    data class ContactPicked(val contactUri: Uri) : ContactBirthdayIntent
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
