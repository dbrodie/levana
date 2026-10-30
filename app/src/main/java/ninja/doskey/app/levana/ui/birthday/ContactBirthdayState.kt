package ninja.doskey.app.levana.ui.birthday

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.data.HebrewDateMatcher
import java.time.LocalDate

data class ContactBirthdayState(
    val isEditing: Boolean = false,
    val contactLookupKey: String = "",
    val contactName: String = "",
    val contactPhotoUri: String? = null,
    val hebrewDay: Int = 1,
    // Tishrei
    val hebrewMonth: Int = 7,
    // The year shown in the editor is the year that gets saved. Starts as the
    // real current Hebrew year; the view model replaces it with the device's
    // (developer override included) year, which is also used for a stored unknown year.
    val hebrewYear: Int = HebrewDateMatcher.hebrewYearOf(DeviceContext.realToday()),
    val showDatePicker: Boolean = false,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val errorMessage: String? = null,
    // Set while the chosen contact's stored birthday has not been looked up
    // (or the lookup failed); Save stays off so it is never overwritten unseen
    val storedBirthdayUnchecked: Boolean = false,
    // The chosen contact already had a Hebrew birthday, now being edited
    val storedBirthdayNotice: StoredBirthdayNotice? = null
) {
    /** The shown Hebrew date's Gregorian day; the view model only holds real dates. */
    val gregorianDate: LocalDate get() = JewishDate(hebrewYear, hebrewMonth, hebrewDay).localDate
}

/**
 * The birthday stored for a contact picked in add mode (TASK-51): the date as stored, and
 * whether it replaced a date the user had chosen before the pick.
 */
data class StoredBirthdayNotice(
    val day: Int,
    val month: Int,
    val year: Int,
    val replacedChosenDate: Boolean
)
