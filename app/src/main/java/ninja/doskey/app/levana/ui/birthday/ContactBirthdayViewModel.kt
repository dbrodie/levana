package ninja.doskey.app.levana.ui.birthday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.HebrewDateMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactBirthdayViewModel(
    private val repository: ContactBirthdayRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ContactBirthdayState())
    val state: StateFlow<ContactBirthdayState> = _state.asStateFlow()

    fun onIntent(intent: ContactBirthdayIntent) {
        when (intent) {
            is ContactBirthdayIntent.LoadBirthday ->
                loadBirthday(intent.contactLookupKey)
            is ContactBirthdayIntent.ContactSelected ->
                _state.value = _state.value.copy(
                    contactLookupKey = intent.lookupKey,
                    contactName = intent.name,
                    contactPhotoUri = intent.photoUri
                )
            is ContactBirthdayIntent.SetHebrewDay ->
                _state.value = _state.value.copy(hebrewDay = intent.day)
            is ContactBirthdayIntent.SetHebrewMonth ->
                setDate(month = intent.month)
            is ContactBirthdayIntent.SetHebrewYear ->
                setDate(year = intent.year)
            is ContactBirthdayIntent.Save -> save()
        }
    }

    /**
     * Sets the editor's date. The year is kept inside the range the calendar
     * library handles, and the month is one that year has (Adar II becomes
     * Adar when the year is stepped to a regular year), so a date that cannot
     * exist is never held in state or saved.
     */
    private fun setDate(
        month: Int = _state.value.hebrewMonth,
        year: Int = _state.value.hebrewYear
    ) {
        val safeYear = year.coerceIn(
            HebrewDateMatcher.MIN_KNOWN_YEAR,
            HebrewDateMatcher.MAX_KNOWN_YEAR
        )
        _state.value = _state.value.copy(
            hebrewMonth = HebrewDateMatcher.monthForYear(month, safeYear),
            hebrewYear = safeYear
        )
    }

    private fun loadBirthday(contactLookupKey: String) {
        viewModelScope.launch {
            val birthday =
                repository.getBirthdayForContact(contactLookupKey)
            if (birthday != null) {
                _state.value = ContactBirthdayState(
                    isEditing = true,
                    contactLookupKey = birthday.contactLookupKey,
                    contactName = birthday.contactName,
                    contactPhotoUri = birthday.contactPhotoUri,
                    hebrewDay = birthday.hebrewDay,
                    hebrewMonth = birthday.hebrewMonth,
                    hebrewYear = if (
                        HebrewDateMatcher.isKnownYear(birthday.hebrewYear)
                    ) {
                        birthday.hebrewYear
                    } else {
                        DEFAULT_BIRTHDAY_YEAR
                    }
                )
            }
        }
    }

    private fun save() {
        val s = _state.value
        if (s.contactLookupKey.isBlank()) return

        _state.value = s.copy(isSaving = true)

        viewModelScope.launch {
            try {
                repository.setBirthday(
                    s.contactLookupKey,
                    s.hebrewDay,
                    s.hebrewMonth,
                    s.hebrewYear
                )
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveComplete = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    errorMessage = e.message
                )
            }
        }
    }
}
