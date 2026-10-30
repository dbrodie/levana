package ninja.doskey.app.levana.ui.agenda

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import ninja.doskey.app.levana.ui.calendar.CalendarHeaderFormatter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** A month of the agenda's leading calendar, which groups its days under a header. */
sealed interface AgendaMonth {
    val first: LocalDate
    val last: LocalDate

    data class Hebrew(val month: HebrewYearMonth) : AgendaMonth {
        override val first: LocalDate get() = JewishDate(month.year, month.jewishDateMonth, 1).localDate
        override val last: LocalDate get() = first.plusDays(month.daysInMonth() - 1L)
    }

    data class Gregorian(val month: YearMonth) : AgendaMonth {
        override val first: LocalDate get() = month.atDay(1)
        override val last: LocalDate get() = month.atEndOfMonth()
    }

    companion object {
        fun of(date: LocalDate, hebrewLeading: Boolean): AgendaMonth =
            if (hebrewLeading) Hebrew(HebrewYearMonth.from(date)) else Gregorian(YearMonth.from(date))
    }
}

/**
 * The agenda's header texts. Like [CalendarHeaderFormatter] they depend on the UI locale,
 * so they are formatted at composition time.
 */
object AgendaFormatter {

    /** The month's title in its own calendar, as the Month screen titles it. */
    fun title(month: AgendaMonth, locale: Locale): String = when (month) {
        is AgendaMonth.Hebrew -> CalendarHeaderFormatter.hebrewMonthTitle(month.month, locale)
        is AgendaMonth.Gregorian ->
            month.month.month.getDisplayName(TextStyle.FULL, locale) + " " + month.month.year
    }

    /**
     * The month's span in the other calendar: "October – November 2026" (or with [short]
     * month names, "Oct – Nov 2026") for a Hebrew month, "Cheshvan–Kislev 5787" for a
     * Gregorian one.
     */
    fun otherCalendarSpan(month: AgendaMonth, locale: Locale, short: Boolean = false): String = when (month) {
        is AgendaMonth.Hebrew -> gregorianSpan(month.first, month.last, locale, short)
        is AgendaMonth.Gregorian -> {
            val first = JewishDate(month.first)
            val last = JewishDate(month.last)
            CalendarHeaderFormatter.hebrewMonthRange(
                first.jewishYear, HebrewMonth.from(first.jewishMonth, first.isJewishLeapYear),
                last.jewishYear, HebrewMonth.from(last.jewishMonth, last.isJewishLeapYear),
                locale
            )
        }
    }

    private fun gregorianSpan(first: LocalDate, last: LocalDate, locale: Locale, short: Boolean): String {
        val month = DateTimeFormatter.ofPattern(if (short) "LLL" else "LLLL", locale)
        val monthYear = DateTimeFormatter.ofPattern(if (short) "LLL yyyy" else "LLLL yyyy", locale)
        return when {
            YearMonth.from(first) == YearMonth.from(last) -> first.format(monthYear)
            first.year == last.year -> "${first.format(month)} – ${last.format(monthYear)}"
            else -> "${first.format(monthYear)} – ${last.format(monthYear)}"
        }
    }

    /** The weekday above a day's date, e.g. "FRI" / "ו׳"-style short name in the locale. */
    fun weekday(date: LocalDate, locale: Locale): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)

    /** The small Gregorian date under a Hebrew-led day, e.g. "30 Oct". */
    fun shortGregorian(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", locale))
}
