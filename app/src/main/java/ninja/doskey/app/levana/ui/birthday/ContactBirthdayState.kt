package ninja.doskey.app.levana.ui.birthday

import java.time.LocalDate
import ninja.doskey.app.levana.data.HebrewDateMatcher

data class ContactBirthdayState(
    val isEditing: Boolean = false,
    val contactLookupKey: String = "",
    val contactName: String = "",
    val contactPhotoUri: String? = null,
    val hebrewDay: Int = 1,
    // Tishrei
    val hebrewMonth: Int = 7,
    // The year shown in the editor is the year that gets saved. Starts as the
    // current Hebrew year; the view model replaces it with the app's "today"
    // (date override) year, which is also used for a stored unknown year.
    val hebrewYear: Int = HebrewDateMatcher.hebrewYearOf(LocalDate.now()),
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val errorMessage: String? = null
)
