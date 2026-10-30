package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.domain.model.SupportedHebrewYears
import ninja.doskey.app.levana.domain.model.YahrzeitLeapYearAdar
import ninja.doskey.app.levana.domain.model.YahrzeitRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HebrewDateMatcherTest {

    @Test
    fun unknownOrOutOfRangeYearIsNonLeapAndDoesNotThrow() {
        for (year in listOf(0, -1, 1, 3761)) {
            assertFalse(HebrewDateMatcher.isKnownYear(year))
            assertFalse(HebrewDateMatcher.isEventLeapYear(year))
        }
    }

    @Test
    fun absurdlyLargeYearIsUnknownAndDoesNotThrow() {
        assertTrue(HebrewDateMatcher.isKnownYear(HebrewDateMatcher.MIN_KNOWN_YEAR))
        assertTrue(HebrewDateMatcher.isKnownYear(HebrewDateMatcher.MAX_KNOWN_YEAR))
        for (year in listOf(10000, 99999, Int.MAX_VALUE, Int.MIN_VALUE)) {
            assertFalse(HebrewDateMatcher.isKnownYear(year))
            assertFalse(HebrewDateMatcher.isEventLeapYear(year))
            // Still matches as an unknown-year date
            assertTrue(birthdayMatches(10, JewishDate.KISLEV, year, JewishDate.KISLEV, 10, false))
            assertTrue(birthdayMatches(14, JewishDate.ADAR, year, JewishDate.ADAR_II, 14, true))
        }
    }

    @Test
    fun pickerYearsAreAllKnown() {
        assertTrue(HebrewDateMatcher.isKnownYear(SupportedHebrewYears.FIRST))
        assertTrue(HebrewDateMatcher.isKnownYear(SupportedHebrewYears.LAST))
    }

    @Test
    fun monthForYearDropsAdarIIFromRegularYears() {
        assertEquals(JewishDate.ADAR, HebrewDateMatcher.monthForYear(JewishDate.ADAR_II, 5785))
        assertEquals(JewishDate.ADAR_II, HebrewDateMatcher.monthForYear(JewishDate.ADAR_II, 5784))
        assertEquals(JewishDate.ADAR, HebrewDateMatcher.monthForYear(JewishDate.ADAR, 5784))
        assertEquals(JewishDate.KISLEV, HebrewDateMatcher.monthForYear(JewishDate.KISLEV, 5785))
    }

    @Test
    fun hebrewYearOfFollowsRoshHashana() {
        // 1 Tishrei 5787 = 12 September 2026
        assertEquals(5786, HebrewDateMatcher.hebrewYearOf(java.time.LocalDate.of(2026, 9, 11)))
        assertEquals(5787, HebrewDateMatcher.hebrewYearOf(java.time.LocalDate.of(2026, 9, 12)))
    }

    @Test
    fun editorDateShowsUnknownYearsOnTheCurrentYear() {
        val adar = JewishDate.ADAR
        val adar2 = JewishDate.ADAR_II
        // Known year: as stored
        assertEquals(adar to 5784, editorMonthYear(adar, 5784, false, 5787))
        assertEquals(JewishDate.KISLEV to 5700, editorMonthYear(JewishDate.KISLEV, 5700, false, 5787))
        // Known regular year stored with Adar II: shown as Adar
        assertEquals(adar to 5785, editorMonthYear(adar2, 5785, false, 5787))
        // Unknown year: current year, month where it falls this year
        for (unknown in listOf(0, 1, -1, 3761, 10000)) {
            for (yahrzeit in listOf(false, true)) {
                assertEquals(JewishDate.KISLEV to 5787, editorMonthYear(JewishDate.KISLEV, unknown, yahrzeit, 5787))
            }
            assertEquals(adar2 to 5787, editorMonthYear(adar, unknown, false, 5787))
            assertEquals(adar2 to 5787, editorMonthYear(adar2, unknown, false, 5787))
            assertEquals(adar to 5786, editorMonthYear(adar, unknown, false, 5786))
            assertEquals(adar to 5786, editorMonthYear(adar2, unknown, false, 5786))
        }
    }

    @Test
    fun editorDateForUnknownYearYahrzeitInAdarKeepsTheSameRules() {
        val adar = JewishDate.ADAR
        val adar2 = JewishDate.ADAR_II
        // 5784 leap, 5785 and 5786 ordinary, 5787 leap
        assertEquals(adar to 5785, editorMonthYear(adar, 0, true, 5787))
        assertEquals(adar to 5785, editorMonthYear(adar, 0, true, 5786))
        assertEquals(adar2 to 5787, editorMonthYear(adar2, 0, true, 5787))
        assertEquals(adar2 to 5784, editorMonthYear(adar2, 0, true, 5786))

        // The shown date behaves like the unknown year under every setting, in every year
        for (current in 5780..5800) {
            for (storedMonth in listOf(adar, adar2)) {
                val (_, month, year) = HebrewDateMatcher.editorDate(15, storedMonth, 0, true, current)
                for (rules in YahrzeitAdarRulesTest.allRules) {
                    for (target in 5770..5810) {
                        for (day in listOf(1, 15, 29, 30)) {
                            assertEquals(
                                "current=$current stored=$storedMonth target=$target $rules",
                                HebrewDateMatcher.occurrences(day, storedMonth, 0, true, target, rules),
                                HebrewDateMatcher.occurrences(day, month, year, true, target, rules)
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun knownYearLeapMatchesKosherJava() {
        assertTrue(HebrewDateMatcher.isEventLeapYear(5784))
        assertFalse(HebrewDateMatcher.isEventLeapYear(5785))
        for (year in HebrewDateMatcher.MIN_KNOWN_YEAR..HebrewDateMatcher.MIN_KNOWN_YEAR + 40) {
            assertEquals(
                JewishDate(year, JewishDate.TISHREI, 1).isJewishLeapYear,
                HebrewDateMatcher.isEventLeapYear(year)
            )
        }
        for (year in 5700..5900) {
            assertEquals(
                JewishDate(year, JewishDate.TISHREI, 1).isJewishLeapYear,
                HebrewDateMatcher.isEventLeapYear(year)
            )
        }
    }

    @Test
    fun birthdayWithYearZeroMatchesWithoutCrashing() {
        assertTrue(birthdayMatches(10, JewishDate.KISLEV, 0, JewishDate.KISLEV, 10, false))
        assertTrue(birthdayMatches(10, JewishDate.KISLEV, 0, JewishDate.KISLEV, 10, true))
        assertFalse(birthdayMatches(10, JewishDate.KISLEV, 0, JewishDate.KISLEV, 11, false))
        assertFalse(birthdayMatches(10, JewishDate.KISLEV, 1, JewishDate.TEVES, 10, false))
    }

    @Test
    fun birthdayWithValidYearMatches() {
        assertTrue(birthdayMatches(10, JewishDate.KISLEV, 5784, JewishDate.KISLEV, 10, false))
        assertFalse(birthdayMatches(10, JewishDate.KISLEV, 5784, JewishDate.TEVES, 10, false))
    }

    @Test
    fun unknownYearAdarBirthday() {
        // Non-leap target: Adar
        assertTrue(birthdayMatches(14, JewishDate.ADAR, 0, JewishDate.ADAR, 14, false))
        // Leap target: Adar II, not Adar I
        assertTrue(birthdayMatches(14, JewishDate.ADAR, 0, JewishDate.ADAR_II, 14, true))
        assertFalse(birthdayMatches(14, JewishDate.ADAR, 0, JewishDate.ADAR, 14, true))
        // Adar II with unknown year: Adar II in leap years, Adar otherwise
        assertTrue(birthdayMatches(14, JewishDate.ADAR_II, 0, JewishDate.ADAR_II, 14, true))
        assertTrue(birthdayMatches(14, JewishDate.ADAR_II, 0, JewishDate.ADAR, 14, false))
    }

    @Test
    fun knownYearAdarBirthday() {
        // Adar I of leap year 5784 -> Adar I in leap years, Adar otherwise
        assertTrue(birthdayMatches(14, JewishDate.ADAR, 5784, JewishDate.ADAR, 14, true))
        assertFalse(birthdayMatches(14, JewishDate.ADAR, 5784, JewishDate.ADAR_II, 14, true))
        assertTrue(birthdayMatches(14, JewishDate.ADAR, 5784, JewishDate.ADAR, 14, false))
        // Adar II of 5784 -> Adar in non-leap years
        assertTrue(birthdayMatches(14, JewishDate.ADAR_II, 5784, JewishDate.ADAR, 14, false))
        // Adar of non-leap 5785 -> Adar II in leap years, not Adar I
        assertTrue(birthdayMatches(14, JewishDate.ADAR, 5785, JewishDate.ADAR_II, 14, true))
        assertFalse(birthdayMatches(14, JewishDate.ADAR, 5785, JewishDate.ADAR, 14, true))
    }

    /**
     * Full Adar matrix: stored month x stored year kind x target year kind x
     * yahrzeit rule. Stored years: 5785 regular, 5784 leap, and every flavour
     * of unknown.
     */
    @Test
    fun adarMatrix() {
        val adar = JewishDate.ADAR
        val adar2 = JewishDate.ADAR_II
        val regularYears = listOf(5785, 5786)
        val leapYears = listOf(5784, 5787)
        val unknownYears = listOf(0, 1, -1, 3761)

        // Triple(stored month, stored years, expected month in a leap target year)
        val cases = listOf(
            Triple(adar, regularYears, adar2),
            Triple(adar, unknownYears, adar2),
            Triple(adar, leapYears, adar),
            Triple(adar2, regularYears, adar2),
            Triple(adar2, unknownYears, adar2),
            Triple(adar2, leapYears, adar2)
        )

        for ((storedMonth, storedYears, expectedInLeap) in cases) {
            for (storedYear in storedYears) {
                for (yahrzeit in listOf(false, true)) {
                    val label = "month=$storedMonth year=$storedYear yahrzeit=$yahrzeit"
                    assertEquals("$label, regular target", adar, month(storedMonth, storedYear, yahrzeit, false))
                    assertEquals("$label, leap target", expectedInLeap, month(storedMonth, storedYear, yahrzeit, true))
                    // matchesDate agrees for both Adars
                    for (targetLeap in listOf(false, true)) {
                        val expected = if (targetLeap) expectedInLeap else adar
                        for (targetMonth in listOf(adar, adar2)) {
                            assertEquals(
                                "$label targetLeap=$targetLeap targetMonth=$targetMonth",
                                targetMonth == expected,
                                HebrewDateMatcher.matchesDate(
                                    7, storedMonth, storedYear, yahrzeit,
                                    targetYear(targetLeap), targetMonth, 7, adarIIRules
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun monthsOtherThanAdarNeverMove() {
        val others = (JewishDate.NISSAN..JewishDate.SHEVAT).toList()
        for (month in others) {
            for (storedYear in listOf(0, 1, 5784, 5785)) {
                for (yahrzeit in listOf(false, true)) {
                    for (targetLeap in listOf(false, true)) {
                        assertEquals(month, month(month, storedYear, yahrzeit, targetLeap))
                    }
                }
            }
        }
    }

    @Test
    fun unknownYearAdarYahrzeitFollowsNonLeapRule() {
        // Default annual setting is both Adars: Adar I and Adar II in leap years
        assertTrue(yahrzeitMatches(7, JewishDate.ADAR, 0, JewishDate.ADAR_II, 7, true))
        assertTrue(yahrzeitMatches(7, JewishDate.ADAR, 0, JewishDate.ADAR, 7, true))
        assertTrue(yahrzeitMatches(7, JewishDate.ADAR, 0, JewishDate.ADAR, 7, false))
        // With the Adar II setting, only Adar II
        assertFalse(
            HebrewDateMatcher.matchesDate(7, JewishDate.ADAR, 0, true, targetYear(true), JewishDate.ADAR, 7, adarIIRules)
        )
    }

    @Test
    fun knownYearAdarYahrzeit() {
        // Adar of non-leap 5785 -> Adar II in leap years
        assertTrue(yahrzeitMatches(7, JewishDate.ADAR, 5785, JewishDate.ADAR_II, 7, true))
        // Adar I of leap 5784 -> Adar I in leap years
        assertTrue(yahrzeitMatches(7, JewishDate.ADAR, 5784, JewishDate.ADAR, 7, true))
    }

    private fun birthdayMatches(
        day: Int,
        month: Int,
        year: Int,
        targetMonth: Int,
        targetDay: Int,
        targetIsLeapYear: Boolean
    ) = HebrewDateMatcher.matchesDate(
        day, month, year, useYahrzeitRules = false,
        targetYear(targetIsLeapYear), targetMonth, targetDay
    )

    private fun yahrzeitMatches(
        day: Int,
        month: Int,
        year: Int,
        targetMonth: Int,
        targetDay: Int,
        targetIsLeapYear: Boolean
    ) = HebrewDateMatcher.matchesDate(
        day, month, year, useYahrzeitRules = true,
        targetYear(targetIsLeapYear), targetMonth, targetDay
    )

    /** The (month, year) the editor shows for a mid-month stored date. */
    private fun editorMonthYear(month: Int, year: Int, yahrzeit: Boolean, current: Int): Pair<Int, Int> =
        HebrewDateMatcher.editorDate(15, month, year, yahrzeit, current).let { it.second to it.third }

    @Test
    fun editorDateKeepsAStoredDay30WhereTheMonthHasIt() {
        val cheshvan = JewishDate.CHESHVAN
        val kislev = JewishDate.KISLEV
        fun days(y: Int, m: Int) = JewishDate(y, m, 1).daysInJewishMonth
        // Known year with day 30: unchanged
        val full = (5780..5800).first { days(it, cheshvan) == 30 }
        assertEquals(Triple(30, cheshvan, full), HebrewDateMatcher.editorDate(30, cheshvan, full, false, 5787))
        // Known year without day 30 (bad data): shown as 29
        val short = (5780..5800).first { days(it, cheshvan) == 29 }
        assertEquals(Triple(29, cheshvan, short), HebrewDateMatcher.editorDate(30, cheshvan, short, false, 5787))
        assertEquals(Triple(29, JewishDate.IYAR, 5785), HebrewDateMatcher.editorDate(30, JewishDate.IYAR, 5785, false, 5787))
        // Unknown year: a year in which the day exists, so day 30 is kept
        for (current in 5780..5800) for (month in listOf(cheshvan, kislev)) for (yahrzeit in listOf(false, true)) {
            val (day, shownMonth, year) = HebrewDateMatcher.editorDate(30, month, 0, yahrzeit, current)
            assertEquals(30, day)
            assertEquals(month, shownMonth)
            assertTrue(year <= current)
            assertEquals(30, days(year, month))
            // Behaves exactly like the unknown year under every setting
            for (rules in YahrzeitAdarRulesTest.allRules) for (target in 5770..5810) {
                assertEquals(
                    HebrewDateMatcher.occurrences(30, month, 0, yahrzeit, target, rules),
                    HebrewDateMatcher.occurrences(day, shownMonth, year, yahrzeit, target, rules)
                )
            }
        }
        // Unknown-year Adar day 30 has no day 30 to keep: shown as 29
        assertEquals(29, HebrewDateMatcher.editorDate(30, JewishDate.ADAR, 0, false, 5787).first)
        assertEquals(29, HebrewDateMatcher.editorDate(30, JewishDate.ADAR, 0, false, 5786).first)
    }

    /** 5790 is a leap year, 5788 an ordinary year; neither is the year after a test death year. */
    private fun targetYear(leap: Boolean) = if (leap) 5790 else 5788

    /** The single month a stored day-7 date falls in, with default yahrzeit settings. */
    /**
     * The yahrzeit cases in this file pin the "Adar II" annual setting, which
     * gives one month per year like the simple rule. The default (both
     * Adars) is covered in YahrzeitAdarRulesTest.
     */
    private val adarIIRules = YahrzeitRules(leapYearAdar = YahrzeitLeapYearAdar.ADAR_II)

    private fun month(storedMonth: Int, storedYear: Int, yahrzeit: Boolean, targetLeap: Boolean): Int =
        HebrewDateMatcher.occurrences(7, storedMonth, storedYear, yahrzeit, targetYear(targetLeap), adarIIRules)
            .single().month
}
