package ninja.doskey.app.levana.ui.calendarselection

import ninja.doskey.app.levana.domain.model.DeviceCalendar

data class CalendarSelectionState(
    val calendars: List<DeviceCalendar> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val hasPermission: Boolean = false,
    val isLoading: Boolean = true
)
