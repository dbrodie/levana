package ninja.doskey.app.levana.ui.datepicker

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import ninja.doskey.app.levana.ui.calendar.CalendarHeaderFormatter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * The picker's text. Hebrew dates are written in Hebrew letters in both UI languages, as in
 * design 1b; Gregorian dates follow the UI locale. Done at composition time with the
 * current configuration's locale, like [CalendarHeaderFormatter].
 */
object DatePickerFormatter {

    private val hebrewFormatter = HebrewDateFormatter().apply {
        isHebrewFormat = true
        isUseLongHebrewYears = true
    }

    /**
     * A Hebrew year in letters, e.g. "ה׳תשפ״ז". KosherJava writes "ה׳ תשפ״ז"; the space is
     * kept only in a whole thousand, "ו׳ אלפים".
     */
    fun hebrewYear(year: Int): String = hebrewFormatter.formatHebrewNumber(year).let {
        if (year % 1000 == 0) it else it.replaceFirst("׳ ", "׳")
    }

    fun hebrewMonthName(year: Int, jewishMonth: Int): String =
        hebrewFormatter.formatMonth(JewishDate(year, jewishMonth, 1))

    /** The Hebrew-led title, e.g. "חשון ה׳תשפ״ז" or "אדר ב׳ ה׳תשפ״ז". */
    fun hebrewMonthTitle(month: HebrewYearMonth): String =
        "${hebrewMonthName(month.year, month.jewishDateMonth)} ${hebrewYear(month.year)}"

    /** The Gregorian-led title, e.g. "October 2026". */
    fun gregorianMonthTitle(month: YearMonth, locale: Locale): String =
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))

    /** The Gregorian months a Hebrew month spans, e.g. "Oct – Nov 2026" or "Dec 2026 – Jan 2027". */
    fun gregorianSpan(days: List<HebrewDay>, locale: Locale): String {
        if (days.isEmpty()) return ""
        val first = days.first().gregorianDate
        val last = days.last().gregorianDate
        val monthYear = DateTimeFormatter.ofPattern("MMM yyyy", locale)
        return when {
            YearMonth.from(first) == YearMonth.from(last) -> first.format(monthYear)
            first.year == last.year ->
                "${first.format(DateTimeFormatter.ofPattern("MMM", locale))} – ${last.format(monthYear)}"
            else -> "${first.format(monthYear)} – ${last.format(monthYear)}"
        }
    }

    /** The Hebrew months a Gregorian month spans, e.g. "Tishrei–Cheshvan 5787". */
    fun hebrewSpan(days: List<HebrewDay>, locale: Locale): String =
        CalendarHeaderFormatter.hebrewMonthRange(days, locale)

    /** The selection's Hebrew date, e.g. "י״ח חשון ה׳תשפ״ז". */
    fun hebrewDate(date: LocalDate): String {
        val jd = JewishDate(date)
        return "${hebrewFormatter.formatHebrewNumber(jd.jewishDayOfMonth)} " +
            "${hebrewFormatter.formatMonth(jd)} ${hebrewYear(jd.jewishYear)}"
    }

    /** The selection's Gregorian date, e.g. "Thu, 29 Oct 2026". */
    fun gregorianDate(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale))

    /**
     * The small Gregorian line of a Hebrew-led cell: the day, with the month on the 1st of
     * a Gregorian month and on the month's first cell, e.g. "12 Oct", "13", "1 Nov".
     */
    fun gregorianCellLabel(day: HebrewDay, firstCell: Boolean, locale: Locale): String {
        val date = day.gregorianDate
        return if (firstCell || date.dayOfMonth == 1) {
            date.format(DateTimeFormatter.ofPattern("d MMM", locale))
        } else {
            date.dayOfMonth.toString()
        }
    }

    /** The small Hebrew line of a Gregorian-led cell: the day in letters, or the month name on day 1. */
    fun hebrewCellLabel(day: HebrewDay): String =
        if (day.day == 1) hebrewMonthName(day.year, day.month.jewishDateValue) else day.hebrewDayOfMonthFormatted

    /** The holiday's name in the UI language, if [day] has one. */
    fun holidayName(day: HebrewDay, locale: Locale): String? =
        if (CalendarHeaderFormatter.isHebrew(locale)) day.holidayLabelHebrew else day.holidayLabel

    /**
     * What a screen reader says for a day cell: both dates and the holiday, then [today]
     * (the localized word) when it is today. The selected state is announced separately.
     */
    fun dayDescription(day: HebrewDay, locale: Locale, today: String?): String {
        val hebrew = if (CalendarHeaderFormatter.isHebrew(locale)) day.hebrewFormatted else day.transliterated
        val gregorian = day.gregorianDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
        return listOfNotNull(gregorian, hebrew, holidayName(day, locale), today).joinToString(", ")
    }
}
