package ninja.doskey.app.levana.ui.birthday

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.device.DeviceContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactBirthdayViewModel(
    private val repository: ContactBirthdayRepository,
    private val deviceContext: DeviceContext
) : ViewModel() {

    private val _state = MutableStateFlow(ContactBirthdayState())
    val state: StateFlow<ContactBirthdayState> = _state.asStateFlow()

    // The device's current Hebrew year (developer override included),
    // resolved once when the editor opens
    private val currentYear: Deferred<Int> = viewModelScope.async {
        HebrewDateMatcher.hebrewYearOf(deviceContext.today())
    }

    // True once the year came from the user, a prefill or a stored item, so
    // the default year no longer applies
    private var yearChosen = false

    // Only the latest contact lookup may apply, so a slow earlier pick never
    // replaces a later one
    private var pickJob: Job? = null

    // The stored birthday load. A repeated request for the same record (the
    // screen re-issues it after rotation) is ignored, so neither a pending
    // load nor completed edits are reset. A later contact choice, a load of
    // another record or Save supersedes it; the token stops a superseded load
    // from applying even if it has already resumed.
    private var loadJob: Job? = null
    private var requestedKey: String? = null
    private var loadToken = 0

    // Date fields the user changed while the stored birthday was loading;
    // their values win over the stored ones when the load completes
    private val editedWhileLoading = mutableSetOf<DateField>()

    private enum class DateField { DAY, MONTH, YEAR }

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
            is ContactBirthdayIntent.ContactPicked -> {
                supersedeLoad()
                resolvePickedContact(intent.contactUri)
            }
            is ContactBirthdayIntent.ContactSelected -> {
                supersedeLoad()
                pickJob?.cancel()
                selectContact(intent.lookupKey, intent.name, intent.photoUri)
            }
            is ContactBirthdayIntent.SetHebrewDay -> {
                editedWhileLoading += DateField.DAY
                setDate(day = intent.day)
            }
            is ContactBirthdayIntent.SetHebrewMonth -> {
                editedWhileLoading += DateField.MONTH
                setDate(month = intent.month)
            }
            is ContactBirthdayIntent.SetHebrewYear -> {
                editedWhileLoading += DateField.YEAR
                setDate(year = intent.year)
            }
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

    private fun selectContact(lookupKey: String, name: String, photoUri: String?) {
        _state.value = _state.value.copy(
            contactLookupKey = lookupKey,
            contactName = name,
            contactPhotoUri = photoUri
        )
    }

    private fun resolvePickedContact(contactUri: Uri) {
        pickJob?.cancel()
        pickJob = viewModelScope.launch {
            val contact = try {
                repository.resolveContact(contactUri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.message)
                return@launch
            } ?: return@launch
            selectContact(contact.lookupKey, contact.name, contact.photoUri)
        }
    }

    private fun supersedeLoad() {
        loadToken++
        loadJob?.cancel()
        loadJob = null
    }

    private fun loadBirthday(contactLookupKey: String) {
        if (contactLookupKey == requestedKey) return
        requestedKey = contactLookupKey
        supersedeLoad()
        pickJob?.cancel()
        editedWhileLoading.clear()
        val token = loadToken
        loadJob = viewModelScope.launch {
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
                if (token != loadToken) return@launch
                val current = _state.value
                _state.value = current.copy(
                    isEditing = true,
                    contactLookupKey = birthday.contactLookupKey,
                    contactName = birthday.contactName,
                    contactPhotoUri = birthday.contactPhotoUri,
                    hebrewDay = day,
                    hebrewMonth = month,
                    hebrewYear = year
                )
                if (editedWhileLoading.isNotEmpty()) {
                    // Keep what the user changed meanwhile, normalised
                    // against the resulting month and year
                    setDate(
                        day = if (DateField.DAY in editedWhileLoading) current.hebrewDay else day,
                        month = if (DateField.MONTH in editedWhileLoading) current.hebrewMonth else month,
                        year = if (DateField.YEAR in editedWhileLoading) current.hebrewYear else year
                    )
                }
                yearChosen = true
            }
        }
    }

    private fun save() {
        val s = _state.value
        if (s.contactLookupKey.isBlank()) return

        // Save what the editor shows; a load still pending must not replace it
        supersedeLoad()
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    errorMessage = e.message
                )
            }
        }
    }
}
