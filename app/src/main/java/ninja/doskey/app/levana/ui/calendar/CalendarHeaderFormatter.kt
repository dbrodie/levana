package ninja.doskey.app.levana.ui.calendar

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Builds the month header strings shown above the calendar grid.
 *
 * Formatting depends on the UI locale, so it is done at composition time with the
 * current configuration locale rather than cached in the ViewModel (which survives
 * per-app language changes).
 */
object CalendarHeaderFormatter {

    private val hebrewFormatter = HebrewDateFormatter().apply { isHebrewFormat = true }

    fun isHebrew(locale: Locale): Boolean = locale.language in setOf("he", "iw")

    private fun hebrewMonthName(year: Int, jewishDateMonth: Int): String =
        hebrewFormatter.formatMonth(JewishDate(year, jewishDateMonth, 1))

    private fun yearLabel(year: Int, locale: Locale): String =
        if (isHebrew(locale)) hebrewFormatter.formatHebrewNumber(year) else year.toString()

    /** Title for the Hebrew-calendar mode, e.g. "תשרי 5787" (English) / "תשרי תשפ״ז" (Hebrew). */
    fun hebrewMonthTitle(hym: HebrewYearMonth?, locale: Locale): String {
        if (hym == null) return ""
        return "${hebrewMonthName(hym.year, hym.jewishDateMonth)} ${yearLabel(hym.year, locale)}"
    }

    /**
     * Hebrew month span of a Gregorian month, e.g. "Tishrei–Cheshvan 5787" or
     * "Adar I–Adar II 5787" (English) /
     * "תשרי–חשון תשפ״ז" (Hebrew).
     */
    fun hebrewMonthRange(days: List<HebrewDay>, locale: Locale): String {
        if (days.isEmpty()) return ""
        val firstDay = days.first()
        val lastDay = days.last()
        val hebrew = isHebrew(locale)

        fun monthName(day: HebrewDay): String = if (hebrew) {
            hebrewMonthName(day.year, day.month.jewishDateValue)
        } else {
            day.month.englishName
        }

        val firstMonthName = monthName(firstDay)
        val firstYear = yearLabel(firstDay.year, locale)

        return if (firstDay.month == lastDay.month) {
            "$firstMonthName $firstYear"
        } else {
            val lastMonthName = monthName(lastDay)
            if (firstDay.year == lastDay.year) {
                "$firstMonthName–$lastMonthName $firstYear"
            } else {
                "$firstMonthName $firstYear – $lastMonthName ${yearLabel(lastDay.year, locale)}"
            }
        }
    }

    /** Gregorian span of a Hebrew month, e.g. "Sep 12, 2026 – Oct 11, 2026", locale-formatted. */
    fun gregorianRange(days: List<HebrewDay>, locale: Locale): String {
        if (days.isEmpty()) return ""
        val first = days.first().gregorianDate
        val last = days.last().gregorianDate
        return if (first.month == last.month && first.year == last.year) {
            first.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        } else {
            val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            "${first.format(fmt)} – ${last.format(fmt)}"
        }
    }
}
