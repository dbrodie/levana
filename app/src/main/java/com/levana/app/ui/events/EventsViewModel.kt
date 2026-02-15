package com.levana.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.PersonalEventRepository
import com.levana.app.data.db.PersonalEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventsViewModel(
    private val repository: PersonalEventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventsState())
    val state: StateFlow<EventsState> = _state.asStateFlow()

    init {
        observeEvents()
    }

    fun onIntent(intent: EventsIntent) {
        when (intent) {
            is EventsIntent.LoadEvents -> { /* already observing */ }
            is EventsIntent.DeleteEvent -> deleteEvent(intent.event)
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            repository.getAll().collect { events ->
                _state.value = EventsState(
                    events = events,
                    isLoading = false
                )
            }
        }
    }

    private fun deleteEvent(event: PersonalEvent) {
        viewModelScope.launch {
            repository.delete(event)
        }
    }
}
