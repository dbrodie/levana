package com.levana.app.ui.calendarselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.PreferencesRepository
import com.levana.app.data.SystemCalendarRepository
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
        viewModelScope.launch {
            val hasPermission =
                systemCalendarRepository.hasCalendarPermission()
            if (!hasPermission) {
                _state.value = CalendarSelectionState(
                    hasPermission = false,
                    isLoading = false
                )
                return@launch
            }

            val calendars = systemCalendarRepository.getDeviceCalendars()
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
