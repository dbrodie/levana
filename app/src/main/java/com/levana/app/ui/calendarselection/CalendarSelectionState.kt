package com.levana.app.ui.calendarselection

import com.levana.app.domain.model.DeviceCalendar

data class CalendarSelectionState(
    val calendars: List<DeviceCalendar> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val hasPermission: Boolean = false,
    val isLoading: Boolean = true
)
