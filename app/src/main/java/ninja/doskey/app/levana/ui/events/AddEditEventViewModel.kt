package ninja.doskey.app.levana.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.TodayProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import ninja.doskey.app.levana.data.db.PersonalEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddEditEventViewModel(
    private val repository: PersonalEventRepository,
    private val todayProvider: TodayProvider
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditEventState())
    val state: StateFlow<AddEditEventState> = _state.asStateFlow()

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

    fun onIntent(intent: AddEditEventIntent) {
        when (intent) {
            is AddEditEventIntent.LoadEvent -> loadEvent(intent.eventId)
            is AddEditEventIntent.SetTitle ->
                _state.value = _state.value.copy(title = intent.title)
            is AddEditEventIntent.SetHebrewDay ->
                _state.value = _state.value.copy(hebrewDay = intent.day)
            is AddEditEventIntent.SetHebrewMonth ->
                setDate(month = intent.month)
            is AddEditEventIntent.SetHebrewYear ->
                setDate(year = intent.year)
            is AddEditEventIntent.SetNotes ->
                _state.value = _state.value.copy(notes = intent.notes)
            is AddEditEventIntent.SetUseYahrzeitRules ->
                _state.value = _state.value.copy(
                    useYahrzeitRules = intent.enabled
                )
            is AddEditEventIntent.PreFillDate -> {
                _state.value = _state.value.copy(hebrewDay = intent.day)
                setDate(month = intent.month, year = intent.year)
            }
            is AddEditEventIntent.Save -> save()
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
        year: Int = _state.value.hebrewYear,
        chosen: Boolean = true
    ) {
        if (chosen) yearChosen = true
        val safeYear = year.coerceIn(
            HebrewDateMatcher.MIN_KNOWN_YEAR,
            HebrewDateMatcher.MAX_KNOWN_YEAR
        )
        _state.value = _state.value.copy(
            hebrewMonth = HebrewDateMatcher.monthForYear(month, safeYear),
            hebrewYear = safeYear
        )
    }

    private fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            val event = repository.getById(eventId) ?: return@launch
            // An event stored without a usable year (imported with year 0)
            // opens on the current Hebrew year
            val (month, year) = HebrewDateMatcher.editorDate(
                event.hebrewMonth,
                event.hebrewYear,
                event.useYahrzeitRules,
                currentYear = currentYear.await()
            )
            yearChosen = true
            _state.value = AddEditEventState(
                isEditing = true,
                eventId = event.id,
                title = event.title,
                hebrewDay = event.hebrewDay,
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

        viewModelScope.launch {
            val event = PersonalEvent(
                id = if (s.isEditing) s.eventId else 0,
                title = s.title,
                hebrewDay = s.hebrewDay,
                hebrewMonth = s.hebrewMonth,
                hebrewYear = s.hebrewYear,
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
