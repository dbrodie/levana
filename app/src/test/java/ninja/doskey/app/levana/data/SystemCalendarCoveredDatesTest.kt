package ninja.doskey.app.levana.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import ninja.doskey.app.levana.data.SystemCalendarRepository.Companion.coveredDates
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemCalendarCoveredDatesTest {

    private val newYork = ZoneId.of("America/New_York")
    private val tokyo = ZoneId.of("Asia/Tokyo")

    private fun utcMidnight(date: LocalDate) =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun millis(dateTime: LocalDateTime, zone: ZoneId) =
        dateTime.atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `all-day dates are independent of the device zone`() {
        val day = LocalDate.of(2026, 3, 8)
        for (zone in listOf(newYork, tokyo, ZoneOffset.UTC)) {
            assertEquals(
                day..day,
                coveredDates(utcMidnight(day), utcMidnight(day.plusDays(1)), true, zone)
            )
        }
    }

    @Test
    fun `multi-day all-day end is exclusive`() {
        val first = LocalDate.of(2026, 12, 30)
        assertEquals(
            first..LocalDate.of(2027, 1, 1),
            coveredDates(utcMidnight(first), utcMidnight(first.plusDays(3)), true, tokyo)
        )
    }

    @Test
    fun `zero-length events cover their start date`() {
        val day = LocalDate.of(2026, 5, 1)
        assertEquals(day..day, coveredDates(utcMidnight(day), utcMidnight(day), true, newYork))
        val begin = millis(LocalDateTime.of(2026, 5, 1, 23, 30), newYork)
        assertEquals(day..day, coveredDates(begin, begin, false, newYork))
    }

    @Test
    fun `timed end at local midnight is exclusive`() {
        val begin = millis(LocalDateTime.of(2026, 5, 1, 0, 0), tokyo)
        val end = millis(LocalDateTime.of(2026, 5, 3, 0, 0), tokyo)
        assertEquals(
            LocalDate.of(2026, 5, 1)..LocalDate.of(2026, 5, 2),
            coveredDates(begin, end, false, tokyo)
        )
    }

    @Test
    fun `timed events use device zone dates`() {
        // 2026-05-01T20:00 New York == 2026-05-02T09:00 Tokyo.
        val begin = millis(LocalDateTime.of(2026, 5, 1, 20, 0), newYork)
        val end = begin + 60 * 60 * 1000
        assertEquals(
            LocalDate.of(2026, 5, 1)..LocalDate.of(2026, 5, 1),
            coveredDates(begin, end, false, newYork)
        )
        assertEquals(
            LocalDate.of(2026, 5, 2)..LocalDate.of(2026, 5, 2),
            coveredDates(begin, end, false, tokyo)
        )
    }
}
