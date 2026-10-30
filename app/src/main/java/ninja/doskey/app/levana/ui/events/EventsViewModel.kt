package ninja.doskey.app.levana.ui.events

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventFileRepository
import ninja.doskey.app.levana.data.EventSerializer
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.db.PersonalEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventsViewModel(
    private val personalEventRepository: PersonalEventRepository,
    private val contactBirthdayRepository: ContactBirthdayRepository,
    private val eventFileRepository: EventFileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventsState())
    val state: StateFlow<EventsState> = _state.asStateFlow()

    // Only the latest birthdays load may publish, so a slow earlier load can
    // never bring back a birthday that was just removed
    private var birthdaysJob: Job? = null

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
            is EventsIntent.ExportEvents -> exportEvents(intent.uri, intent.isCsv)
            is EventsIntent.DismissMessage -> {
                _state.value = _state.value.copy(message = null)
            }
            is EventsIntent.ImportFromUri -> importFromUri(intent.uri)
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
        birthdaysJob?.cancel()
        birthdaysJob = viewModelScope.launch {
            val hasPermission =
                contactBirthdayRepository.hasContactsPermission()
            if (hasPermission) {
                // Known at once: Add opens the editor without waiting for the contacts query
                _state.value = _state.value.copy(hasContactsPermission = true)
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
            try {
                contactBirthdayRepository.removeBirthday(contactLookupKey)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage(EventsMessage.RemoveBirthdayFailed(e.message.orEmpty()))
            }
            loadBirthdays()
        }
    }

    private fun showMessage(message: EventsMessage) {
        _state.value = _state.value.copy(message = message)
    }

    // Exports the events shown when the file was chosen
    private fun exportEvents(uri: Uri, isCsv: Boolean) {
        val events = _state.value.customEvents
        viewModelScope.launch {
            val message = try {
                val content = if (isCsv) {
                    EventSerializer.toCsv(events)
                } else {
                    EventSerializer.toJson(events)
                }
                eventFileRepository.writeText(uri, content)
                EventsMessage.Exported(events.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EventsMessage.ExportFailed(e.message.orEmpty())
            }
            showMessage(message)
        }
    }

    private fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            val content = try {
                eventFileRepository.readText(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: EventFileRepository.FileTooLargeException) {
                showMessage(EventsMessage.ImportTooLarge(e.maxBytes / 1024))
                return@launch
            } catch (e: Exception) {
                showMessage(EventsMessage.ImportFailed(e.message.orEmpty()))
                return@launch
            }
            if (content == null) {
                showMessage(EventsMessage.ImportReadFailed)
                return@launch
            }
            importEvents(content, detectCsv(uri, content))
        }
    }

    private suspend fun importEvents(content: String, isCsv: Boolean) {
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
            return
        }

        val importPlan = deduplicateImportedEvents(
            existing = personalEventRepository.getAllOnce(),
            parsed = parsed
        )

        for (event in importPlan.events) {
            personalEventRepository.insert(event)
        }

        _state.value = _state.value.copy(
            importResult = ImportResult(
                imported = importPlan.events.size,
                skipped = importPlan.skipped
            )
        )
    }
}

internal data class EventImportPlan(
    val events: List<PersonalEvent>,
    val skipped: Int
)

internal fun deduplicateImportedEvents(
    existing: List<PersonalEvent>,
    parsed: List<PersonalEvent>
): EventImportPlan {
    val knownKeys = existing.map {
        DuplicateKey(it.title, it.hebrewDay, it.hebrewMonth, it.hebrewYear)
    }.toMutableSet()
    val eventsToImport = mutableListOf<PersonalEvent>()
    var skipped = 0

    for (event in parsed) {
        val key = DuplicateKey(
            event.title,
            event.hebrewDay,
            event.hebrewMonth,
            event.hebrewYear
        )
        if (key in knownKeys) {
            skipped++
        } else {
            eventsToImport.add(event)
            knownKeys.add(key)
        }
    }

    return EventImportPlan(events = eventsToImport, skipped = skipped)
}

internal fun detectCsv(uri: Uri, content: String): Boolean {
    val path = uri.lastPathSegment?.lowercase() ?: ""
    if (path.endsWith(".json")) return false
    if (path.endsWith(".csv")) return true
    return !content.trimStart().startsWith("[")
}

private data class DuplicateKey(
    val title: String,
    val hebrewDay: Int,
    val hebrewMonth: Int,
    val hebrewYear: Int
)
