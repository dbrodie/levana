package ninja.doskey.app.levana.widget

import java.text.Bidi
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLocationSuffixTest {

    /** The label's characters in left-to-right screen order, in a paragraph of [direction]. */
    private fun visual(text: String, direction: Int): String {
        val bidi = Bidi(text, direction)
        val runs = (0 until bidi.runCount).map { i ->
            val run = text.substring(bidi.getRunStart(i), bidi.getRunLimit(i))
            if (bidi.getRunLevel(i) % 2 == 1) run.reversed() else run
        }
        val levels = ByteArray(runs.size) { bidi.getRunLevel(it).toByte() }
        val order: Array<Any> = runs.toTypedArray()
        Bidi.reorderVisually(levels, 0, order, 0, order.size)
        return order.joinToString("") { it as String }.filter { Character.getType(it) != Character.FORMAT.toInt() }.trim()
    }

    private val paragraphs = listOf(Bidi.DIRECTION_LEFT_TO_RIGHT, Bidi.DIRECTION_RIGHT_TO_LEFT)

    @Test
    fun `the label is the separator and the city in isolates of the UI direction`() {
        assertEquals("⁧ · ⁨Jerusalem⁩⁩", widgetLocationSuffix("Jerusalem", hebrewUi = true))
        assertEquals("⁦ · ⁨Jerusalem⁩⁩", widgetLocationSuffix("Jerusalem", hebrewUi = false))
    }

    @Test
    fun `in the Hebrew UI the separator is right of the city, next to the date, in either paragraph direction`() {
        for (paragraph in paragraphs) {
            assertEquals("Jerusalem ·", visual(widgetLocationSuffix("Jerusalem", hebrewUi = true), paragraph))
            assertEquals("םילשורי ·", visual(widgetLocationSuffix("ירושלים", hebrewUi = true), paragraph))
            assertEquals("יחכונ םוקימ ·", visual(widgetLocationSuffix("מיקום נוכחי", hebrewUi = true), paragraph))
        }
    }

    @Test
    fun `in the English UI the separator is left of the city, next to the date, in either paragraph direction`() {
        for (paragraph in paragraphs) {
            assertEquals("· Jerusalem", visual(widgetLocationSuffix("Jerusalem", hebrewUi = false), paragraph))
            assertEquals("· םילשורי", visual(widgetLocationSuffix("ירושלים", hebrewUi = false), paragraph))
        }
    }
}
