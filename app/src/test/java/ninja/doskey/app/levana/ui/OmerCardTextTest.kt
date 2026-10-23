package ninja.doskey.app.levana.ui

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import ninja.doskey.app.levana.notifications.withAppLocale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The day panel's Omer card text in the app language (levana-3kn). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class OmerCardTextTest {

    private val app: Context get() = RuntimeEnvironment.getApplication()
    private val localeManager get() = app.getSystemService(LocaleManager::class.java)

    private fun language(tag: String): Context {
        localeManager.applicationLocales = LocaleList.forLanguageTags(tag)
        return app.withAppLocale()
    }

    @After
    fun resetAppLanguage() {
        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
    }

    @Test
    fun englishBoundaries() {
        val en = language("en-US")
        val expected = mapOf(
            1 to "Day 1 of the Omer",
            6 to "Day 6 of the Omer",
            7 to "Day 7, which is 1 week of the Omer",
            8 to "Day 8, which is 1 week and 1 day of the Omer",
            9 to "Day 9, which is 1 week and 2 days of the Omer",
            13 to "Day 13, which is 1 week and 6 days of the Omer",
            14 to "Day 14, which is 2 weeks of the Omer",
            15 to "Day 15, which is 2 weeks and 1 day of the Omer",
            21 to "Day 21, which is 3 weeks of the Omer",
            33 to "Day 33, which is 4 weeks and 5 days of the Omer",
            48 to "Day 48, which is 6 weeks and 6 days of the Omer",
            49 to "Day 49, which is 7 weeks of the Omer"
        )
        expected.forEach { (day, text) -> assertEquals(text, omerCardText(en, day)) }
    }

    @Test
    fun hebrewBoundaries() {
        val he = language("iw")
        val expected = mapOf(
            1 to "א׳ בעומר",
            6 to "ו׳ בעומר",
            7 to "ז׳ בעומר, שהם שבוע אחד",
            8 to "ח׳ בעומר, שהם שבוע אחד ויום אחד",
            9 to "ט׳ בעומר, שהם שבוע אחד ושני ימים",
            13 to "י״ג בעומר, שהם שבוע אחד ו־6 ימים",
            14 to "י״ד בעומר, שהם שני שבועות",
            15 to "ט״ו בעומר, שהם שני שבועות ויום אחד",
            21 to "כ״א בעומר, שהם 3 שבועות",
            33 to "ל״ג בעומר, שהם 4 שבועות ו־5 ימים",
            48 to "מ״ח בעומר, שהם 6 שבועות ו־6 ימים",
            49 to "מ״ט בעומר, שהם 7 שבועות"
        )
        expected.forEach { (day, text) -> assertEquals(text, omerCardText(he, day)) }
    }

    @Test
    fun everyDayInBothLanguages() {
        val en = language("en-US")
        val english = (1..49).map { omerCardText(en, it) }
        val he = language("iw")
        val hebrew = (1..49).map { omerCardText(he, it) }
        for (day in 1..49) {
            val e = english[day - 1]
            assertTrue(e, e.startsWith("Day $day") && e.endsWith("of the Omer"))
            assertEquals(e, day >= 7, e.contains("week"))
            assertEquals(e, day % 7 != 0 && day > 7, e.contains(" and "))
            assertFalse(e, Regex("\\b1 (weeks|days)\\b").containsMatchIn(e))

            val h = hebrew[day - 1]
            val dayLabel = h.substringBefore(',')
            assertTrue(h, dayLabel.endsWith(" בעומר"))
            assertFalse(h, dayLabel.any { it in 'A'..'z' || it in '0'..'9' })
            assertEquals(h, day >= 7, h.contains(", שהם "))
        }
        // Each day reads differently
        assertEquals(49, english.toSet().size)
        assertEquals(49, hebrew.toSet().size)
    }

    @Test
    fun followsTheAppLanguage() {
        assertEquals("Day 8, which is 1 week and 1 day of the Omer", omerCardText(language("en-US"), 8))
        assertEquals("ח׳ בעומר, שהם שבוע אחד ויום אחד", omerCardText(language("iw"), 8))
        assertEquals("Day 8, which is 1 week and 1 day of the Omer", omerCardText(language("en-US"), 8))
    }
}
