package ninja.doskey.app.levana.data

import android.Manifest
import android.app.Application
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone
import ninja.doskey.app.levana.data.SystemCalendarRepositoryTest.FakeCalendarProvider
import ninja.doskey.app.levana.data.SystemCalendarRepositoryTest.Row
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Independent verifier boundary cases. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SystemCalendarVerifierTest {

    private val cal = 3L
    private val originalZone = TimeZone.getDefault()
    private lateinit var repo: SystemCalendarRepository

    @Before
    fun setUp() {
        FakeCalendarProvider.rows.clear()
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
        Robolectric.setupContentProvider(FakeCalendarProvider::class.java, CalendarContract.AUTHORITY)
        repo = SystemCalendarRepository(app)
    }

    @After
    fun tearDown() = TimeZone.setDefault(originalZone)

    private fun zone(id: String) = TimeZone.setDefault(TimeZone.getTimeZone(id))
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private fun utc(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    private fun local(dt: LocalDateTime) = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun allDay(id: Long, color: Int, first: LocalDate, days: Long) {
        FakeCalendarProvider.rows += Row(id, utc(first), utc(first.plusDays(days)), true, cal, color)
    }

    private fun timed(id: Long, color: Int, begin: Long, end: Long) {
        FakeCalendarProvider.rows += Row(id, begin, end, false, cal, color)
    }

    private suspend fun range(a: LocalDate, b: LocalDate) = repo.getEventColorsForDateRange(a, b, setOf(cal))
    private suspend fun daysOf(a: LocalDate, b: LocalDate): Set<LocalDate> =
        generateSequence(a) { it.plusDays(1) }.takeWhile { !it.isAfter(b) }.toSet()

    @Test
    fun `all-day keeps date at extreme offsets plus14 and minus11`() = runBlocking<Unit> {
        for (z in listOf("Pacific/Kiritimati", "Pacific/Pago_Pago", "Asia/Jerusalem", "America/Los_Angeles")) {
            zone(z)
            FakeCalendarProvider.rows.clear()
            allDay(1, RED, d(2026, 10, 1), 1)
            allDay(2, BLUE, d(2026, 10, 31), 1)
            assertEquals(z, setOf(d(2026, 10, 1), d(2026, 10, 31)), range(d(2026, 10, 1), d(2026, 10, 31)).keys)
            assertEquals(z, 1L, repo.getEventsForDate(d(2026, 10, 1), setOf(cal)).single().eventId)
            assertTrue(z, repo.getEventsForDate(d(2026, 9, 30), setOf(cal)).isEmpty())
            assertTrue(z, repo.getEventsForDate(d(2026, 10, 2), setOf(cal)).isEmpty())
            assertTrue(z, range(d(2026, 11, 1), d(2026, 11, 30)).isEmpty())
            assertTrue(z, range(d(2026, 9, 1), d(2026, 9, 30)).isEmpty())
        }
    }

    @Test
    fun `all-day event starting months before the queried month marks the whole month`() = runBlocking<Unit> {
        zone("Pacific/Pago_Pago")
        allDay(1, RED, d(2026, 8, 15), 100) // Aug 15 .. Nov 22 inclusive
        assertEquals(daysOf(d(2026, 10, 1), d(2026, 10, 31)), range(d(2026, 10, 1), d(2026, 10, 31)).keys)
        assertEquals(daysOf(d(2026, 11, 1), d(2026, 11, 22)), range(d(2026, 11, 1), d(2026, 11, 30)).keys)
    }

    @Test
    fun `six-week grid range spanning three months`() = runBlocking<Unit> {
        zone("Pacific/Kiritimati")
        allDay(1, RED, d(2026, 9, 27), 2)
        allDay(2, BLUE, d(2026, 11, 7), 1)
        assertEquals(
            setOf(d(2026, 9, 27), d(2026, 9, 28), d(2026, 11, 7)),
            range(d(2026, 9, 27), d(2026, 11, 7)).keys
        )
    }

    @Test
    fun `recurring all-day and timed instances stay distinct and colours dedupe and cap`() = runBlocking<Unit> {
        zone("America/New_York")
        for (w in listOf(4, 11, 18)) allDay(10, RED, d(2026, 10, w), 1)
        timed(11, RED, local(LocalDateTime.of(2026, 10, 11, 9, 0)), local(LocalDateTime.of(2026, 10, 11, 10, 0)))
        timed(12, BLUE, local(LocalDateTime.of(2026, 10, 11, 9, 0)), local(LocalDateTime.of(2026, 10, 11, 10, 0)))
        timed(13, GREEN, local(LocalDateTime.of(2026, 10, 11, 9, 0)), local(LocalDateTime.of(2026, 10, 11, 10, 0)))
        timed(14, BLACK, local(LocalDateTime.of(2026, 10, 11, 9, 0)), local(LocalDateTime.of(2026, 10, 11, 10, 0)))
        val c = range(d(2026, 10, 1), d(2026, 10, 31))
        assertEquals(setOf(d(2026, 10, 4), d(2026, 10, 11), d(2026, 10, 18)), c.keys)
        assertEquals(listOf(RED, BLUE, GREEN), c[d(2026, 10, 11)])
        assertEquals(5, repo.getEventsForDate(d(2026, 10, 11), setOf(cal)).size)
    }

    @Test
    fun `timed event across DST fall-back night`() = runBlocking<Unit> {
        zone("America/New_York")
        // 2026-11-01 DST ends in NY.
        timed(1, RED, local(LocalDateTime.of(2026, 10, 31, 23, 0)), local(LocalDateTime.of(2026, 11, 2, 0, 0)))
        assertEquals(setOf(d(2026, 10, 31)), range(d(2026, 10, 1), d(2026, 10, 31)).keys)
        assertEquals(setOf(d(2026, 11, 1)), range(d(2026, 11, 1), d(2026, 11, 30)).keys)
        val e = repo.getEventsForDate(d(2026, 11, 1), setOf(cal)).single()
        assertEquals(LocalDateTime.of(2026, 10, 31, 23, 0), e.startTime)
        assertEquals(LocalDateTime.of(2026, 11, 2, 0, 0), e.endTime)
        assertTrue(repo.getEventsForDate(d(2026, 11, 2), setOf(cal)).isEmpty())
    }

    @Test
    fun `timed event ending at start of a day whose midnight is skipped by DST`() = runBlocking<Unit> {
        // Cuba springs forward at 00:00 -> 01:00 on 2026-03-08; start of day is 01:00.
        zone("America/Havana")
        val endOfDay = d(2026, 3, 8).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val begin = local(LocalDateTime.of(2026, 3, 7, 22, 0))
        timed(1, RED, begin, endOfDay)
        assertEquals(setOf(d(2026, 3, 7)), range(d(2026, 3, 1), d(2026, 3, 31)).keys)
        assertTrue(repo.getEventsForDate(d(2026, 3, 8), setOf(cal)).isEmpty())
        assertEquals(1, repo.getEventsForDate(d(2026, 3, 7), setOf(cal)).size)

        // One millisecond past the shifted start of day does cover 2026-03-08.
        FakeCalendarProvider.rows.clear()
        timed(1, RED, begin, endOfDay + 1)
        assertEquals(setOf(d(2026, 3, 7), d(2026, 3, 8)), range(d(2026, 3, 1), d(2026, 3, 31)).keys)
        assertEquals(1, repo.getEventsForDate(d(2026, 3, 8), setOf(cal)).size)

        // One millisecond before it stays on 2026-03-07 only.
        FakeCalendarProvider.rows.clear()
        timed(1, RED, begin, endOfDay - 1)
        assertEquals(setOf(d(2026, 3, 7)), range(d(2026, 3, 1), d(2026, 3, 31)).keys)
    }

    @Test
    fun `ordinary midnight end exact and just after in New York`() = runBlocking<Unit> {
        zone("America/New_York")
        val begin = local(LocalDateTime.of(2026, 10, 10, 22, 0))
        val midnight = local(LocalDateTime.of(2026, 10, 11, 0, 0))
        timed(1, RED, begin, midnight)
        assertEquals(setOf(d(2026, 10, 10)), range(d(2026, 10, 1), d(2026, 10, 31)).keys)
        FakeCalendarProvider.rows.clear()
        timed(1, RED, begin, midnight + 1)
        assertEquals(setOf(d(2026, 10, 10), d(2026, 10, 11)), range(d(2026, 10, 1), d(2026, 10, 31)).keys)
    }

    @Test
    fun `all-day exclusive UTC end unaffected by device zone with midnight DST`() = runBlocking<Unit> {
        zone("America/Havana")
        allDay(1, RED, d(2026, 3, 7), 2)
        assertEquals(setOf(d(2026, 3, 7), d(2026, 3, 8)), range(d(2026, 3, 1), d(2026, 3, 31)).keys)
        assertTrue(repo.getEventsForDate(d(2026, 3, 9), setOf(cal)).isEmpty())
        val e = repo.getEventsForDate(d(2026, 3, 8), setOf(cal)).single()
        assertEquals(LocalDateTime.of(2026, 3, 7, 0, 0), e.startTime)
        assertEquals(LocalDateTime.of(2026, 3, 9, 0, 0), e.endTime)
    }

    @Test
    fun `timed event entirely in the widened margin is not reported`() = runBlocking<Unit> {
        zone("Asia/Jerusalem")
        timed(1, RED, local(LocalDateTime.of(2026, 9, 30, 10, 0)), local(LocalDateTime.of(2026, 9, 30, 11, 0)))
        timed(2, BLUE, local(LocalDateTime.of(2026, 11, 1, 0, 0)), local(LocalDateTime.of(2026, 11, 1, 1, 0)))
        timed(3, GREEN, local(LocalDateTime.of(2026, 9, 30, 23, 0)), local(LocalDateTime.of(2026, 10, 1, 0, 0)))
        assertTrue(range(d(2026, 10, 1), d(2026, 10, 31)).isEmpty())
        assertTrue(repo.getEventsForDate(d(2026, 10, 1), setOf(cal)).isEmpty())
    }

    @Test
    fun `timed event starting exactly at local midnight of first day`() = runBlocking<Unit> {
        zone("Asia/Tokyo")
        timed(1, RED, local(LocalDateTime.of(2026, 10, 1, 0, 0)), local(LocalDateTime.of(2026, 10, 1, 0, 30)))
        assertEquals(setOf(d(2026, 10, 1)), range(d(2026, 10, 1), d(2026, 10, 31)).keys)
        assertEquals(1, repo.getEventsForDate(d(2026, 10, 1), setOf(cal)).size)
        assertTrue(repo.getEventsForDate(d(2026, 9, 30), setOf(cal)).isEmpty())
    }

    private companion object {
        const val RED = 0xFFFF0000.toInt()
        const val BLUE = 0xFF0000FF.toInt()
        const val GREEN = 0xFF00FF00.toInt()
        const val BLACK = 0xFF000000.toInt()
    }
}
