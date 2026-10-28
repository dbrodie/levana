package ninja.doskey.app.levana.ui

import android.content.Context
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.notifications.isHebrewLanguage

/*
 * The Omer count in [Context]'s language, shared by the day panel's Omer card and the Omer
 * notifications. Pass a context carrying the app language.
 */

private val omerFormatter = HebrewDateFormatter().apply { isHebrewFormat = true }

/**
 * The day of the Omer, 1–49: its number in English, and in Hebrew the traditional form from
 * KosherJava (e.g. "ח׳ בעומר", "ל״ג בעומר").
 */
fun omerDayLabel(context: Context, omerDay: Int): String =
    if (context.isHebrewLanguage()) hebrewOmerDay(omerDay) else omerDay.toString()

/** The weeks and days that [omerDay] makes up, e.g. "1 week and 1 day"; null before day 7. */
fun omerWeeksText(context: Context, omerDay: Int): String? {
    val weeks = omerDay / 7
    val remaining = omerDay % 7
    val res = context.resources
    return when {
        weeks == 0 -> null
        remaining == 0 -> res.getQuantityString(R.plurals.omer_weeks, weeks, weeks)
        else -> context.getString(
            R.string.omer_weeks_and_days,
            res.getQuantityString(R.plurals.omer_weeks, weeks, weeks),
            res.getQuantityString(R.plurals.omer_and_days, remaining, remaining)
        )
    }
}

/** The day panel's Omer count, e.g. "Day 8, which is 1 week and 1 day of the Omer". */
fun omerCardText(context: Context, omerDay: Int): String {
    val day = omerDayLabel(context, omerDay)
    val weeks = omerWeeksText(context, omerDay)
        ?: return context.getString(R.string.omer_card_day, day)
    return context.getString(R.string.omer_card_day_with_weeks, day, weeks)
}

/** KosherJava's Hebrew Omer day, e.g. "ח׳ בעומר", via a calendar date on that day. */
private fun hebrewOmerDay(omerDay: Int): String {
    // 16 Nisan is day 1. Nisan always has 30 days and Iyar 29, so any year works
    val jc = when {
        omerDay <= 15 -> JewishCalendar(5786, JewishCalendar.NISSAN, 15 + omerDay)
        omerDay <= 44 -> JewishCalendar(5786, JewishCalendar.IYAR, omerDay - 15)
        else -> JewishCalendar(5786, JewishCalendar.SIVAN, omerDay - 44)
    }
    return omerFormatter.formatOmer(jc)
}
