package ninja.doskey.app.levana.domain.model

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HebrewMonthTest {

    @Test
    fun adarNamesDependOnLeapYear() {
        assertEquals("Adar", HebrewMonth.englishName(JewishDate.ADAR, isLeapYear = false))
        assertEquals("Adar I", HebrewMonth.englishName(JewishDate.ADAR, isLeapYear = true))
        assertEquals("Adar II", HebrewMonth.englishName(JewishDate.ADAR_II, isLeapYear = true))
    }

    @Test
    fun namesAreCapitalisedWithoutUnderscores() {
        for (month in HebrewMonth.entries) {
            val name = month.englishName
            assertTrue(name, name.first().isUpperCase())
            assertTrue(name, !name.contains('_'))
            assertTrue(
                name,
                name.split(" ").all { word ->
                    word == "I" || word == "II" || word.drop(1).all { it.isLowerCase() }
                }
            )
        }
    }

    /**
     * The quick tile and HebrewDay.transliterated get whole English dates from
     * KosherJava; the shared names must be the same ones.
     */
    @Test
    fun namesMatchKosherJavaTransliteration() {
        val formatter = HebrewDateFormatter().apply { isHebrewFormat = false }
        // 5786 is a regular year, 5787 a leap year
        for (year in listOf(5786, 5787)) {
            val isLeap = JewishDate(year, JewishDate.TISHREI, 1).isJewishLeapYear
            val lastMonth = if (isLeap) JewishDate.ADAR_II else JewishDate.ADAR
            for (month in JewishDate.NISSAN..lastMonth) {
                assertEquals(
                    formatter.formatMonth(JewishDate(year, month, 1)),
                    HebrewMonth.englishName(month, isLeap)
                )
            }
        }
    }
}
