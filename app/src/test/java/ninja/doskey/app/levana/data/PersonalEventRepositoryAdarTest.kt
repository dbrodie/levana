package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
import org.junit.Assert.assertEquals
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
