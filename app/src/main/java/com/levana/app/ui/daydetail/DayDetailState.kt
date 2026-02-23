package com.levana.app.ui.daydetail

import com.levana.app.domain.model.CalendarEvent
import com.levana.app.domain.model.DayInfo

data class DayDetailState(
    val dayInfo: DayInfo? = null,
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val isLoading: Boolean = true
)
