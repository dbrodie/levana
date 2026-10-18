package ninja.doskey.app.levana.ui.birthday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.data.TodayProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactBirthdayViewModel(
    private val repository: ContactBirthdayRepository,
    private val todayProvider: TodayProvider
) : ViewModel() {

    private val _state = MutableStateFlow(ContactBirthdayState())
    val state: StateFlow<ContactBirthdayState> = _state.asStateFlow()

    // The app's current Hebrew year (honours the developer date override),
    // resolved once when the editor opens
    private val currentYear: Deferred<Int> = viewModelScope.async {
        HebrewDateMatcher.hebrewYearOf(todayProvider.today())
    }

    // True once the year came from the user, a prefill or a stored item, so
    // the default year no longer applies
    private var yearChosen = false

    init {
        viewModelScope.launch {
            val year = currentYear.await()
            if (!yearChosen) setDate(year = year, chosen = false)
        }
    }

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
                setDate(day = intent.day)
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
     * exist is never held in state or saved. The day is clamped to the
     * month's length in that year (spec 1e).
     */
    private fun setDate(
        day: Int = _state.value.hebrewDay,
        month: Int = _state.value.hebrewMonth,
        year: Int = _state.value.hebrewYear,
        chosen: Boolean = true
    ) {
        if (chosen) yearChosen = true
        val safeYear = year.coerceIn(
            HebrewDateMatcher.MIN_KNOWN_YEAR,
            HebrewDateMatcher.MAX_KNOWN_YEAR
        )
        val safeMonth = HebrewDateMatcher.monthForYear(month, safeYear)
        _state.value = _state.value.copy(
            // Day 30 becomes 29 when that month has 29 days in that year, so
            // what the editor shows is what gets saved. Not restored later.
            hebrewDay = HebrewDateMatcher.clampDay(day, safeMonth, safeYear),
            hebrewMonth = safeMonth,
            hebrewYear = safeYear
        )
    }

    private fun loadBirthday(contactLookupKey: String) {
        viewModelScope.launch {
            val birthday =
                repository.getBirthdayForContact(contactLookupKey)
            if (birthday != null) {
                // A birthday stored without a usable year opens on the
                // current Hebrew year
                val (day, month, year) = HebrewDateMatcher.editorDate(
                    birthday.hebrewDay,
                    birthday.hebrewMonth,
                    birthday.hebrewYear,
                    useYahrzeitRules = false,
                    currentYear = currentYear.await()
                )
                yearChosen = true
                _state.value = ContactBirthdayState(
                    isEditing = true,
                    contactLookupKey = birthday.contactLookupKey,
                    contactName = birthday.contactName,
                    contactPhotoUri = birthday.contactPhotoUri,
                    hebrewDay = day,
                    hebrewMonth = month,
                    hebrewYear = year
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
