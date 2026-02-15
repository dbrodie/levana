package com.levana.app.domain.model

import java.time.LocalDate

data class HebrewDay(
    val day: Int,
    val month: HebrewMonth,
    val year: Int,
    val hebrewFormatted: String,
    val transliterated: String,
    val hebrewDayOfMonthFormatted: String,
    val gregorianDate: LocalDate,
    val hasCandles: Boolean = false,
    val holidayCategory: HolidayCategory? = null,
    val hasPersonalEvent: Boolean = false
)
