package com.levana.app.data

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.levana.app.domain.model.HebrewDay
import com.levana.app.domain.model.HebrewMonth
import java.time.LocalDate
import java.time.YearMonth
import java.util.GregorianCalendar

class CalendarRepository {

    private val hebrewFormatter = HebrewDateFormatter().apply {
        isHebrewFormat = true
    }

    private val translitFormatter = HebrewDateFormatter().apply {
        isHebrewFormat = false
    }

    fun getHebrewDay(date: LocalDate): HebrewDay {
        val jewishCalendar = createJewishCalendar(date)

        return toHebrewDay(jewishCalendar, date)
    }

    fun getMonthDays(yearMonth: YearMonth): List<HebrewDay> {
        return (1..yearMonth.lengthOfMonth()).map { dayOfMonth ->
            val date = yearMonth.atDay(dayOfMonth)
            getHebrewDay(date)
        }
    }

    fun getHebrewMonthName(date: LocalDate): String {
        val jewishCalendar = createJewishCalendar(date)
        return translitFormatter.formatMonth(jewishCalendar)
    }

    fun getHebrewMonthNameHebrew(date: LocalDate): String {
        val jewishCalendar = createJewishCalendar(date)
        return hebrewFormatter.formatMonth(jewishCalendar)
    }

    private fun createJewishCalendar(date: LocalDate): JewishCalendar {
        // GregorianCalendar months are 0-based
        val gregorianCalendar = GregorianCalendar(
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        )
        return JewishCalendar(gregorianCalendar)
    }

    private fun toHebrewDay(jewishCalendar: JewishCalendar, date: LocalDate): HebrewDay {
        return HebrewDay(
            day = jewishCalendar.jewishDayOfMonth,
            month = HebrewMonth.from(
                jewishCalendar.jewishMonth,
                jewishCalendar.isJewishLeapYear
            ),
            year = jewishCalendar.jewishYear,
            hebrewFormatted = hebrewFormatter.format(jewishCalendar),
            transliterated = translitFormatter.format(jewishCalendar),
            hebrewDayOfMonthFormatted = hebrewFormatter.formatHebrewNumber(
                jewishCalendar.jewishDayOfMonth
            ),
            gregorianDate = date
        )
    }
}
