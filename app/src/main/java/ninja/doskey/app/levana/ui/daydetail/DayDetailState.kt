package ninja.doskey.app.levana.ui.daydetail

import ninja.doskey.app.levana.domain.model.CalendarEvent
import ninja.doskey.app.levana.domain.model.DayInfo
import ninja.doskey.app.levana.domain.model.SystemCalendarEvent
import ninja.doskey.app.levana.domain.model.ZmanTime

data class DayDetailState(
    val dayInfo: DayInfo? = null,
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val systemEvents: List<SystemCalendarEvent> = emptyList(),
    val halachicTimes: List<ZmanTime> = emptyList(),
    val isLoading: Boolean = true
)
