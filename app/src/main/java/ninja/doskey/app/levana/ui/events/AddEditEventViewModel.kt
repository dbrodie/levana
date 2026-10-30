package ninja.doskey.app.levana.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.SupportedHebrewYears
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import ninja.doskey.app.levana.data.db.PersonalEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddEditEventViewModel(
    private val repository: PersonalEventRepository,
    private val deviceContext: DeviceContext
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditEventState())
    val state: StateFlow<AddEditEventState> = _state.asStateFlow()

    // The device's current Hebrew year (developer override included),
    // resolved once when the editor opens
    private val currentYear: Deferred<Int> = viewModelScope.async {
        HebrewDateMatcher.hebrewYearOf(deviceContext.today())
    }

    // True once the year came from the user, a prefill or a stored item, so
    // the default year no longer applies
    private var yearChosen = false

    // The stored date exactly as stored, kept while its year is outside
    // SupportedHebrewYears and the user has not changed the date, so Save
    // writes it back unchanged (TASK-24)
    private var preservedDate: Triple<Int, Int, Int>? = null

    init {
        viewModelScope.launch {
            val year = currentYear.await()
            if (!yearChosen) setDate(year = SupportedHebrewYears.coerce(year), chosen = false)
        }
    }

    fun onIntent(intent: AddEditEventIntent) {
        when (intent) {
            is AddEditEventIntent.LoadEvent -> loadEvent(intent.eventId)
            is AddEditEventIntent.SetTitle ->
                _state.value = _state.value.copy(title = intent.title)
            is AddEditEventIntent.SetHebrewDay -> {
                preservedDate = null
                setDate(day = intent.day)
            }
            is AddEditEventIntent.SetHebrewMonth -> {
                preservedDate = null
                setDate(month = intent.month)
            }
            is AddEditEventIntent.SetHebrewYear -> {
                preservedDate = null
                setDate(year = SupportedHebrewYears.coerce(intent.year))
            }
            is AddEditEventIntent.SetNotes ->
                _state.value = _state.value.copy(notes = intent.notes)
            is AddEditEventIntent.SetUseYahrzeitRules ->
                _state.value = _state.value.copy(
                    useYahrzeitRules = intent.enabled
                )
            is AddEditEventIntent.PreFillDate -> {
                preservedDate = null
                // A calendar date is kept as picked; an unusable year is not
                val year = if (HebrewDateMatcher.isKnownYear(intent.year)) {
                    intent.year
                } else {
                    SupportedHebrewYears.coerce(intent.year)
                }
                setDate(day = intent.day, month = intent.month, year = year)
            }
            is AddEditEventIntent.Save -> save()
        }
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

    private fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            val event = repository.getById(eventId) ?: return@launch
            // An event stored without a usable year (imported with year 0)
            // opens on the current Hebrew year
            val (day, month, year) = HebrewDateMatcher.editorDate(
                event.hebrewDay,
                event.hebrewMonth,
                event.hebrewYear,
                event.useYahrzeitRules,
                currentYear = currentYear.await()
            )
            yearChosen = true
            preservedDate = Triple(event.hebrewDay, event.hebrewMonth, event.hebrewYear)
                .takeIf { event.hebrewYear !in SupportedHebrewYears }
            _state.value = AddEditEventState(
                isEditing = true,
                eventId = event.id,
                title = event.title,
                hebrewDay = day,
                hebrewMonth = month,
                hebrewYear = year,
                notes = event.notes,
                useYahrzeitRules = event.useYahrzeitRules
            )
        }
    }

    private fun save() {
        val s = _state.value
        if (s.title.isBlank()) return

        _state.value = s.copy(isSaving = true)

        // An untouched out-of-range stored date is written back as stored
        val (day, month, year) = preservedDate ?: Triple(s.hebrewDay, s.hebrewMonth, s.hebrewYear)

        viewModelScope.launch {
            val event = PersonalEvent(
                id = if (s.isEditing) s.eventId else 0,
                title = s.title,
                hebrewDay = day,
                hebrewMonth = month,
                hebrewYear = year,
                notes = s.notes,
                useYahrzeitRules = s.useYahrzeitRules
            )
            if (s.isEditing) {
                repository.update(event)
            } else {
                repository.insert(event)
            }
            _state.value = _state.value.copy(
                isSaving = false,
                saved = true
            )
        }
    }
}
