package ninja.doskey.app.levana.data

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone
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

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SystemCalendarRepositoryTest {

    private val calendarId = 7L
    private val originalZone = TimeZone.getDefault()
    private lateinit var repository: SystemCalendarRepository

    @Before
    fun setUp() {
        FakeCalendarProvider.rows.clear()
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
        Robolectric.setupContentProvider(
            FakeCalendarProvider::class.java,
            CalendarContract.AUTHORITY
        )
        repository = SystemCalendarRepository(app)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun `all-day event keeps its calendar date west of UTC`() = runBlocking<Unit> {
        useZone(NEW_YORK)
        addAllDay(id = 1, color = RED, first = LocalDate.of(2026, 10, 20), days = 1)

        val colors = october()

        assertEquals(mapOf(LocalDate.of(2026, 10, 20) to listOf(RED)), colors)
    }

    @Test
    fun `multi-day all-day event marks every day with exclusive end`() = runBlocking<Unit> {
        useZone(NEW_YORK)
        addAllDay(id = 1, color = RED, first = LocalDate.of(2026, 10, 5), days = 3)

        val colors = october()

        assertEquals(
            listOf(5, 6, 7).map { LocalDate.of(2026, 10, it) }.toSet(),
            colors.keys
        )
    }

    @Test
    fun `timed event starting before the month marks its days inside the month`() = runBlocking<Unit> {
        useZone(NEW_YORK)
        addTimed(
            id = 1, color = RED,
            begin = LocalDateTime.of(2026, 9, 29, 10, 0),
            end = LocalDateTime.of(2026, 10, 2, 9, 0)
        )

        val colors = october()

        assertEquals(
            setOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)),
            colors.keys
        )
    }

    @Test
    fun `all-day event spanning the month boundary marks days in both months`() = runBlocking<Unit> {
        useZone(NEW_YORK)
        addAllDay(id = 1, color = RED, first = LocalDate.of(2026, 10, 30), days = 4)

        val october = october()
        val november = repository.getEventColorsForDateRange(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30),
            setOf(calendarId)
        )

        assertEquals(
            setOf(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31)),
            october.keys
        )
        assertEquals(
            setOf(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2)),
            november.keys
        )
    }

    @Test
    fun `timed event ending exactly at midnight does not mark the next day`() = runBlocking<Unit> {
        useZone(NEW_YORK)
        addTimed(
            id = 1, color = RED,
            begin = LocalDateTime.of(2026, 10, 10, 22, 0),
            end = LocalDateTime.of(2026, 10, 11, 0, 0)
        )

        assertEquals(setOf(LocalDate.of(2026, 10, 10)), october().keys)
    }

    @Test
    fun `timed event ending at a DST-shifted start of day does not mark that day`() = runBlocking<Unit> {
        // Havana springs forward 00:00 -> 01:00 on 2026-03-08; the day starts at 01:00.
        useZone("America/Havana")
        addTimed(
            id = 1, color = RED,
            begin = LocalDateTime.of(2026, 3, 7, 22, 0),
            end = LocalDateTime.of(2026, 3, 8, 1, 0)
        )

        val march = repository.getEventColorsForDateRange(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31),
            setOf(calendarId)
        )

        assertEquals(setOf(LocalDate.of(2026, 3, 7)), march.keys)
        assertTrue(
            repository.getEventsForDate(LocalDate.of(2026, 3, 8), setOf(calendarId))
                .isEmpty()
        )
    }

    @Test
    fun `overnight timed event marks both local days`() = runBlocking<Unit> {
        useZone(NEW_YORK)
        addTimed(
            id = 1, color = RED,
            begin = LocalDateTime.of(2026, 10, 12, 22, 0),
            end = LocalDateTime.of(2026, 10, 13, 1, 0)
        )

        assertEquals(
            setOf(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 13)),
            october().keys
        )
    }

    @Test
    fun `timed evening event uses the device time zone date`() = runBlocking<Unit> {
        useZone(NEW_YORK)
        // 21:00 New York is 01:00 UTC on the next day.
        addTimed(
            id = 1, color = RED,
            begin = LocalDateTime.of(2026, 10, 15, 21, 0),
            end = LocalDateTime.of(2026, 10, 15, 22, 0)
        )

        assertEquals(setOf(LocalDate.of(2026, 10, 15)), october().keys)
    }

    @Test
    fun `recurring instances each mark their own day and colors are merged`() = runBlocking<Unit> {
        useZone(NEW_YORK)
        for (day in listOf(14, 21, 28)) {
            addTimed(
                id = 1, color = RED,
                begin = LocalDateTime.of(2026, 10, day, 9, 0),
                end = LocalDateTime.of(2026, 10, day, 10, 0)
            )
        }
        addAllDay(id = 2, color = BLUE, first = LocalDate.of(2026, 10, 21), days = 1)

        val colors = october()

        assertEquals(listOf(RED), colors[LocalDate.of(2026, 10, 14)])
        assertEquals(setOf(RED, BLUE), colors[LocalDate.of(2026, 10, 21)]?.toSet())
        assertEquals(listOf(RED), colors[LocalDate.of(2026, 10, 28)])
        assertEquals(3, colors.size)
    }

    @Test
    fun `events for date excludes neighbouring all-day events west of UTC`() = runBlocking<Unit> {
        useZone(NEW_YORK)
        addAllDay(id = 1, color = RED, first = LocalDate.of(2026, 10, 20), days = 1)

        val dayBefore = repository.getEventsForDate(
            LocalDate.of(2026, 10, 19), setOf(calendarId)
        )
        val day = repository.getEventsForDate(
            LocalDate.of(2026, 10, 20), setOf(calendarId)
        )

        assertTrue(dayBefore.isEmpty())
        val event = day.single()
        assertTrue(event.allDay)
        assertEquals(LocalDateTime.of(2026, 10, 20, 0, 0), event.startTime)
        assertEquals(LocalDateTime.of(2026, 10, 21, 0, 0), event.endTime)
    }

    @Test
    fun `events for date excludes neighbouring all-day events east of UTC`() = runBlocking<Unit> {
        useZone(JERUSALEM)
        addAllDay(id = 1, color = RED, first = LocalDate.of(2026, 10, 20), days = 1)

        val dayAfter = repository.getEventsForDate(
            LocalDate.of(2026, 10, 21), setOf(calendarId)
        )
        val day = repository.getEventsForDate(
            LocalDate.of(2026, 10, 20), setOf(calendarId)
        )

        assertTrue(dayAfter.isEmpty())
        assertEquals(1L, day.single().eventId)
    }

    @Test
    fun `events for date includes every day of a multi-day timed event`() = runBlocking<Unit> {
        useZone(JERUSALEM)
        addTimed(
            id = 1, color = RED,
            begin = LocalDateTime.of(2026, 10, 5, 18, 0),
            end = LocalDateTime.of(2026, 10, 7, 12, 0)
        )

        for (day in 5..7) {
            val events = repository.getEventsForDate(
                LocalDate.of(2026, 10, day), setOf(calendarId)
            )
            assertEquals("day $day", 1, events.size)
            assertEquals(LocalDateTime.of(2026, 10, 5, 18, 0), events.single().startTime)
        }
        assertTrue(
            repository.getEventsForDate(LocalDate.of(2026, 10, 8), setOf(calendarId))
                .isEmpty()
        )
    }

    private suspend fun october() = repository.getEventColorsForDateRange(
        LocalDate.of(2026, 10, 1),
        LocalDate.of(2026, 10, 31),
        setOf(calendarId)
    )

    private fun useZone(zone: String) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
    }

    private fun addAllDay(id: Long, color: Int, first: LocalDate, days: Long) {
        FakeCalendarProvider.rows += Row(
            eventId = id,
            begin = first.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            end = first.plusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant()
                .toEpochMilli(),
            allDay = true,
            calendarId = calendarId,
            color = color
        )
    }

    private fun addTimed(id: Long, color: Int, begin: LocalDateTime, end: LocalDateTime) {
        val zone = ZoneId.systemDefault()
        FakeCalendarProvider.rows += Row(
            eventId = id,
            begin = begin.atZone(zone).toInstant().toEpochMilli(),
            end = end.atZone(zone).toInstant().toEpochMilli(),
            allDay = false,
            calendarId = calendarId,
            color = color
        )
    }

    data class Row(
        val eventId: Long,
        val begin: Long,
        val end: Long,
        val allDay: Boolean,
        val calendarId: Long,
        val color: Int
    )

    /** Serves `Instances` rows overlapping the requested window, like the real provider. */
    class FakeCalendarProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            val segments = uri.pathSegments
            val windowStart = segments[segments.size - 2].toLong()
            val windowEnd = segments.last().toLong()
            val cursor = MatrixCursor(projection)
            rows.filter { it.begin <= windowEnd && it.end >= windowStart }
                .sortedBy { it.begin }
                .forEach { row ->
                    cursor.addRow(
                        projection!!.map { column ->
                            when (column) {
                                CalendarContract.Instances.EVENT_ID -> row.eventId
                                CalendarContract.Instances.TITLE -> "Event ${row.eventId}"
                                CalendarContract.Instances.BEGIN -> row.begin
                                CalendarContract.Instances.END -> row.end
                                CalendarContract.Instances.ALL_DAY -> if (row.allDay) 1 else 0
                                CalendarContract.Instances.CALENDAR_ID -> row.calendarId
                                CalendarContract.Instances.CALENDAR_COLOR -> row.color
                                else -> null
                            }
                        }
                    )
                }
            return cursor
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ) = 0

        companion object {
            val rows = mutableListOf<Row>()
        }
    }

    private companion object {
        const val NEW_YORK = "America/New_York"
        const val JERUSALEM = "Asia/Jerusalem"
        const val RED = 0xFFFF0000.toInt()
        const val BLUE = 0xFF0000FF.toInt()
    }
}
