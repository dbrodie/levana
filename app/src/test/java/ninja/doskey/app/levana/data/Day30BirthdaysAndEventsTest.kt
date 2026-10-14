package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.Occurrence
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import ninja.doskey.app.levana.domain.model.YahrzeitRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 1d: a birthday, or a custom event with the yahrzeit switch off, on a
 * day 30 the target month lacks moves to the 1st of the following month.
 *
 * Birthdays and custom events take the same path: ContactBirthdayRepository
 * calls HebrewDateMatcher with useYahrzeitRules = false, exactly as
 * PersonalEventRepository does for a custom event with the switch off. The
 * matcher is tested for both; the event repository is tested end to end.
 */
class Day30BirthdaysAndEventsTest {

    private val years = 5700..5900
    private fun days(year: Int, month: Int) = JewishDate(year, month, 1).daysInJewishMonth
    private fun isLeap(year: Int) = JewishDate(year, JewishDate.TISHREI, 1).isJewishLeapYear

    /** A known leap year (Adar I is month 12 of it). */
    private val leapDeathYear = years.first { isLeap(it) }
    private val ordinaryYear = years.first { !isLeap(it) }

    private fun simple(day: Int, month: Int, storedYear: Int, target: Int) =
        HebrewDateMatcher.occurrences(day, month, storedYear, useYahrzeitRules = false, targetYear = target)

    private fun delayed(to: Int, from: HebrewMonth) =
        listOf(Occurrence(to, 1, OccurrenceLabel.DelayedFrom30(from)))

    @Test
    fun thirtyCheshvan() {
        val short = years.first { days(it, JewishDate.CHESHVAN) == 29 }
        val full = years.first { days(it, JewishDate.CHESHVAN) == 30 }
        for (stored in listOf(0, 5770, 5784)) {
            assertEquals(delayed(JewishDate.KISLEV, HebrewMonth.CHESHVAN), simple(30, JewishDate.CHESHVAN, stored, short))
            assertEquals(listOf(Occurrence(JewishDate.CHESHVAN, 30)), simple(30, JewishDate.CHESHVAN, stored, full))
        }
    }

    @Test
    fun thirtyKislev() {
        val short = years.first { days(it, JewishDate.KISLEV) == 29 }
        val full = years.first { days(it, JewishDate.KISLEV) == 30 }
        for (stored in listOf(0, 5770, 5784)) {
            assertEquals(delayed(JewishDate.TEVES, HebrewMonth.KISLEV), simple(30, JewishDate.KISLEV, stored, short))
            assertEquals(listOf(Occurrence(JewishDate.KISLEV, 30)), simple(30, JewishDate.KISLEV, stored, full))
        }
    }

    @Test
    fun thirtyAdarI() {
        // Adar of an ordinary year has 29 days; Adar I of a leap year has 30
        val ordinaryTarget = years.first { !isLeap(it) }
        val leapTarget = years.first { isLeap(it) && it > leapDeathYear }
        assertEquals(29, days(ordinaryTarget, JewishDate.ADAR))
        assertEquals(30, days(leapTarget, JewishDate.ADAR))
        assertEquals(delayed(JewishDate.NISSAN, HebrewMonth.ADAR_I), simple(30, JewishDate.ADAR, leapDeathYear, ordinaryTarget))
        assertEquals(listOf(Occurrence(JewishDate.ADAR, 30)), simple(30, JewishDate.ADAR, leapDeathYear, leapTarget))
    }

    @Test
    fun anyOtherMissingDay30MovesToTheFirstOfTheNextMonth() {
        val leap = years.first { isLeap(it) }
        val ordinary = years.first { !isLeap(it) }
        for (target in listOf(leap, ordinary)) {
            assertEquals(delayed(JewishDate.SIVAN, HebrewMonth.IYAR), simple(30, JewishDate.IYAR, 0, target))
            assertEquals(delayed(JewishDate.AV, HebrewMonth.TAMMUZ), simple(30, JewishDate.TAMMUZ, 0, target))
            assertEquals(delayed(JewishDate.SHEVAT, HebrewMonth.TEVES), simple(30, JewishDate.TEVES, 0, target))
            assertEquals(delayed(JewishDate.TISHREI, HebrewMonth.ELUL), simple(30, JewishDate.ELUL, 0, target))
            // Adar of an ordinary or unknown year, and Adar II, fall in a 29-day month either way
            for (stored in listOf(0, ordinaryYear)) {
                assertEquals(delayed(JewishDate.NISSAN, HebrewMonth.ADAR), simple(30, JewishDate.ADAR, stored, target))
            }
            assertEquals(delayed(JewishDate.NISSAN, HebrewMonth.ADAR_II), simple(30, JewishDate.ADAR_II, leap, target))
            assertEquals(delayed(JewishDate.NISSAN, HebrewMonth.ADAR_II), simple(30, JewishDate.ADAR_II, 0, target))
        }
    }

    @Test
    fun day30IsNeverDroppedAndEveryOccurrenceIsARealDay() {
        for (month in JewishDate.NISSAN..JewishDate.ADAR_II) {
            for (stored in listOf(0, 5784, 5785)) for (target in 5770..5800) {
                val occurrences = simple(30, month, stored, target)
                assertEquals(1, occurrences.size)
                val occurrence = occurrences.single()
                assertTrue("$month $stored $target $occurrence", occurrence.day <= days(target, occurrence.month))
                // Labelled exactly when moved
                assertEquals(occurrence.day == 1, occurrence.label is OccurrenceLabel.DelayedFrom30)
            }
        }
    }

    @Test
    fun otherDaysAreNeverMovedOrLabelled() {
        for (month in JewishDate.NISSAN..JewishDate.ADAR_II) for (day in 1..29) {
            for (target in listOf(5784, 5785, 5786, 5787)) {
                val occurrence = simple(day, month, 5785, target).single()
                assertEquals(day, occurrence.day)
                assertEquals(null, occurrence.label)
            }
        }
    }

    @Test
    fun yahrzeitEventsAreUnaffected() {
        val shortCheshvan = years.first { days(it, JewishDate.CHESHVAN) == 29 }
        for (missing in YahrzeitMissingDay.entries) {
            val rules = YahrzeitRules(missingDay = missing)
            val result = HebrewDateMatcher.occurrences(30, JewishDate.CHESHVAN, 5770, true, shortCheshvan, rules)
            assertTrue(result.none { it.label is OccurrenceLabel.DelayedFrom30 })
        }
        // 30 Adar I yahrzeit in an ordinary year is 30 Shevat (1b), not 1 Nisan
        assertEquals(
            listOf(Occurrence(JewishDate.SHEVAT, 30)),
            HebrewDateMatcher.occurrences(30, JewishDate.ADAR, leapDeathYear, true, ordinaryYear)
        )
        // Iyar 30 yahrzeit keeps the previous behaviour (no move)
        assertEquals(
            listOf(Occurrence(JewishDate.IYAR, 30)),
            HebrewDateMatcher.occurrences(30, JewishDate.IYAR, 5770, true, 5785)
        )
    }

    // ── Custom events end to end through the repository ─────────────────────

    private class FakeDao(private val events: List<PersonalEvent>) : PersonalEventDao {
        override fun getAll(): Flow<List<PersonalEvent>> = flowOf(events)
        override suspend fun getAllOnce(): List<PersonalEvent> = events
        override suspend fun getById(id: Long): PersonalEvent? = null
        override suspend fun getByHebrewDate(month: Int, day: Int): List<PersonalEvent> = emptyList()
        override suspend fun insert(event: PersonalEvent): Long = 0
        override suspend fun update(event: PersonalEvent) = Unit
        override suspend fun delete(event: PersonalEvent) = Unit
    }

    private fun gregorian(year: Int, month: Int, day: Int): LocalDate {
        val gc = JewishDate(year, month, day).gregorianCalendar
        return LocalDate.of(
            gc.get(java.util.Calendar.YEAR),
            gc.get(java.util.Calendar.MONTH) + 1,
            gc.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    @Test
    fun movedCustomEventIsFoundOnEveryPathWithItsLabel() = runBlocking {
        val event = PersonalEvent(title = "Anniversary", hebrewDay = 30, hebrewMonth = JewishDate.KISLEV, hebrewYear = 5770)
        val repository = PersonalEventRepository(FakeDao(listOf(event)))
        val short = years.first { it > 5780 && days(it, JewishDate.KISLEV) == 29 }
        val full = years.first { it > 5780 && days(it, JewishDate.KISLEV) == 30 }

        // Short Kislev: 1 Teves, labelled; nothing on 29 Kislev
        val teves1 = gregorian(short, JewishDate.TEVES, 1)
        assertTrue(1 in repository.getEventDaysForHebrewMonth(short, JewishDate.TEVES))
        assertTrue(teves1 in repository.getEventDatesForGregorianMonth(teves1.year, teves1.monthValue))
        assertEquals(
            listOf(OccurrenceLabel.DelayedFrom30(HebrewMonth.KISLEV)),
            repository.getEventsForDate(teves1).map { it.label }
        )
        assertFalse(29 in repository.getEventDaysForHebrewMonth(short, JewishDate.KISLEV))
        assertTrue(repository.getEventsForDate(gregorian(short, JewishDate.KISLEV, 29)).isEmpty())

        // Full Kislev: 30 Kislev, no label; nothing on 1 Teves
        val kislev30 = gregorian(full, JewishDate.KISLEV, 30)
        assertTrue(30 in repository.getEventDaysForHebrewMonth(full, JewishDate.KISLEV))
        assertTrue(kislev30 in repository.getEventDatesForGregorianMonth(kislev30.year, kislev30.monthValue))
        assertEquals(listOf(null), repository.getEventsForDate(kislev30).map { it.label })
        assertFalse(1 in repository.getEventDaysForHebrewMonth(full, JewishDate.TEVES))
    }
}
