package com.levana.app.ui.daydetail

import com.levana.app.domain.model.CalendarEvent
import com.levana.app.domain.model.DayInfo
import com.levana.app.domain.model.SystemCalendarEvent
import com.levana.app.domain.model.ZmanTime

data class DayDetailState(
    val dayInfo: DayInfo? = null,
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val systemEvents: List<SystemCalendarEvent> = emptyList(),
    val halachicTimes: List<ZmanTime> = emptyList(),
    val isLoading: Boolean = true
)
