package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.domain.model.Occurrence
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import ninja.doskey.app.levana.domain.model.YahrzeitFirstYear
import ninja.doskey.app.levana.domain.model.YahrzeitLeapYearAdar
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import ninja.doskey.app.levana.domain.model.YahrzeitRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The owner's yahrzeit Adar rules (spec section "Yahrzeit Adar rules").
 *
 * Years used: 5784 leap; 5785 ordinary followed by ordinary 5786; 5786
 * ordinary followed by leap 5787; 5788 ordinary; 5790 leap.
 */
class YahrzeitAdarRulesTest {

    companion object {
        /** Every combination of the four settings (3 x 3 x 2 x 3 = 54). */
        val allRules: List<YahrzeitRules> =
            YahrzeitLeapYearAdar.entries.flatMap { leap ->
                YahrzeitFirstYear.entries.flatMap { first ->
                    listOf(false, true).flatMap { nisan ->
                        YahrzeitMissingDay.entries.map { missing ->
                            YahrzeitRules(leap, first, nisan, missing)
                        }
                    }
                }
            }

        private const val ADAR = JewishDate.ADAR
        private const val ADAR_II = JewishDate.ADAR_II

        private val ORDINARY_TARGETS = listOf(5786, 5788)
        private val LEAP_TARGETS = listOf(5787, 5790)
        private val UNKNOWN_YEARS = listOf(0, 1, -1, 3761, 10000)
    }

    private fun yahrzeit(day: Int, month: Int, deathYear: Int, target: Int, rules: YahrzeitRules) =
        HebrewDateMatcher.occurrences(day, month, deathYear, true, target, rules)

    private fun one(month: Int, day: Int) = listOf(Occurrence(month, day))

    private val bothAdars = { day: Int ->
        listOf(
            Occurrence(ADAR, day, OccurrenceLabel.ADAR_I),
            Occurrence(ADAR_II, day, OccurrenceLabel.ADAR_II)
        )
    }

    private fun annual(rules: YahrzeitRules, day: Int): List<Occurrence> = when (rules.leapYearAdar) {
        YahrzeitLeapYearAdar.ADAR_I -> one(ADAR, day)
        YahrzeitLeapYearAdar.ADAR_II -> one(ADAR_II, day)
        YahrzeitLeapYearAdar.BOTH -> bothAdars(day)
    }

    @Test
    fun settingsAreIndependentAndCoverEveryValue() {
        assertEquals(54, allRules.toSet().size)
        assertEquals(
            YahrzeitRules(
                YahrzeitLeapYearAdar.BOTH,
                YahrzeitFirstYear.FOLLOW_ANNUAL,
                false,
                YahrzeitMissingDay.FIRST_ANNIVERSARY
            ),
            YahrzeitRules()
        )
    }

    // ── Section 1: death in a leap year ─────────────────────────────────────

    @Test
    fun section1AdarIDays1To29() {
        for (rules in allRules) for (day in listOf(1, 15, 29)) {
            for (target in ORDINARY_TARGETS) assertEquals("$rules", one(ADAR, day), yahrzeit(day, ADAR, 5784, target, rules))
            for (target in LEAP_TARGETS) assertEquals("$rules", one(ADAR, day), yahrzeit(day, ADAR, 5784, target, rules))
            // The year right after the death year is not special for a leap-year death
            assertEquals(one(ADAR, day), yahrzeit(day, ADAR, 5784, 5785, rules))
        }
    }

    @Test
    fun section1AdarIIDays1To29() {
        for (rules in allRules) for (day in listOf(1, 15, 29)) {
            for (target in ORDINARY_TARGETS) assertEquals("$rules", one(ADAR, day), yahrzeit(day, ADAR_II, 5784, target, rules))
            for (target in LEAP_TARGETS) assertEquals("$rules", one(ADAR_II, day), yahrzeit(day, ADAR_II, 5784, target, rules))
        }
    }

    @Test
    fun section1Day30OfAdarI() {
        for (rules in allRules) {
            for (target in ORDINARY_TARGETS + 5785) {
                val expected = if (rules.adarI30AlsoNisan1) {
                    listOf(
                        Occurrence(JewishDate.SHEVAT, 30, OccurrenceLabel.SHEVAT_30),
                        Occurrence(JewishDate.NISSAN, 1, OccurrenceLabel.NISAN_1)
                    )
                } else {
                    one(JewishDate.SHEVAT, 30)
                }
                assertEquals("$rules target=$target", expected, yahrzeit(30, ADAR, 5784, target, rules))
            }
            for (target in LEAP_TARGETS) {
                assertEquals("$rules target=$target", one(ADAR, 30), yahrzeit(30, ADAR, 5784, target, rules))
            }
        }
    }

    @Test
    fun day30OfAdarIOccurrencesAreRealDays() {
        val nisanOn = YahrzeitRules(adarI30AlsoNisan1 = true)
        for (target in ORDINARY_TARGETS) {
            for (occurrence in yahrzeit(30, ADAR, 5784, target, nisanOn)) {
                assertTrue(occurrence.day <= JewishDate(target, occurrence.month, 1).daysInJewishMonth)
            }
            // Shevat 30 and 1 Nisan are in the same Hebrew year as that Adar
            assertTrue(HebrewDateMatcher.matchesDate(30, ADAR, 5784, true, target, JewishDate.SHEVAT, 30, nisanOn))
            assertTrue(HebrewDateMatcher.matchesDate(30, ADAR, 5784, true, target, JewishDate.NISSAN, 1, nisanOn))
            assertFalse(HebrewDateMatcher.matchesDate(30, ADAR, 5784, true, target, ADAR, 30, nisanOn))
        }
        for (target in LEAP_TARGETS) {
            assertEquals(30, JewishDate(target, ADAR, 1).daysInJewishMonth)
        }
    }

    // ── Section 2: death in Adar of an ordinary year ────────────────────────

    @Test
    fun section2OrdinaryTargetIsAlwaysAdar() {
        for (rules in allRules) for (death in listOf(5785, 5786)) for (target in ORDINARY_TARGETS) {
            assertEquals("$rules", one(ADAR, 15), yahrzeit(15, ADAR, death, target, rules))
        }
    }

    @Test
    fun section2LeapTargetFollowsTheAnnualSetting() {
        // 5785 is followed by an ordinary year, so the first-year setting never applies
        for (rules in allRules) for (target in LEAP_TARGETS) {
            assertEquals("$rules target=$target", annual(rules, 15), yahrzeit(15, ADAR, 5785, target, rules))
        }
    }

    // ── Section 3: first-year exception ─────────────────────────────────────

    @Test
    fun section3FirstYear() {
        // Death 5786 (ordinary), first yahrzeit 5787 (leap)
        for (rules in allRules) {
            val expected = when (rules.firstYear) {
                YahrzeitFirstYear.FOLLOW_ANNUAL -> annual(rules, 15)
                YahrzeitFirstYear.ADAR_I -> one(ADAR, 15)
                YahrzeitFirstYear.BOTH -> bothAdars(15)
            }
            assertEquals("$rules", expected, yahrzeit(15, ADAR, 5786, 5787, rules))
        }
    }

    @Test
    fun section3LaterYearsUseTheAnnualSetting() {
        for (rules in allRules) {
            // Later leap years
            for (target in listOf(5790, 5793)) {
                assertEquals("$rules target=$target", annual(rules, 15), yahrzeit(15, ADAR, 5786, target, rules))
            }
            // Ordinary years before and after, and the death year itself
            for (target in listOf(5786, 5788, 5789)) {
                assertEquals("$rules target=$target", one(ADAR, 15), yahrzeit(15, ADAR, 5786, target, rules))
            }
            // Earlier leap years are not the first year either
            assertEquals("$rules", annual(rules, 15), yahrzeit(15, ADAR, 5786, 5784, rules))
        }
    }

    // ── Unknown death year ──────────────────────────────────────────────────

    @Test
    fun unknownDeathYearAdarIsOrdinaryYearAdarWithoutFirstYearException() {
        for (rules in allRules) for (year in UNKNOWN_YEARS) {
            for (target in ORDINARY_TARGETS) assertEquals("$rules", one(ADAR, 15), yahrzeit(15, ADAR, year, target, rules))
            for (target in LEAP_TARGETS) {
                assertEquals("$rules year=$year target=$target", annual(rules, 15), yahrzeit(15, ADAR, year, target, rules))
            }
        }
    }

    @Test
    fun unknownDeathYearAdarIIIsLeapYearAdarII() {
        for (rules in allRules) for (year in UNKNOWN_YEARS) {
            for (target in ORDINARY_TARGETS) assertEquals(one(ADAR, 15), yahrzeit(15, ADAR_II, year, target, rules))
            for (target in LEAP_TARGETS) assertEquals(one(ADAR_II, 15), yahrzeit(15, ADAR_II, year, target, rules))
        }
    }

    @Test
    fun day30OfAdarOtherThanAdarIIsNotSpecialAndCheshvanKislevIgnoreAdarSettings() {
        // Unknown-year Adar 30 is not Adar I: no Shevat/Nisan substitution
        for (rules in allRules) for (target in ORDINARY_TARGETS) {
            assertEquals(one(ADAR, 30), yahrzeit(30, ADAR, 0, target, rules))
        }
        // 30 Cheshvan / 30 Kislev (section 1c) are unaffected by the Adar settings
        for (month in listOf(JewishDate.CHESHVAN, JewishDate.KISLEV)) {
            for (target in ORDINARY_TARGETS + LEAP_TARGETS) {
                for (missing in YahrzeitMissingDay.entries) {
                    val results = allRules.filter { it.missingDay == missing }
                        .map { yahrzeit(30, month, 5784, target, it) }.toSet()
                    assertEquals(1, results.size)
                }
            }
        }
    }

    @Test
    fun otherMonthsAreUnaffectedBySettings() {
        for (rules in allRules) for (month in (JewishDate.NISSAN..JewishDate.SHEVAT)) {
            for (death in listOf(0, 5784, 5785, 5786)) for (target in ORDINARY_TARGETS + LEAP_TARGETS) {
                assertEquals(one(month, 10), yahrzeit(10, month, death, target, rules))
            }
        }
    }

    // ── Non-yahrzeit events ─────────────────────────────────────────────────

    @Test
    fun nonYahrzeitEventsAreUnaffectedByEverySetting() {
        for (rules in allRules) {
            for (month in JewishDate.NISSAN..JewishDate.ADAR_II) {
                for (stored in listOf(0, 1, -1, 5784, 5785, 5786, 5787)) {
                    for (target in 5780..5800) {
                        for (day in listOf(1, 15, 30)) {
                            val withRules = HebrewDateMatcher.occurrences(day, month, stored, false, target, rules)
                            val defaults = HebrewDateMatcher.occurrences(day, month, stored, false, target)
                            assertEquals(defaults, withRules)
                            assertEquals(1, withRules.size)
                            // A missing day 30 moves to the 1st (spec 1d, Day30BirthdaysAndEventsTest)
                            if (day == 30 && withRules.single().day == 1) continue
                            assertEquals(null, withRules.single().label)
                            assertEquals(day, withRules.single().day)
                            assertEquals(
                                HebrewDateMatcher.resolveSimpleMonth(
                                    month,
                                    HebrewDateMatcher.isEventLeapYear(stored),
                                    HebrewDateMatcher.isEventLeapYear(target)
                                ),
                                withRules.single().month
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Defaults ─────────────────────────────────────────────────────────────

    /** The default annual setting is both Adars: two labelled occurrences in a leap year. */
    @Test
    fun defaultIsBothAdarsWithTwoLabelledOccurrencesInALeapYear() {
        assertEquals(YahrzeitLeapYearAdar.BOTH, YahrzeitRules().leapYearAdar)
        for (death in listOf(0, 5785, 5786)) for (target in LEAP_TARGETS) {
            if (death == 5786 && target == 5787) continue // first year: FOLLOW_ANNUAL gives the same
            assertEquals(bothAdars(15), yahrzeit(15, ADAR, death, target, YahrzeitRules()))
        }
        // First year with the default first-year setting follows the annual setting
        assertEquals(bothAdars(15), yahrzeit(15, ADAR, 5786, 5787, YahrzeitRules()))
        // Ordinary years: one unlabelled Adar
        for (target in ORDINARY_TARGETS) {
            assertEquals(one(ADAR, 15), yahrzeit(15, ADAR, 5785, target, YahrzeitRules()))
        }
        // Leap-year deaths are unaffected by the annual setting
        assertEquals(one(ADAR, 15), yahrzeit(15, ADAR, 5784, 5787, YahrzeitRules()))
        assertEquals(one(ADAR_II, 15), yahrzeit(15, ADAR_II, 5784, 5787, YahrzeitRules()))
    }

    /** The "Adar II" annual setting keeps the pre-settings yahrzeit rule (except 30 Adar I). */
    @Test
    fun adarIISettingKeepsThePreviousYahrzeitBehaviour() {
        fun previousRule(month: Int, deathLeap: Boolean, targetLeap: Boolean): Int = when {
            !targetLeap && (month == ADAR || month == ADAR_II) -> ADAR
            month == ADAR && !deathLeap -> ADAR_II
            month == ADAR && deathLeap -> ADAR
            else -> month
        }
        for (month in JewishDate.NISSAN..JewishDate.ADAR_II) {
            for (death in listOf(0, 5784, 5785, 5786, 5787)) for (target in 5780..5800) {
                for (day in 1..29) {
                    val expected = previousRule(
                        month,
                        HebrewDateMatcher.isEventLeapYear(death),
                        HebrewDateMatcher.isEventLeapYear(target)
                    )
                    assertEquals(
                        one(expected, day),
                        yahrzeit(day, month, death, target, YahrzeitRules(leapYearAdar = YahrzeitLeapYearAdar.ADAR_II))
                    )
                }
            }
        }
    }
}
