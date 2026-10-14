package ninja.doskey.app.levana.ui

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import org.junit.Assert.assertEquals

/**
 * Spec 1e day-clamp cases shared by both editor view model tests. [Editor]
 * adapts each editor's intents; years come from KosherJava month lengths.
 */
object EditorDayClampCases {

    interface Editor {
        fun setDay(day: Int)
        fun setMonth(month: Int)
        fun setYear(year: Int)
        val day: Int
        val month: Int
        val year: Int
    }

    private val years = 5780..5820
    fun days(year: Int, month: Int) = JewishDate(year, month, 1).daysInJewishMonth
    fun isLeap(year: Int) = JewishDate(year, JewishDate.TISHREI, 1).isJewishLeapYear

    fun shortYear(month: Int) = years.first { days(it, month) == 29 }
    fun fullYear(month: Int) = years.first { days(it, month) == 30 }
    val leapYear get() = years.first { isLeap(it) }
    val ordinaryYear get() = years.first { !isLeap(it) }

    /** Day 30 of Tishrei (always 30 days) in [year]. */
    private fun day30Tishrei(editor: Editor, year: Int) {
        editor.setYear(year)
        editor.setMonth(JewishDate.TISHREI)
        editor.setDay(30)
        assertEquals(30, editor.day)
    }

    fun switchingToA29DayMonthClampsTo29(newEditor: () -> Editor) {
        val cases = listOf(
            shortYear(JewishDate.CHESHVAN) to JewishDate.CHESHVAN,
            shortYear(JewishDate.KISLEV) to JewishDate.KISLEV,
            leapYear to JewishDate.ADAR_II,
            ordinaryYear to JewishDate.ADAR,
            ordinaryYear to JewishDate.IYAR,
            leapYear to JewishDate.TAMMUZ,
            ordinaryYear to JewishDate.ELUL
        )
        for ((year, month) in cases) {
            val editor = newEditor()
            day30Tishrei(editor, year)
            editor.setMonth(month)
            assertEquals("year=$year month=$month", month, editor.month)
            assertEquals("year=$year month=$month", 29, editor.day)
        }
        // A 30-day month keeps 30
        for ((year, month) in listOf(
            fullYear(JewishDate.CHESHVAN) to JewishDate.CHESHVAN,
            fullYear(JewishDate.KISLEV) to JewishDate.KISLEV,
            leapYear to JewishDate.ADAR,
            ordinaryYear to JewishDate.SHEVAT
        )) {
            val editor = newEditor()
            day30Tishrei(editor, year)
            editor.setMonth(month)
            assertEquals("year=$year month=$month", 30, editor.day)
        }
    }

    fun yearChangeThatShortensTheMonthClamps(newEditor: () -> Editor) {
        for (month in listOf(JewishDate.CHESHVAN, JewishDate.KISLEV)) {
            val editor = newEditor()
            editor.setYear(fullYear(month))
            editor.setMonth(month)
            editor.setDay(30)
            assertEquals(30, editor.day)
            editor.setYear(shortYear(month))
            assertEquals(29, editor.day)
            assertEquals(month, editor.month)
        }
    }

    fun adarI30ThenAnOrdinaryYear(newEditor: () -> Editor) {
        val editor = newEditor()
        editor.setYear(leapYear)
        editor.setMonth(JewishDate.ADAR) // Adar I
        editor.setDay(30)
        assertEquals(30, editor.day)
        editor.setYear(ordinaryYear)
        assertEquals(JewishDate.ADAR, editor.month)
        assertEquals(29, editor.day)
        // Adar II 29 -> ordinary year: Adar, still 29
        val editor2 = newEditor()
        editor2.setYear(leapYear)
        editor2.setMonth(JewishDate.ADAR_II)
        editor2.setDay(30)
        assertEquals(29, editor2.day)
        editor2.setYear(ordinaryYear)
        assertEquals(JewishDate.ADAR, editor2.month)
        assertEquals(29, editor2.day)
    }

    fun steppingBackToA30DayMonthDoesNotRestore30(newEditor: () -> Editor) {
        val editor = newEditor()
        day30Tishrei(editor, ordinaryYear)
        editor.setMonth(JewishDate.IYAR)
        assertEquals(29, editor.day)
        editor.setMonth(JewishDate.TISHREI)
        assertEquals(29, editor.day)

        val year = shortYear(JewishDate.CHESHVAN)
        val editor2 = newEditor()
        editor2.setYear(fullYear(JewishDate.CHESHVAN))
        editor2.setMonth(JewishDate.CHESHVAN)
        editor2.setDay(30)
        editor2.setYear(year)
        editor2.setYear(fullYear(JewishDate.CHESHVAN))
        assertEquals(29, editor2.day)
        // The user can still pick 30 again
        editor2.setDay(30)
        assertEquals(30, editor2.day)
    }

    fun dayCanNeverExceedTheMonth(newEditor: () -> Editor) {
        val editor = newEditor()
        editor.setYear(ordinaryYear)
        editor.setMonth(JewishDate.IYAR)
        editor.setDay(30)
        assertEquals(29, editor.day)
        editor.setDay(0)
        assertEquals(1, editor.day)
    }
}
