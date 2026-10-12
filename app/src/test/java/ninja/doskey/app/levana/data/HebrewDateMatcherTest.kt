package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
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
        assertTrue(HebrewDateMatcher.isKnownYear(HebrewDateMatcher.EDITABLE_YEARS.first))
        assertTrue(HebrewDateMatcher.isKnownYear(HebrewDateMatcher.EDITABLE_YEARS.last))
    }

    @Test
    fun monthForYearDropsAdarIIFromRegularYears() {
        assertEquals(JewishDate.ADAR, HebrewDateMatcher.monthForYear(JewishDate.ADAR_II, 5785))
        assertEquals(JewishDate.ADAR_II, HebrewDateMatcher.monthForYear(JewishDate.ADAR_II, 5784))
        assertEquals(JewishDate.ADAR, HebrewDateMatcher.monthForYear(JewishDate.ADAR, 5784))
        assertEquals(JewishDate.KISLEV, HebrewDateMatcher.monthForYear(JewishDate.KISLEV, 5785))
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
                    assertEquals(
                        "$label, regular target",
                        adar,
                        HebrewDateMatcher.resolveMonth(storedMonth, storedYear, yahrzeit, false)
                    )
                    assertEquals(
                        "$label, leap target",
                        expectedInLeap,
                        HebrewDateMatcher.resolveMonth(storedMonth, storedYear, yahrzeit, true)
                    )
                    // matchesDate agrees with resolveMonth for both Adars
                    for (targetLeap in listOf(false, true)) {
                        val expected = if (targetLeap) expectedInLeap else adar
                        for (targetMonth in listOf(adar, adar2)) {
                            assertEquals(
                                "$label targetLeap=$targetLeap targetMonth=$targetMonth",
                                targetMonth == expected,
                                HebrewDateMatcher.matchesDate(
                                    7, storedMonth, storedYear, yahrzeit,
                                    targetMonth, 7, targetLeap
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
                        assertEquals(
                            month,
                            HebrewDateMatcher.resolveMonth(month, storedYear, yahrzeit, targetLeap)
                        )
                    }
                }
            }
        }
    }

    /** The yahrzeit rule itself is unchanged by the birthday/custom-event fix. */
    @Test
    fun yahrzeitRuleIsUnchanged() {
        val adar = JewishDate.ADAR
        val adar2 = JewishDate.ADAR_II
        assertEquals(adar2, HebrewDateMatcher.resolveYahrzeitMonth(adar, false, true))
        assertEquals(adar, HebrewDateMatcher.resolveYahrzeitMonth(adar, true, true))
        assertEquals(adar2, HebrewDateMatcher.resolveYahrzeitMonth(adar2, true, true))
        assertEquals(adar2, HebrewDateMatcher.resolveYahrzeitMonth(adar2, false, true))
        assertEquals(adar, HebrewDateMatcher.resolveYahrzeitMonth(adar, false, false))
        assertEquals(adar, HebrewDateMatcher.resolveYahrzeitMonth(adar, true, false))
        assertEquals(adar, HebrewDateMatcher.resolveYahrzeitMonth(adar2, true, false))
    }

    @Test
    fun unknownYearAdarYahrzeitFollowsNonLeapRule() {
        assertTrue(yahrzeitMatches(7, JewishDate.ADAR, 0, JewishDate.ADAR_II, 7, true))
        assertFalse(yahrzeitMatches(7, JewishDate.ADAR, 0, JewishDate.ADAR, 7, true))
        assertTrue(yahrzeitMatches(7, JewishDate.ADAR, 0, JewishDate.ADAR, 7, false))
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
        targetMonth, targetDay, targetIsLeapYear
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
        targetMonth, targetDay, targetIsLeapYear
    )
}
