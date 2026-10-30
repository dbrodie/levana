package ninja.doskey.app.levana.ui.birthday

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.SupportedHebrewYears
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

    // The stored date exactly as stored, kept while its year is outside
    // SupportedHebrewYears and the user has not changed the date, so Save
    // writes it back unchanged (TASK-24)
    private var preservedDate: Triple<Int, Int, Int>? = null

    // True once the user changed the date themselves
    private var dateChanged = false

    // The add-mode date, kept while a picked contact's stored birthday is
    // shown in its place and restored when a contact without one is chosen,
    // so one contact's date is never saved to another (TASK-51)
    private var addModeDate: AddModeDate? = null

    private class AddModeDate(
        val date: Triple<Int, Int, Int>,
        val yearChosen: Boolean,
        val changed: Boolean
    )

    init {
        viewModelScope.launch {
            val year = currentYear.await()
            if (!yearChosen) setDate(year = SupportedHebrewYears.coerce(year), chosen = false)
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
            is ContactBirthdayIntent.OpenDatePicker ->
                _state.value = _state.value.copy(showDatePicker = true)
            is ContactBirthdayIntent.DismissDatePicker ->
                _state.value = _state.value.copy(showDatePicker = false)
            is ContactBirthdayIntent.SetHebrewDate -> {
                _state.value = _state.value.copy(showDatePicker = false)
                setPickedDate(intent.day, intent.month, intent.year)
            }
            is ContactBirthdayIntent.Save -> save()
        }
    }

    /**
     * A date the user picked. Picking the shown date changes nothing, so an untouched
     * out-of-range stored date is still saved as stored (TASK-24). The shown year is kept,
     * even a stored one outside SupportedHebrewYears; another year is limited to that range.
     * The fields that differ count as edited for a load still pending.
     */
    private fun setPickedDate(day: Int, month: Int, year: Int) {
        val s = _state.value
        if (day != s.hebrewDay) editedWhileLoading += DateField.DAY
        if (month != s.hebrewMonth) editedWhileLoading += DateField.MONTH
        if (year != s.hebrewYear) editedWhileLoading += DateField.YEAR
        if (day == s.hebrewDay && month == s.hebrewMonth && year == s.hebrewYear) return
        preservedDate = null
        dateChanged = true
        setDate(day, month, if (year == s.hebrewYear) year else SupportedHebrewYears.coerce(year))
    }

    /**
     * Sets the editor's date. A year the user enters is limited to
     * SupportedHebrewYears by the caller; a day or month change keeps the
     * year shown, even a stored one outside that range. The year is always
     * kept inside the range the calendar library handles, and the month is
     * one that year has (Adar II becomes Adar when the year is stepped to a
     * regular year), so a date that cannot exist is never held in state or
     * saved. The day is clamped to the
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

    /**
     * A contact the user chose. Its stored birthday, if any, is looked up
     * before Save is allowed, so it is edited rather than silently replaced.
     */
    private fun selectContact(lookupKey: String, name: String, photoUri: String?) {
        _state.value = _state.value.copy(
            contactLookupKey = lookupKey,
            contactName = name,
            contactPhotoUri = photoUri,
            storedBirthdayUnchecked = true
        )
        load(lookupKey, fromPick = true)
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
        pickJob?.cancel()
        load(contactLookupKey, fromPick = false)
    }

    /**
     * Loads the birthday stored for [contactLookupKey] into the editor. Opened
     * from the list, date fields the user changed while it loaded win. After
     * a contact is picked ([fromPick]) the stored date always wins, with a
     * notice saying so; a contact without one returns the editor to add mode.
     */
    private fun load(contactLookupKey: String, fromPick: Boolean) {
        supersedeLoad()
        editedWhileLoading.clear()
        preservedDate = null
        val token = loadToken
        loadJob = viewModelScope.launch {
            val birthday = if (fromPick) {
                try {
                    repository.getBirthdayForContact(contactLookupKey)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Save stays off: the stored birthday is unknown
                    if (token == loadToken) _state.value = _state.value.copy(errorMessage = e.message)
                    return@launch
                }
            } else {
                repository.getBirthdayForContact(contactLookupKey)
            }
            if (birthday == null) {
                if (token != loadToken) return@launch
                if (fromPick) returnToAddMode() else _state.value = _state.value.copy(storedBirthdayUnchecked = false)
            } else {
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
                val stored = Triple(birthday.hebrewDay, birthday.hebrewMonth, birthday.hebrewYear)
                val notice = if (fromPick) {
                    // The first switch away from add mode keeps its date
                    val addMode = addModeDate ?: AddModeDate(
                        Triple(current.hebrewDay, current.hebrewMonth, current.hebrewYear),
                        yearChosen,
                        dateChanged
                    ).takeIf { !current.isEditing }
                    addModeDate = addMode
                    StoredBirthdayNotice(
                        day = stored.first,
                        month = stored.second,
                        year = stored.third,
                        replacedChosenDate = addMode != null && addMode.changed &&
                            addMode.date != Triple(day, month, year)
                    )
                } else {
                    null
                }
                _state.value = current.copy(
                    isEditing = true,
                    contactLookupKey = birthday.contactLookupKey,
                    contactName = birthday.contactName,
                    contactPhotoUri = birthday.contactPhotoUri,
                    hebrewDay = day,
                    hebrewMonth = month,
                    hebrewYear = year,
                    storedBirthdayUnchecked = false,
                    storedBirthdayNotice = notice
                )
                if (!fromPick && editedWhileLoading.isNotEmpty()) {
                    // Keep what the user changed meanwhile, normalised
                    // against the resulting month and year
                    setDate(
                        day = if (DateField.DAY in editedWhileLoading) current.hebrewDay else day,
                        month = if (DateField.MONTH in editedWhileLoading) current.hebrewMonth else month,
                        year = if (DateField.YEAR in editedWhileLoading) current.hebrewYear else year
                    )
                } else {
                    preservedDate = stored.takeIf { birthday.hebrewYear !in SupportedHebrewYears }
                }
                yearChosen = true
            }
        }
    }

    /** A picked contact has no stored birthday: a new one is added, on the add-mode date. */
    private fun returnToAddMode() {
        val addMode = addModeDate
        addModeDate = null
        _state.value = _state.value.copy(
            isEditing = false,
            storedBirthdayUnchecked = false,
            storedBirthdayNotice = null
        )
        if (addMode != null) {
            _state.value = _state.value.copy(
                hebrewDay = addMode.date.first,
                hebrewMonth = addMode.date.second,
                hebrewYear = addMode.date.third
            )
            yearChosen = addMode.yearChosen
            dateChanged = addMode.changed
        }
    }

    private fun save() {
        val s = _state.value
        // Never before the chosen contact's stored birthday is known (TASK-51)
        if (s.contactLookupKey.isBlank() || s.storedBirthdayUnchecked) return

        // Save what the editor shows; a load still pending must not replace it
        supersedeLoad()
        _state.value = s.copy(isSaving = true)
        // An untouched out-of-range stored date is written back as stored
        val (day, month, year) = preservedDate ?: Triple(s.hebrewDay, s.hebrewMonth, s.hebrewYear)

        viewModelScope.launch {
            try {
                repository.setBirthday(
                    s.contactLookupKey,
                    day,
                    month,
                    year
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
