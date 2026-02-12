package com.levana.app.data

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.levana.app.domain.model.HebrewDay
import com.levana.app.domain.model.HebrewMonth
import java.time.LocalDate
import java.util.GregorianCalendar

class CalendarRepository {

    private val hebrewFormatter = HebrewDateFormatter().apply {
        isHebrewFormat = true
    }

    private val translitFormatter = HebrewDateFormatter().apply {
        isHebrewFormat = false
    }

    fun getHebrewDay(date: LocalDate): HebrewDay {
        // GregorianCalendar months are 0-based
        val gregorianCalendar = GregorianCalendar(
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        )
        val jewishCalendar = JewishCalendar(gregorianCalendar)

        val hebrewFormatted = hebrewFormatter.format(jewishCalendar)
        val transliterated = translitFormatter.format(jewishCalendar)

        return HebrewDay(
            day = jewishCalendar.jewishDayOfMonth,
            month = HebrewMonth.from(
                jewishCalendar.jewishMonth,
                jewishCalendar.isJewishLeapYear
            ),
            year = jewishCalendar.jewishYear,
            hebrewFormatted = hebrewFormatted,
            transliterated = transliterated,
            gregorianDate = date
        )
    }
}
