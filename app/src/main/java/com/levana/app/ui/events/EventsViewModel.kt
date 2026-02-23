package com.levana.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.ContactBirthdayRepository
import com.levana.app.data.PersonalEventRepository
import com.levana.app.data.db.PersonalEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventsViewModel(
    private val personalEventRepository: PersonalEventRepository,
    private val contactBirthdayRepository: ContactBirthdayRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventsState())
    val state: StateFlow<EventsState> = _state.asStateFlow()

    init {
        observeEvents()
        loadBirthdays()
    }

    fun onIntent(intent: EventsIntent) {
        when (intent) {
            is EventsIntent.LoadEvents -> {
                loadBirthdays()
            }
            is EventsIntent.DeleteCustomEvent -> deleteEvent(intent.event)
            is EventsIntent.DeleteBirthday -> deleteBirthday(
                intent.contactLookupKey
            )
            is EventsIntent.ContactsPermissionGranted -> loadBirthdays()
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            personalEventRepository.getAll().collect { events ->
                _state.value = _state.value.copy(
                    customEvents = events,
                    isLoading = false
                )
            }
        }
    }

    private fun loadBirthdays() {
        viewModelScope.launch {
            val hasPermission =
                contactBirthdayRepository.hasContactsPermission()
            if (hasPermission) {
                val birthdays = contactBirthdayRepository.getAll()
                _state.value = _state.value.copy(
                    birthdays = birthdays,
                    hasContactsPermission = true
                )
            } else {
                _state.value = _state.value.copy(
                    hasContactsPermission = false
                )
            }
        }
    }

    private fun deleteEvent(event: PersonalEvent) {
        viewModelScope.launch {
            personalEventRepository.delete(event)
        }
    }

    private fun deleteBirthday(contactLookupKey: String) {
        viewModelScope.launch {
            contactBirthdayRepository.removeBirthday(contactLookupKey)
            loadBirthdays()
        }
    }
}
