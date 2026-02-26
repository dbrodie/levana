package com.levana.app.domain.model

import java.time.LocalDateTime

data class SystemCalendarEvent(
    val eventId: Long,
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val allDay: Boolean,
    val calendarId: Long,
    val calendarColor: Int
)
