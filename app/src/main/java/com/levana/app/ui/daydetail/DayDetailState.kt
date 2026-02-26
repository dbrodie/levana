package com.levana.app.ui.daydetail

import com.levana.app.domain.model.CalendarEvent
import com.levana.app.domain.model.DayInfo
import com.levana.app.domain.model.SystemCalendarEvent

data class DayDetailState(
    val dayInfo: DayInfo? = null,
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val systemEvents: List<SystemCalendarEvent> = emptyList(),
    val isLoading: Boolean = true
)
