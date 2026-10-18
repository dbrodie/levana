package ninja.doskey.app.levana.ui.events

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HebrewDateFormatTest {

    private val unknownYears = listOf(0, 1, -1, 3761, 10000, Int.MAX_VALUE, Int.MIN_VALUE)

    @Test
    fun anyUnknownYearGivesTheYearlessHebrewDate() {
        for (year in unknownYears) {
            assertEquals("year $year", "י׳ כסלו", formatHebrewDate(10, JewishDate.KISLEV, year))
        }
    }

    @Test
    fun unknownYearNeverShowsRawNumbers() {
        for (year in unknownYears) {
            for (month in JewishDate.NISSAN..JewishDate.ADAR_II) {
                for (day in listOf(1, 15, 29, 30)) {
                    val text = formatHebrewDate(day, month, year)
                    assertFalse("$day/$month/$year -> $text", text.any { it.isDigit() || it == '/' })
                }
            }
        }
    }

    @Test
    fun yearlessAdarIsPlainAdarAndAdarIIStaysAdarII() {
        assertEquals("י״ד אדר", formatHebrewDate(14, JewishDate.ADAR, 0))
        assertEquals("י״ד אדר ב׳", formatHebrewDate(14, JewishDate.ADAR_II, 0))
    }

    @Test
    fun knownYearOutputIsUnchanged() {
        val formatter = HebrewDateFormatter().apply { isHebrewFormat = true }
        for (year in listOf(3762, 5784, 5785, 5787, 9999)) {
            val lastMonth = if (JewishDate(year, JewishDate.TISHREI, 1).isJewishLeapYear) 13 else 12
            for (month in JewishDate.NISSAN..lastMonth) {
                val jd = JewishDate(year, month, 1)
                assertEquals(formatter.format(jd), formatHebrewDate(1, month, year))
            }
        }
        assertEquals("י״ד אדר א׳ תשפ״ד", formatHebrewDate(14, JewishDate.ADAR, 5784))
        assertEquals("י״ד אדר תשפ״ה", formatHebrewDate(14, JewishDate.ADAR, 5785))
    }

    @Test
    fun knownYearWithADayOrMonthThatYearLacksIsStillReadable() {
        // Adar II recorded against a regular year
        assertEquals("י״ד אדר ב׳ תשפ״ה", formatHebrewDate(14, JewishDate.ADAR_II, 5785))
        // 30 Cheshvan in a year where Cheshvan has 29 days
        val shortCheshvanYear = (5780..5800).first {
            JewishDate(it, JewishDate.CHESHVAN, 1).daysInJewishMonth == 29
        }
        val text = formatHebrewDate(30, JewishDate.CHESHVAN, shortCheshvanYear)
        assertFalse(text, text.any { it.isDigit() || it == '/' })
    }

    @Test
    fun garbageDayOrMonthFallsBackWithoutThrowing() {
        assertEquals("31/9", formatHebrewDate(31, JewishDate.KISLEV, 0))
        assertEquals("10/14", formatHebrewDate(10, 14, 0))
        assertEquals("10/14/5785", formatHebrewDate(10, 14, 5785))
    }
}
