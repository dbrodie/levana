package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import ninja.doskey.app.levana.domain.model.Occurrence
import ninja.doskey.app.levana.domain.model.YahrzeitFirstYear
import ninja.doskey.app.levana.domain.model.YahrzeitLeapYearAdar
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import ninja.doskey.app.levana.domain.model.YahrzeitRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The agenda's range lookup lists exactly the days the day panel's per-day lookup
 * ([HebrewDateMatcher.occurrenceOn]) finds, with the same labels, across leap and ordinary
 * years, day 30 and the yahrzeit Adar rules.
 */
class OccurrencesBetweenTest {

    private data class Stored(val day: Int, val month: Int, val year: Int, val yahrzeit: Boolean)

    private val cases = listOf(
        Stored(1, JewishDate.TISHREI, 5750, false),
        Stored(30, JewishDate.CHESHVAN, 5780, false),
        Stored(30, JewishDate.CHESHVAN, 5780, true),
        Stored(30, JewishDate.KISLEV, 5784, false),
        Stored(30, JewishDate.KISLEV, 5784, true),
        Stored(30, JewishDate.ELUL, 0, false),
        Stored(15, JewishDate.ADAR, 0, false),
        Stored(15, JewishDate.ADAR, 0, true),
        Stored(14, JewishDate.ADAR, 5784, false),
        Stored(14, JewishDate.ADAR, 5784, true),
        Stored(30, JewishDate.ADAR, 5784, true),
        Stored(10, JewishDate.ADAR_II, 5784, true),
        Stored(10, JewishDate.ADAR, 5786, true),
        Stored(23, JewishDate.CHESHVAN, 5770, true),
    )

    private val ruleSets = listOf(
        YahrzeitRules(),
        YahrzeitRules(
            leapYearAdar = YahrzeitLeapYearAdar.BOTH,
            firstYear = YahrzeitFirstYear.BOTH,
            missingDay = YahrzeitMissingDay.BOTH,
            adarI30AlsoNisan1 = true
        ),
        YahrzeitRules(leapYearAdar = YahrzeitLeapYearAdar.ADAR_I, missingDay = YahrzeitMissingDay.FIRST_ANNIVERSARY),
    )

    private val start = LocalDate.of(2023, 1, 1)
    private val end = LocalDate.of(2029, 12, 31)

    private fun perDay(stored: Stored, rules: YahrzeitRules): List<Pair<LocalDate, Occurrence>> =
        generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.mapNotNull { date ->
            val jd = JewishDate(date)
            HebrewDateMatcher.occurrenceOn(
                stored.day, stored.month, stored.year, stored.yahrzeit,
                jd.jewishYear, jd.jewishMonth, jd.jewishDayOfMonth, rules
            )?.let { date to it }
        }.toList()

    @Test
    fun `the range lookup matches the per-day lookup`() {
        for (stored in cases) {
            for (rules in ruleSets) {
                val expected = perDay(stored, rules)
                val actual = HebrewDateMatcher.occurrencesBetween(
                    stored.day, stored.month, stored.year, stored.yahrzeit, start, end, rules
                )
                assertEquals("$stored with $rules", expected, actual)
                assertTrue("$stored recurs", expected.size >= 6)
            }
        }
    }

    @Test
    fun `only days within the range are listed`() {
        val from = LocalDate.of(2026, 10, 30)
        val to = LocalDate.of(2026, 11, 3)
        // 23 Cheshvan 5787 is Tuesday 3 November 2026
        val actual = HebrewDateMatcher.occurrencesBetween(23, JewishDate.CHESHVAN, 5770, true, from, to)
        assertEquals(listOf(LocalDate.of(2026, 11, 3)), actual.map { it.first })
        assertEquals(emptyList<Any>(), HebrewDateMatcher.occurrencesBetween(23, JewishDate.CHESHVAN, 5770, true, to, from))
    }
}
