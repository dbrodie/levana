package ninja.doskey.app.levana.ui.birthday

// Initial year for the birthday editor, and the fallback when a stored
// birthday has an unknown (0) or out-of-range year
const val DEFAULT_BIRTHDAY_YEAR = 5784

data class ContactBirthdayState(
    val isEditing: Boolean = false,
    val contactLookupKey: String = "",
    val contactName: String = "",
    val contactPhotoUri: String? = null,
    val hebrewDay: Int = 1,
    // Tishrei
    val hebrewMonth: Int = 7,
    // The year shown in the editor is the year that gets saved
    val hebrewYear: Int = DEFAULT_BIRTHDAY_YEAR,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val errorMessage: String? = null
)
