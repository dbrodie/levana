package ninja.doskey.app.levana.ui

import android.content.Context
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.Molad
import ninja.doskey.app.levana.notifications.isHebrewLanguage
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val TIME_12H = DateTimeFormatter.ofPattern("h:mm a")
private val TIME_24H = DateTimeFormatter.ofPattern("HH:mm")

/** Left-to-right isolate, so "10:27 PM" keeps its order inside Hebrew text. */
private fun ltr(text: String) = "\u2066$text\u2069"

/**
 * The molad's weekday, time and chalakim in [context]'s language, e.g. "Monday 10:27 PM and
 * 3 chalakim" / "יום שני 22:27 ו־3 חלקים". The time uses the screens' 12/24-hour formats.
 */
fun moladTimeText(context: Context, molad: Molad, use24Hour: Boolean): String {
    val weekday = molad.dayOfWeek.getDisplayName(TextStyle.FULL, context.resources.configuration.locales[0])
    val clock = LocalTime.of(molad.hour, molad.minute).format(if (use24Hour) TIME_24H else TIME_12H)
    val time = if (context.isHebrewLanguage()) ltr(clock) else clock
    return if (molad.chalakim == 0) {
        context.getString(R.string.molad_time, weekday, time)
    } else {
        context.resources.getQuantityString(R.plurals.molad_time_chalakim, molad.chalakim, weekday, time, molad.chalakim)
    }
}

/** The blessed month's name in [context]'s language, e.g. "Kislev" / "כסלו". */
fun moladMonthName(context: Context, molad: Molad): String =
    if (context.isHebrewLanguage()) molad.month.hebrewName else molad.month.englishName

/** The Agenda's Shabbat Mevarchim line: "Mevarchim Kislev · Molad Monday 10:27 PM and 3 chalakim". */
fun agendaMevarchimText(context: Context, molad: Molad, use24Hour: Boolean): String =
    context.getString(R.string.agenda_mevarchim, moladMonthName(context, molad), moladTimeText(context, molad, use24Hour))
