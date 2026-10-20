package ninja.doskey.app.levana.ui.calendarselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CalendarSelectionViewModel(
    private val systemCalendarRepository: SystemCalendarRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarSelectionState())
    val state: StateFlow<CalendarSelectionState> = _state.asStateFlow()

    // Only the latest Load / PermissionGranted may publish, so a slower
    // earlier query never replaces a newer calendar list or permission state
    private var loadJob: Job? = null

    init {
        onIntent(CalendarSelectionIntent.Load)
    }

    fun onIntent(intent: CalendarSelectionIntent) {
        when (intent) {
            is CalendarSelectionIntent.Load -> load()
            is CalendarSelectionIntent.ToggleCalendar ->
                toggleCalendar(intent.calendarId)
            is CalendarSelectionIntent.PermissionGranted -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val hasPermission =
                systemCalendarRepository.hasCalendarPermission()
            if (!hasPermission) {
                _state.value = CalendarSelectionState(
                    hasPermission = false,
                    isLoading = false
                )
                return@launch
            }

            val calendars = try {
                systemCalendarRepository.getDeviceCalendars()
            } catch (_: SecurityException) {
                // Permission revoked since the check
                _state.value = CalendarSelectionState(
                    hasPermission = false,
                    isLoading = false
                )
                return@launch
            }
            val prefs = preferencesRepository.preferences.first()

            _state.value = CalendarSelectionState(
                calendars = calendars,
                selectedIds = prefs.selectedCalendarIds,
                hasPermission = true,
                isLoading = false
            )
        }
    }

    private fun toggleCalendar(calendarId: Long) {
        viewModelScope.launch {
            val current = _state.value.selectedIds
            val updated = if (calendarId in current) {
                current - calendarId
            } else {
                current + calendarId
            }
            _state.value = _state.value.copy(selectedIds = updated)
            preferencesRepository.saveSelectedCalendarIds(updated)
        }
    }
}
