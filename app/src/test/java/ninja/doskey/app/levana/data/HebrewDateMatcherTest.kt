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
        // Leap target: Adar (I), not Adar II
        assertTrue(birthdayMatches(14, JewishDate.ADAR, 0, JewishDate.ADAR, 14, true))
        assertFalse(birthdayMatches(14, JewishDate.ADAR, 0, JewishDate.ADAR_II, 14, true))
        // Adar II with unknown year: Adar II in leap years, Adar otherwise
        assertTrue(birthdayMatches(14, JewishDate.ADAR_II, 0, JewishDate.ADAR_II, 14, true))
        assertTrue(birthdayMatches(14, JewishDate.ADAR_II, 0, JewishDate.ADAR, 14, false))
    }

    @Test
    fun knownYearAdarBirthday() {
        // Adar I of leap year 5784 -> Adar I in leap years, Adar otherwise
        assertTrue(birthdayMatches(14, JewishDate.ADAR, 5784, JewishDate.ADAR, 14, true))
        assertTrue(birthdayMatches(14, JewishDate.ADAR, 5784, JewishDate.ADAR, 14, false))
        // Adar II of 5784 -> Adar in non-leap years
        assertTrue(birthdayMatches(14, JewishDate.ADAR_II, 5784, JewishDate.ADAR, 14, false))
        // Adar of non-leap 5785 -> Adar (I) in leap years
        assertTrue(birthdayMatches(14, JewishDate.ADAR, 5785, JewishDate.ADAR, 14, true))
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
