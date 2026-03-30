package com.levana.app.domain.model

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import java.util.GregorianCalendar

data class HebrewYearMonth(
    val year: Int,
    val month: HebrewMonth,
    val jewishDateMonth: Int
) {
    private fun isLeapYear(): Boolean {
        return JewishDate(year, JewishDate.TISHREI, 1).isJewishLeapYear
    }

    fun daysInMonth(): Int {
        return JewishDate(year, jewishDateMonth, 1).daysInJewishMonth
    }

    fun next(): HebrewYearMonth {
        val isLeap = isLeapYear()
        return when {
            // Elul -> next year Tishrei
            jewishDateMonth == JewishDate.ELUL -> HebrewYearMonth(
                year + 1,
                HebrewMonth.TISHREI,
                JewishDate.TISHREI
            )
            // Adar in leap year (Adar I) -> Adar II
            jewishDateMonth == JewishDate.ADAR && isLeap -> HebrewYearMonth(
                year,
                HebrewMonth.ADAR_II,
                JewishDate.ADAR_II
            )
            // Adar II -> Nissan
            jewishDateMonth == JewishDate.ADAR_II -> HebrewYearMonth(
                year,
                HebrewMonth.NISSAN,
                JewishDate.NISSAN
            )
            // Adar in non-leap year -> Nissan
            jewishDateMonth == JewishDate.ADAR && !isLeap -> HebrewYearMonth(
                year,
                HebrewMonth.NISSAN,
                JewishDate.NISSAN
            )
            else -> {
                val nextJdMonth = jewishDateMonth + 1
                HebrewYearMonth(
                    year,
                    HebrewMonth.from(nextJdMonth, isLeap),
                    nextJdMonth
                )
            }
        }
    }

    fun previous(): HebrewYearMonth {
        val isLeap = isLeapYear()
        return when {
            // Tishrei -> previous year Elul
            jewishDateMonth == JewishDate.TISHREI -> {
                val prevYear = year - 1
                HebrewYearMonth(
                    prevYear,
                    HebrewMonth.ELUL,
                    JewishDate.ELUL
                )
            }
            // Nissan -> Adar II if leap, Adar if not
            jewishDateMonth == JewishDate.NISSAN -> {
                if (isLeap) {
                    HebrewYearMonth(
                        year,
                        HebrewMonth.ADAR_II,
                        JewishDate.ADAR_II
                    )
                } else {
                    HebrewYearMonth(
                        year,
                        HebrewMonth.ADAR,
                        JewishDate.ADAR
                    )
                }
            }
            // Adar II -> Adar I
            jewishDateMonth == JewishDate.ADAR_II -> HebrewYearMonth(
                year,
                HebrewMonth.ADAR_I,
                JewishDate.ADAR
            )
            else -> {
                val prevJdMonth = jewishDateMonth - 1
                HebrewYearMonth(
                    year,
                    HebrewMonth.from(prevJdMonth, isLeap),
                    prevJdMonth
                )
            }
        }
    }

    companion object {
        fun now(): HebrewYearMonth {
            val jd = JewishDate()
            val isLeap = jd.isJewishLeapYear
            return HebrewYearMonth(
                jd.jewishYear,
                HebrewMonth.from(jd.jewishMonth, isLeap),
                jd.jewishMonth
            )
        }

        fun from(date: LocalDate): HebrewYearMonth {
            val gc = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
            val jd = JewishDate(gc)
            val isLeap = jd.isJewishLeapYear
            return HebrewYearMonth(
                jd.jewishYear,
                HebrewMonth.from(jd.jewishMonth, isLeap),
                jd.jewishMonth
            )
        }
    }
}
