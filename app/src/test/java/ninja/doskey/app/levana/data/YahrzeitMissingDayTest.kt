package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.domain.model.Occurrence
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import ninja.doskey.app.levana.domain.model.YahrzeitRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec section 1c: yahrzeits on 30 Cheshvan / 30 Kislev. Concrete years are
 * found by asking KosherJava for month lengths, not hard-coded.
 */
class YahrzeitMissingDayTest {

    private val cheshvan = JewishDate.CHESHVAN
    private val kislev = JewishDate.KISLEV
    private val teves = JewishDate.TEVES
    private val years = 5700..5900

    private fun days(year: Int, month: Int) = JewishDate(year, month, 1).daysInJewishMonth

    private fun targetYear(month: Int, length: Int, after: Int = 5780) =
        years.first { it >= after && days(it, month) == length }

    /** A death year whose first anniversary year (death + 1) has [length] days in [month]. */
    private fun deathYear(month: Int, firstAnniversaryLength: Int) =
        years.first { it >= 5750 && days(it + 1, month) == firstAnniversaryLength }

    private fun rules(missing: YahrzeitMissingDay) = YahrzeitRules(missingDay = missing)

    private fun yahrzeit(month: Int, death: Int, target: Int, missing: YahrzeitMissingDay) =
        HebrewDateMatcher.occurrences(30, month, death, true, target, rules(missing))

    private fun nextMonth(month: Int) = if (month == cheshvan) kislev else teves

    private fun bothLabels(month: Int) = if (month == cheshvan) {
        OccurrenceLabel.CHESHVAN_29 to OccurrenceLabel.KISLEV_1
    } else {
        OccurrenceLabel.KISLEV_29 to OccurrenceLabel.TEVES_1
    }

    /** The table in spec 1c, written independently of the implementation. */
    private fun expected(month: Int, death: Int, target: Int, missing: YahrzeitMissingDay): List<Occurrence> {
        if (days(target, month) == 30) return listOf(Occurrence(month, 30))
        return when (missing) {
            YahrzeitMissingDay.ROSH_CHODESH -> listOf(Occurrence(nextMonth(month), 1))
            YahrzeitMissingDay.BOTH -> {
                val (l29, l1) = bothLabels(month)
                listOf(Occurrence(month, 29, l29), Occurrence(nextMonth(month), 1, l1))
            }
            YahrzeitMissingDay.FIRST_ANNIVERSARY ->
                if (HebrewDateMatcher.isKnownYear(death) && days(death + 1, month) == 29) {
                    listOf(Occurrence(month, 29))
                } else {
                    listOf(Occurrence(nextMonth(month), 1))
                }
        }
    }

    @Test
    fun yearsForEveryCombinationExist() {
        for (month in listOf(cheshvan, kislev)) for (length in listOf(29, 30)) {
            assertEquals(length, days(targetYear(month, length), month))
            assertEquals(length, days(deathYear(month, length) + 1, month))
        }
    }

    /** Each setting x target month 29/30 days x first anniversary month 29/30 days. */
    @Test
    fun fullMatrix() {
        for (month in listOf(cheshvan, kislev)) {
            val next = nextMonth(month)
            val (l29, l1) = bothLabels(month)
            for (firstAnniversaryLength in listOf(29, 30)) {
                val death = deathYear(month, firstAnniversaryLength)
                // Target month has 30 days: always the original date
                val full = targetYear(month, 30, after = death + 2)
                for (missing in YahrzeitMissingDay.entries) {
                    assertEquals(listOf(Occurrence(month, 30)), yahrzeit(month, death, full, missing))
                }
                // Target month has 29 days
                val short = targetYear(month, 29, after = death + 2)
                val label = "month=$month death=$death target=$short"
                assertEquals(label, listOf(Occurrence(next, 1)), yahrzeit(month, death, short, YahrzeitMissingDay.ROSH_CHODESH))
                assertEquals(
                    label,
                    listOf(Occurrence(month, 29, l29), Occurrence(next, 1, l1)),
                    yahrzeit(month, death, short, YahrzeitMissingDay.BOTH)
                )
                assertEquals(
                    label,
                    if (firstAnniversaryLength == 29) listOf(Occurrence(month, 29)) else listOf(Occurrence(next, 1)),
                    yahrzeit(month, death, short, YahrzeitMissingDay.FIRST_ANNIVERSARY)
                )
            }
        }
    }

    @Test
    fun matchesTheSpecTableForEveryYear() {
        for (month in listOf(cheshvan, kislev)) for (death in 5760..5800) {
            for (target in 5700..5900) for (missing in YahrzeitMissingDay.entries) {
                assertEquals(
                    "month=$month death=$death target=$target $missing",
                    expected(month, death, target, missing),
                    yahrzeit(month, death, target, missing)
                )
            }
        }
    }

    @Test
    fun firstAnniversaryYearItselfUsesItsOwnLength() {
        for (month in listOf(cheshvan, kislev)) {
            // Death year where the first anniversary month is short: that year is observed on the 29th
            val death = deathYear(month, 29)
            assertEquals(listOf(Occurrence(month, 29)), yahrzeit(month, death, death + 1, YahrzeitMissingDay.FIRST_ANNIVERSARY))
            // ... and on 30 when the first anniversary month is full
            val deathFull = deathYear(month, 30)
            assertEquals(listOf(Occurrence(month, 30)), yahrzeit(month, deathFull, deathFull + 1, YahrzeitMissingDay.FIRST_ANNIVERSARY))
        }
    }

    @Test
    fun unknownDeathYearFallsBackToRoshChodeshUnderFirstAnniversary() {
        for (month in listOf(cheshvan, kislev)) for (unknown in listOf(0, 1, -1, 3761, 10000)) {
            val short = targetYear(month, 29)
            val full = targetYear(month, 30)
            assertEquals(listOf(Occurrence(nextMonth(month), 1)), yahrzeit(month, unknown, short, YahrzeitMissingDay.FIRST_ANNIVERSARY))
            assertEquals(listOf(Occurrence(nextMonth(month), 1)), yahrzeit(month, unknown, short, YahrzeitMissingDay.ROSH_CHODESH))
            assertEquals(2, yahrzeit(month, unknown, short, YahrzeitMissingDay.BOTH).size)
            for (missing in YahrzeitMissingDay.entries) {
                assertEquals(listOf(Occurrence(month, 30)), yahrzeit(month, unknown, full, missing))
            }
        }
    }

    @Test
    fun targetYearBeforeTheDeathYearStillUsesTheFirstAnniversaryRule() {
        for (month in listOf(cheshvan, kislev)) for (length in listOf(29, 30)) {
            val death = years.first { it >= 5850 && days(it + 1, month) == length }
            val earlierShort = years.first { it in 5780 until death && days(it, month) == 29 }
            assertEquals(
                if (length == 29) listOf(Occurrence(month, 29)) else listOf(Occurrence(nextMonth(month), 1)),
                yahrzeit(month, death, earlierShort, YahrzeitMissingDay.FIRST_ANNIVERSARY)
            )
        }
    }

    @Test
    fun aRecurrenceDoesNotDependOnThePreviousYear() {
        for (month in listOf(cheshvan, kislev)) for (length in listOf(29, 30)) {
            val death = deathYear(month, length)
            val targets = (death + 1)..(death + 40)
            // Find a short year directly preceded by a full year, where a
            // "follow last year" bug would give 30 or Rosh Chodesh
            val short = targets.first { days(it, month) == 29 && days(it - 1, month) == 30 }
            val firstRule = if (length == 29) Occurrence(month, 29) else Occurrence(nextMonth(month), 1)
            assertEquals(listOf(firstRule), yahrzeit(month, death, short, YahrzeitMissingDay.FIRST_ANNIVERSARY))

            // Computing the years forwards, backwards or one at a time gives the same answers
            val forwards = targets.map { yahrzeit(month, death, it, YahrzeitMissingDay.FIRST_ANNIVERSARY) }
            val backwards = targets.reversed().map { yahrzeit(month, death, it, YahrzeitMissingDay.FIRST_ANNIVERSARY) }.reversed()
            assertEquals(forwards, backwards)
            // Every short year gives the same answer as the first anniversary rule
            for ((i, target) in targets.withIndex()) {
                if (days(target, month) == 29) assertEquals(listOf(firstRule), forwards[i])
            }
        }
    }

    @Test
    fun occurrencesAreRealDaysAndInTheSameHebrewYear() {
        for (month in listOf(cheshvan, kislev)) for (target in 5780..5800) {
            for (missing in YahrzeitMissingDay.entries) for (death in listOf(0, 5770, 5771, 5772)) {
                for (occurrence in yahrzeit(month, death, target, missing)) {
                    assertTrue(occurrence.day <= days(target, occurrence.month))
                }
            }
        }
    }

    @Test
    fun nonYahrzeitEventsIgnoreTheSetting() {
        for (month in listOf(cheshvan, kislev)) for (stored in listOf(0, 5770, 5771, 5780)) {
            for (target in 5780..5800) for (missing in YahrzeitMissingDay.entries) {
                // Current behaviour: day 30 only. A 29-day month has no day 30 for
                // the calendar to ask about, so nothing is shown that year.
                assertEquals(
                    listOf(Occurrence(month, 30)),
                    HebrewDateMatcher.occurrences(30, month, stored, false, target, rules(missing))
                )
                assertEquals(
                    false,
                    HebrewDateMatcher.matchesDate(30, month, stored, false, target, month, 29, rules(missing)) ||
                        HebrewDateMatcher.matchesDate(30, month, stored, false, target, nextMonth(month), 1, rules(missing))
                )
            }
        }
    }

    @Test
    fun otherDaysOfCheshvanAndKislevAreUnaffected() {
        for (month in listOf(cheshvan, kislev)) for (day in listOf(1, 15, 29)) {
            for (target in 5780..5800) for (missing in YahrzeitMissingDay.entries) {
                assertEquals(
                    listOf(Occurrence(month, day)),
                    HebrewDateMatcher.occurrences(day, month, 5770, true, target, rules(missing))
                )
            }
        }
    }
}
