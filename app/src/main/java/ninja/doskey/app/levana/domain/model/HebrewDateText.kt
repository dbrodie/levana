package ninja.doskey.app.levana.domain.model

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate

/**
 * The app's only KosherJava date formatters, so Hebrew dates and numbers are written the same
 * way everywhere (TASK-57.25). Hebrew letters use geresh/gershayim and the short year form,
 * "תשפ״ז" rather than "ה׳תשפ״ז". The formatters stay private so their settings cannot drift.
 */
object HebrewDateText {

    private val hebrew = HebrewDateFormatter().apply { isHebrewFormat = true }
    private val transliterated = HebrewDateFormatter().apply { isHebrewFormat = false }

    /** [number] in Hebrew letters, e.g. "י״ח" or "תשפ״ז"; KosherJava accepts 0–9999. */
    fun hebrewNumber(number: Int): String = hebrew.formatHebrewNumber(number)

    /** A Hebrew year in letters, e.g. "תשפ״ז"; a whole thousand is "ו׳ אלפים". */
    fun hebrewYear(year: Int): String = hebrewNumber(year)

    /** The month of [date] in Hebrew, e.g. "חשון", "אדר ב׳". */
    fun hebrewMonth(date: JewishDate): String = hebrew.formatMonth(date)

    /** The month of [date] transliterated, e.g. "Cheshvan", "Adar II". */
    fun transliteratedMonth(date: JewishDate): String = transliterated.formatMonth(date)

    /** A whole date in Hebrew letters, e.g. "י״ח חשון תשפ״ז". */
    fun hebrewDate(date: JewishDate): String = hebrew.format(date)

    /** A whole date transliterated, e.g. "18 Cheshvan, 5787". */
    fun transliteratedDate(date: JewishDate): String = transliterated.format(date)

    /** A whole date in Hebrew letters when [hebrew], else transliterated. */
    fun date(date: JewishDate, hebrew: Boolean): String =
        if (hebrew) hebrewDate(date) else transliteratedDate(date)

    /** The parasha read on [calendar]'s day, in Hebrew when [hebrew], else transliterated. */
    fun parsha(calendar: JewishCalendar, hebrew: Boolean): String =
        (if (hebrew) this.hebrew else transliterated).formatParsha(calendar)

    /** The Omer count of [calendar]'s day in Hebrew, e.g. "ל״ג בעומר". */
    fun hebrewOmer(calendar: JewishCalendar): String = hebrew.formatOmer(calendar)
}
