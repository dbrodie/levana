package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
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
 * The three ways the app asks "which events fall here" (Gregorian-mode day
 * markers, Hebrew-mode day markers, and the day panel / notifications lookup)
 * must agree on where an Adar event falls.
 */
class PersonalEventRepositoryAdarTest {

    private class FakeDao(private val events: List<PersonalEvent>) : PersonalEventDao {
        override fun getAll(): Flow<List<PersonalEvent>> = flowOf(events)
        override suspend fun getAllOnce(): List<PersonalEvent> = events
        override suspend fun getById(id: Long): PersonalEvent? = events.firstOrNull { it.id == id }
        override suspend fun getByHebrewDate(month: Int, day: Int): List<PersonalEvent> =
            error("raw month lookup bypasses the Adar rule and must not be used")
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

    private fun assertFallsIn(event: PersonalEvent, targetYear: Int, expectedMonth: Int) = runBlocking {
        val repository = PersonalEventRepository(FakeDao(listOf(event)))
        val candidates = if (JewishDate(targetYear, JewishDate.TISHREI, 1).isJewishLeapYear) {
            listOf(JewishDate.ADAR, JewishDate.ADAR_II)
        } else {
            listOf(JewishDate.ADAR)
        }
        for (month in candidates) {
            val expected = month == expectedMonth
            val date = gregorian(targetYear, month, event.hebrewDay)
            val label = "${event.title} in $targetYear month $month"

            assertEquals(
                "$label: Hebrew-mode marker",
                expected,
                event.hebrewDay in repository.getEventDaysForHebrewMonth(targetYear, month)
            )
            assertEquals(
                "$label: Gregorian-mode marker",
                expected,
                date in repository.getEventDatesForGregorianMonth(date.year, date.monthValue)
            )
            assertEquals(
                "$label: day panel / notification",
                expected,
                repository.getEventsForDate(date).isNotEmpty()
            )
        }
    }

    private fun event(title: String, month: Int, year: Int, yahrzeit: Boolean = false) = PersonalEvent(
        title = title,
        hebrewDay = 7,
        hebrewMonth = month,
        hebrewYear = year,
        useYahrzeitRules = yahrzeit
    )

    private fun dates(year: Int, month: Int, day: Int) = gregorian(year, month, day)

    /** Each occurrence is found by all three lookups, with its label in the day lookup. */
    private fun assertOccursOn(
        repository: PersonalEventRepository,
        targetYear: Int,
        expected: Map<Pair<Int, Int>, OccurrenceLabel?>,
        alsoCheck: List<Pair<Int, Int>>
    ) = runBlocking {
        for ((monthDay, label) in expected) {
            val (month, day) = monthDay
            val date = dates(targetYear, month, day)
            assertTrue("$monthDay Hebrew-mode", day in repository.getEventDaysForHebrewMonth(targetYear, month))
            assertTrue("$monthDay Gregorian-mode", date in repository.getEventDatesForGregorianMonth(date.year, date.monthValue))
            assertEquals("$monthDay label", listOf(label), repository.getEventsForDate(date).map { it.label })
        }
        for ((month, day) in alsoCheck - expected.keys) {
            val date = dates(targetYear, month, day)
            assertFalse("$month/$day Hebrew-mode", day in repository.getEventDaysForHebrewMonth(targetYear, month))
            assertFalse("$month/$day Gregorian-mode", date in repository.getEventDatesForGregorianMonth(date.year, date.monthValue))
            assertTrue("$month/$day day lookup", repository.getEventsForDate(date).isEmpty())
        }
    }

    @Test
    fun bothAdarsAreTwoLabelledOccurrencesOnEveryPath() {
        val rules = YahrzeitRules(leapYearAdar = YahrzeitLeapYearAdar.BOTH)
        val repository = PersonalEventRepository(
            FakeDao(listOf(event("Yahrzeit", JewishDate.ADAR, 5786, yahrzeit = true).copy(hebrewDay = 15)))
        ) { rules }
        val adarDays = listOf(JewishDate.ADAR to 15, JewishDate.ADAR_II to 15)
        assertOccursOn(
            repository, 5787,
            mapOf(
                (JewishDate.ADAR to 15) to OccurrenceLabel.ADAR_I,
                (JewishDate.ADAR_II to 15) to OccurrenceLabel.ADAR_II
            ),
            adarDays
        )
        // Ordinary year: one unlabelled occurrence
        assertOccursOn(repository, 5788, mapOf((JewishDate.ADAR to 15) to null), listOf(JewishDate.ADAR to 15))
    }

    @Test
    fun firstYearExceptionAppliesOnEveryPath() {
        val rules = YahrzeitRules(firstYear = YahrzeitFirstYear.ADAR_I)
        val repository = PersonalEventRepository(
            FakeDao(listOf(event("Yahrzeit", JewishDate.ADAR, 5786, yahrzeit = true)))
        ) { rules }
        val adarDays = listOf(JewishDate.ADAR to 7, JewishDate.ADAR_II to 7)
        assertOccursOn(repository, 5787, mapOf((JewishDate.ADAR to 7) to null), adarDays)
        assertOccursOn(repository, 5790, mapOf((JewishDate.ADAR_II to 7) to null), adarDays)
    }

    @Test
    fun day30OfAdarIWithTheNisanOptionOnEveryPath() {
        val event = event("Yahrzeit", JewishDate.ADAR, 5784, yahrzeit = true).copy(hebrewDay = 30)
        val checked = listOf(JewishDate.SHEVAT to 30, JewishDate.NISSAN to 1)
        val off = PersonalEventRepository(FakeDao(listOf(event))) { YahrzeitRules() }
        assertOccursOn(off, 5786, mapOf((JewishDate.SHEVAT to 30) to null), checked)
        val on = PersonalEventRepository(FakeDao(listOf(event))) { YahrzeitRules(adarI30AlsoNisan1 = true) }
        assertOccursOn(
            on, 5786,
            mapOf(
                (JewishDate.SHEVAT to 30) to OccurrenceLabel.SHEVAT_30,
                (JewishDate.NISSAN to 1) to OccurrenceLabel.NISAN_1
            ),
            checked
        )
        assertOccursOn(on, 5787, mapOf((JewishDate.ADAR to 30) to null), checked + (JewishDate.ADAR to 30))
    }

    @Test
    fun day30CheshvanObserveBothIsTwoLabelledOccurrencesOnEveryPath() {
        // A target year whose Cheshvan has 29 days, found through KosherJava
        val target = (5780..5800).first { JewishDate(it, JewishDate.CHESHVAN, 1).daysInJewishMonth == 29 }
        val event = event("Yahrzeit", JewishDate.CHESHVAN, 5780, yahrzeit = true).copy(hebrewDay = 30)
        val checked = listOf(JewishDate.CHESHVAN to 29, JewishDate.KISLEV to 1)
        val both = PersonalEventRepository(FakeDao(listOf(event))) {
            YahrzeitRules(missingDay = YahrzeitMissingDay.BOTH)
        }
        assertOccursOn(
            both, target,
            mapOf(
                (JewishDate.CHESHVAN to 29) to OccurrenceLabel.CHESHVAN_29,
                (JewishDate.KISLEV to 1) to OccurrenceLabel.KISLEV_1
            ),
            checked
        )
        val roshChodesh = PersonalEventRepository(FakeDao(listOf(event))) {
            YahrzeitRules(missingDay = YahrzeitMissingDay.ROSH_CHODESH)
        }
        assertOccursOn(roshChodesh, target, mapOf((JewishDate.KISLEV to 1) to null), checked)
    }

    @Test
    fun adarEventsFallInAdarIIInALeapYearOnEveryPath() {
        // 5787 is a leap year, 5786 is not
        for (yahrzeit in listOf(false, true)) {
            assertFallsIn(event("regular-year Adar", JewishDate.ADAR, 5785, yahrzeit), 5787, JewishDate.ADAR_II)
            assertFallsIn(event("unknown-year Adar", JewishDate.ADAR, 0, yahrzeit), 5787, JewishDate.ADAR_II)
            assertFallsIn(event("Adar II", JewishDate.ADAR_II, 5784, yahrzeit), 5787, JewishDate.ADAR_II)
            assertFallsIn(event("explicit Adar I", JewishDate.ADAR, 5784, yahrzeit), 5787, JewishDate.ADAR)

            assertFallsIn(event("regular-year Adar", JewishDate.ADAR, 5785, yahrzeit), 5786, JewishDate.ADAR)
            assertFallsIn(event("Adar II", JewishDate.ADAR_II, 5784, yahrzeit), 5786, JewishDate.ADAR)
            assertFallsIn(event("explicit Adar I", JewishDate.ADAR, 5784, yahrzeit), 5786, JewishDate.ADAR)
        }
    }
}
