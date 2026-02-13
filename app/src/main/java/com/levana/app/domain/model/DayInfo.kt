package com.levana.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

data class DayInfo(
    val hebrewDay: HebrewDay,
    val dayOfWeek: DayOfWeek,
    val gregorianDate: LocalDate,
    val holidays: List<Holiday>,
    val parsha: String?,
    val parshaHebrew: String? = null,
    val specialShabbat: String? = null,
    val molad: String? = null,
    val omerDay: Int?,
    val shabbatInfo: ShabbatInfo? = null
) {
    val omerFormatted: String?
        get() {
            val day = omerDay ?: return null
            val weeks = day / 7
            val remaining = day % 7
            return if (weeks == 0) {
                "Day $day of the Omer"
            } else if (remaining == 0) {
                "Day $day, which is $weeks weeks of the Omer"
            } else {
                "Day $day, which is $weeks weeks and $remaining days of the Omer"
            }
        }
}
