package ninja.doskey.app.levana.ui

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_12H = DateTimeFormatter.ofPattern("h:mm a")
private val TIME_24H = DateTimeFormatter.ofPattern("HH:mm")

private const val FIRST_STRONG_ISOLATE = '\u2068'
private const val POP_DIRECTIONAL_ISOLATE = '\u2069'

/**
 * A clock time as "5:22 PM" or "17:22", for every screen, widget and notification. Each
 * caller decides [use24Hour]. The 12-hour AM/PM marker is in [markerLocale] when given,
 * else the default locale.
 *
 * In the Hebrew UI the time is wrapped in a first-strong isolate (U+2068 … U+2069), so the
 * bidi algorithm doesn't reorder "5:22 PM" into "PM 5:22" inside right-to-left text. A
 * first-strong isolate reads "5:22 PM" and "17:22" left to right, and a Hebrew marker
 * ("5:22 אחה״צ") right to left, as each should.
 */
fun formatClockTime(time: LocalTime, use24Hour: Boolean, hebrewUi: Boolean, markerLocale: Locale? = null): String {
    val formatter = when {
        use24Hour -> TIME_24H
        markerLocale != null -> TIME_12H.withLocale(markerLocale)
        else -> TIME_12H
    }
    val text = time.format(formatter)
    return if (hebrewUi) "$FIRST_STRONG_ISOLATE$text$POP_DIRECTIONAL_ISOLATE" else text
}
