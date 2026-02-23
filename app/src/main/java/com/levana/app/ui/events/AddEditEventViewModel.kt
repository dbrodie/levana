package com.levana.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.PersonalEventRepository
import com.levana.app.data.db.PersonalEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddEditEventViewModel(
    private val repository: PersonalEventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditEventState())
    val state: StateFlow<AddEditEventState> = _state.asStateFlow()

    fun onIntent(intent: AddEditEventIntent) {
        when (intent) {
            is AddEditEventIntent.LoadEvent -> loadEvent(intent.eventId)
            is AddEditEventIntent.SetTitle ->
                _state.value = _state.value.copy(title = intent.title)
            is AddEditEventIntent.SetHebrewDay ->
                _state.value = _state.value.copy(hebrewDay = intent.day)
            is AddEditEventIntent.SetHebrewMonth ->
                _state.value = _state.value.copy(hebrewMonth = intent.month)
            is AddEditEventIntent.SetHebrewYear ->
                _state.value = _state.value.copy(hebrewYear = intent.year)
            is AddEditEventIntent.SetNotes ->
                _state.value = _state.value.copy(notes = intent.notes)
            is AddEditEventIntent.SetUseYahrzeitRules ->
                _state.value = _state.value.copy(
                    useYahrzeitRules = intent.enabled
                )
            is AddEditEventIntent.PreFillDate ->
                _state.value = _state.value.copy(
                    hebrewDay = intent.day,
                    hebrewMonth = intent.month,
                    hebrewYear = intent.year
                )
            is AddEditEventIntent.Save -> save()
        }
    }

    private fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            val event = repository.getById(eventId) ?: return@launch
            _state.value = AddEditEventState(
                isEditing = true,
                eventId = event.id,
                title = event.title,
                hebrewDay = event.hebrewDay,
                hebrewMonth = event.hebrewMonth,
                hebrewYear = event.hebrewYear,
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
