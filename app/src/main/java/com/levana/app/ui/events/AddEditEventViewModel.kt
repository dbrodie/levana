package com.levana.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.PersonalEventRepository
import com.levana.app.data.db.PersonalEvent
import com.levana.app.domain.model.EventType
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
            is AddEditEventIntent.SetEventType ->
                _state.value = _state.value.copy(eventType = intent.eventType)
            is AddEditEventIntent.SetName ->
                _state.value = _state.value.copy(name = intent.name)
            is AddEditEventIntent.SetCustomTitle ->
                _state.value = _state.value.copy(customTitle = intent.title)
            is AddEditEventIntent.SetHebrewDay ->
                _state.value = _state.value.copy(hebrewDay = intent.day)
            is AddEditEventIntent.SetHebrewMonth ->
                _state.value = _state.value.copy(hebrewMonth = intent.month)
            is AddEditEventIntent.SetHebrewYear ->
                _state.value = _state.value.copy(hebrewYear = intent.year)
            is AddEditEventIntent.SetNotes ->
                _state.value = _state.value.copy(notes = intent.notes)
            is AddEditEventIntent.PreFillDate ->
                _state.value = _state.value.copy(
                    hebrewDay = intent.hebrewDay,
                    hebrewMonth = intent.hebrewMonth,
                    hebrewYear = intent.hebrewYear
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
                eventType = event.eventType,
                name = event.name,
                customTitle = event.customTitle ?: "",
                hebrewDay = event.hebrewDay,
                hebrewMonth = event.hebrewMonth,
                hebrewYear = event.hebrewYear,
                notes = event.notes
            )
        }
    }

    private fun save() {
        val s = _state.value
        val nameOrTitle = when (s.eventType) {
            EventType.CUSTOM -> ""
            else -> s.name
        }
        if (s.eventType != EventType.CUSTOM && nameOrTitle.isBlank()) return
        if (s.eventType == EventType.CUSTOM && s.customTitle.isBlank()) return

        _state.value = s.copy(isSaving = true)

        viewModelScope.launch {
            val event = PersonalEvent(
                id = if (s.isEditing) s.eventId else 0,
                name = nameOrTitle,
                eventType = s.eventType,
                customTitle = if (s.eventType == EventType.CUSTOM) {
                    s.customTitle
                } else {
                    null
                },
                hebrewDay = s.hebrewDay,
                hebrewMonth = s.hebrewMonth,
                hebrewYear = s.hebrewYear,
                notes = s.notes
            )
            if (s.isEditing) {
                repository.update(event)
            } else {
                repository.insert(event)
            }
            _state.value = _state.value.copy(isSaving = false, saved = true)
        }
    }
}
