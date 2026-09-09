package ninja.doskey.app.levana.domain.model

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
    val holidayLabel: String? = null,
    val holidayLabelHebrew: String? = null,
    val parshaLabel: String? = null,
    val parshaLabelHebrew: String? = null,
    val hasPersonalEvent: Boolean = false,
    val systemEventColors: List<Int> = emptyList()
)
