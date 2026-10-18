package ninja.doskey.app.levana.ui.events

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.domain.model.HebrewMonth
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The event/birthday editors' month stepper must use the same month names as
 * the rest of the app (KosherJava's formatter via the shared HebrewMonth model).
 */
class EditorMonthNamesTest {

    private val formatter = HebrewDateFormatter().apply { isHebrewFormat = true }

    @Test
    fun ordinaryYearHebrewNames() {
        assertEquals(
            listOf("תשרי", "חשון", "כסלו", "טבת", "שבט", "אדר", "ניסן", "אייר", "סיון", "תמוז", "אב", "אלול"),
            buildMonthList(isLeapYear = false).map { it.hebrewDisplayName }
        )
    }

    @Test
    fun leapYearHebrewNames() {
        assertEquals(
            listOf(
                "תשרי", "חשון", "כסלו", "טבת", "שבט", "אדר א׳", "אדר ב׳",
                "ניסן", "אייר", "סיון", "תמוז", "אב", "אלול"
            ),
            buildMonthList(isLeapYear = true).map { it.hebrewDisplayName }
        )
    }

    /** Same names as the calendar, day cells and My events rows (KosherJava's formatter). */
    @Test
    fun editorNamesMatchKosherJavaForRealYears() {
        // 5786 is an ordinary year, 5787 a leap year
        for (year in listOf(5786, 5787)) {
            val isLeap = JewishDate(year, JewishDate.TISHREI, 1).isJewishLeapYear
            for (option in buildMonthList(isLeap)) {
                assertEquals(
                    formatter.formatMonth(JewishDate(year, option.jewishDateMonth, 1)),
                    option.hebrewDisplayName
                )
                assertEquals(HebrewMonth.hebrewName(option.jewishDateMonth, isLeap), option.hebrewDisplayName)
            }
        }
    }

    @Test
    fun allSharedHebrewNamesMatchKosherJava() {
        for (month in HebrewMonth.entries) {
            val isLeap = month == HebrewMonth.ADAR_I || month == HebrewMonth.ADAR_II
            val year = if (isLeap) 5787 else 5786
            assertEquals(
                month.name,
                formatter.formatMonth(JewishDate(year, month.jewishDateValue, 1)),
                month.hebrewName
            )
        }
    }

    @Test
    fun englishNamesUnchanged() {
        assertEquals(
            listOf(
                "Tishrei", "Cheshvan", "Kislev", "Teves", "Shevat", "Adar I", "Adar II",
                "Nissan", "Iyar", "Sivan", "Tammuz", "Av", "Elul"
            ),
            buildMonthList(isLeapYear = true).map { it.displayName }
        )
        assertEquals("Adar", buildMonthList(isLeapYear = false)[5].displayName)
    }
}
