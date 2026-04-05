package com.levana.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.ContactBirthdayRepository
import com.levana.app.data.EventSerializer
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
            is EventsIntent.ShowExportResult -> {
                _state.value = _state.value.copy(exportMessage = intent.message)
            }
            is EventsIntent.DismissExportResult -> {
                _state.value = _state.value.copy(exportMessage = null)
            }
            is EventsIntent.ImportEvents -> importEvents(intent.content, intent.isCsv)
            is EventsIntent.DismissImportResult -> {
                _state.value = _state.value.copy(importResult = null)
            }
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

    private fun importEvents(content: String, isCsv: Boolean) {
        viewModelScope.launch {
            val parsed = try {
                if (isCsv) EventSerializer.fromCsv(content) else EventSerializer.fromJson(content)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    importResult = ImportResult(
                        imported = 0,
                        skipped = 0,
                        error = "Could not parse file: ${e.message}"
                    )
                )
                return@launch
            }

            val existing = personalEventRepository.getAllOnce()
            val existingKeys = existing.map { DuplicateKey(it.title, it.hebrewDay, it.hebrewMonth, it.hebrewYear) }.toSet()

            var imported = 0
            var skipped = 0
            for (event in parsed) {
                val key = DuplicateKey(event.title, event.hebrewDay, event.hebrewMonth, event.hebrewYear)
                if (key in existingKeys) {
                    skipped++
                } else {
                    personalEventRepository.insert(event)
                    imported++
                }
            }

            _state.value = _state.value.copy(
                importResult = ImportResult(imported = imported, skipped = skipped)
            )
        }
    }

    private data class DuplicateKey(
        val title: String,
        val hebrewDay: Int,
        val hebrewMonth: Int,
        val hebrewYear: Int
    )
}
