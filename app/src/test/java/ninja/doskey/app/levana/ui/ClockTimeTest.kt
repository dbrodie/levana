package ninja.doskey.app.levana.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

/** The shared clock-time format in English and Hebrew, 12- and 24-hour (TASK-57.26). */
class ClockTimeTest {

    private val evening = LocalTime.of(17, 22)
    private val morning = LocalTime.of(6, 0)

    @Test
    fun englishIsPlain() {
        assertEquals("5:22 PM", formatClockTime(evening, use24Hour = false, hebrewUi = false, Locale.ENGLISH))
        assertEquals("6:00 AM", formatClockTime(morning, use24Hour = false, hebrewUi = false, Locale.ENGLISH))
        assertEquals("17:22", formatClockTime(evening, use24Hour = true, hebrewUi = false))
        assertEquals("06:00", formatClockTime(morning, use24Hour = true, hebrewUi = false))
    }

    @Test
    fun hebrewIsIsolated() {
        assertEquals("\u20685:22 PM\u2069", formatClockTime(evening, use24Hour = false, hebrewUi = true, Locale.ENGLISH))
        assertEquals("\u20686:00 AM\u2069", formatClockTime(morning, use24Hour = false, hebrewUi = true, Locale.ENGLISH))
        assertEquals("\u206817:22\u2069", formatClockTime(evening, use24Hour = true, hebrewUi = true))
        assertEquals("\u206806:00\u2069", formatClockTime(morning, use24Hour = true, hebrewUi = true))
    }

    @Test
    fun markerFollowsMarkerLocale() {
        val hebrewMarker = formatClockTime(evening, use24Hour = false, hebrewUi = true, Locale.forLanguageTag("he"))
        assertEquals("\u20685:22 ${hebrewPm()}\u2069", hebrewMarker)
        // 24-hour ignores the marker locale
        assertEquals("17:22", formatClockTime(evening, use24Hour = true, hebrewUi = false, Locale.forLanguageTag("he")))
    }

    private fun hebrewPm(): String =
        java.time.format.DateTimeFormatter.ofPattern("a", Locale.forLanguageTag("he")).format(evening)
}
