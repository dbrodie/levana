package com.levana.app.ui.calendar

import com.levana.app.domain.model.HebrewDay

data class CalendarState(
    val hebrewDay: HebrewDay? = null,
    val isLoading: Boolean = true
)
