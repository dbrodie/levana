package ninja.doskey.app.levana.ui.birthday

import android.net.Uri
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate

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
    data object OpenDatePicker : ContactBirthdayIntent
    data object DismissDatePicker : ContactBirthdayIntent

    /** A date picked in the date picker, which closes it. */
    data class SetHebrewDate(val day: Int, val month: Int, val year: Int) : ContactBirthdayIntent {
        constructor(date: LocalDate) : this(JewishDate(date))
        private constructor(date: JewishDate) : this(date.jewishDayOfMonth, date.jewishMonth, date.jewishYear)
    }
    data object Save : ContactBirthdayIntent
}
