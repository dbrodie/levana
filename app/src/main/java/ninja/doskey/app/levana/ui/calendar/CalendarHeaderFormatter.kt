package ninja.doskey.app.levana.ui.calendar

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HebrewMonth
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
        return hebrewMonthRange(firstDay.year, firstDay.month, lastDay.year, lastDay.month, locale)
    }

    /** The Hebrew month span from [firstMonth] of [firstYear] to [lastMonth] of [lastYear]; see above. */
    fun hebrewMonthRange(
        firstYear: Int,
        firstMonth: HebrewMonth,
        lastYear: Int,
        lastMonth: HebrewMonth,
        locale: Locale
    ): String {
        val hebrew = isHebrew(locale)

        fun monthName(year: Int, month: HebrewMonth): String = if (hebrew) {
            hebrewMonthName(year, month.jewishDateValue)
        } else {
            month.englishName
        }

        val firstMonthName = monthName(firstYear, firstMonth)
        val firstYearLabel = yearLabel(firstYear, locale)

        return if (firstYear == lastYear && firstMonth == lastMonth) {
            "$firstMonthName $firstYearLabel"
        } else {
            val lastMonthName = monthName(lastYear, lastMonth)
            if (firstYear == lastYear) {
                "$firstMonthName–$lastMonthName $firstYearLabel"
            } else {
                "$firstMonthName $firstYearLabel – $lastMonthName ${yearLabel(lastYear, locale)}"
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
