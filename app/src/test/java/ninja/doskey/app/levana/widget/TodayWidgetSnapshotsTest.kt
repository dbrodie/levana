package ninja.doskey.app.levana.widget

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.DeviceNow
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TodayWidgetSnapshotsTest {

    private val snapshots = TodayWidgetSnapshots(ZmanimRepository(), CalendarRepository())
    private val jerusalem = UserPreferences(currentLocation = Location.JERUSALEM, isInIsrael = true)
    private val newYork = UserPreferences(
        currentLocation = Location(40.7128, -74.006, 10.0, "America/New_York", "New York"),
        isInIsrael = false,
    )
    private val tromso = UserPreferences(
        currentLocation = Location(69.6492, 18.9553, 0.0, "Europe/Oslo", "Tromsø"),
    )

    private fun at(date: LocalDate, time: String, prefs: UserPreferences): Instant =
        date.atTime(LocalTime.parse(time)).atZone(ZoneId.of(prefs.currentLocation.timezoneId)).toInstant()

    // --- Next zman ---

    @Test
    fun `next zman is the next selected time later today`() {
        val today = LocalDate.of(2026, 10, 29)
        val next = snapshots.nextZman(jerusalem, jerusalem.currentLocation, today, at(today, "12:00", jerusalem))
        assertEquals("Sunset", next?.name)
        assertEquals(today, next?.at?.toLocalDate())
    }

    @Test
    fun `next zman rolls to tomorrow's first selected time after the last one passes`() {
        val today = LocalDate.of(2026, 10, 29)
        val next = snapshots.nextZman(jerusalem, jerusalem.currentLocation, today, at(today, "23:00", jerusalem))
        assertEquals("Sunrise", next?.name)
        assertEquals(today.plusDays(1), next?.at?.toLocalDate())
    }

    @Test
    fun `no next zman when nothing is selected`() {
        val today = LocalDate.of(2026, 10, 29)
        val prefs = jerusalem.copy(selectedZmanim = emptySet())
        assertNull(snapshots.nextZman(prefs, prefs.currentLocation, today, at(today, "12:00", prefs)))
    }

    @Test
    fun `no next zman when the selected zman does not occur at high latitude`() {
        val today = LocalDate.of(2026, 6, 21)
        val prefs = tromso.copy(selectedZmanim = setOf("Sunset"))
        assertNull(snapshots.nextZman(prefs, prefs.currentLocation, today, at(today, "12:00", prefs)))
    }

    // --- Shabbat / Yom Tov window ---

    @Test
    fun `weekday shows the coming Shabbat`() {
        val times = snapshots.shabbatTimes(jerusalem, jerusalem.currentLocation, LocalDate.of(2026, 10, 29))!!
        assertEquals(LocalDate.of(2026, 10, 30), times.candleLightingDate)
        assertEquals(LocalDate.of(2026, 10, 31), times.havdalahDate)
        assertNotNull(times.candleLighting)
        assertNotNull(times.havdalah)
    }

    @Test
    fun `Shabbat itself still shows its own times`() {
        val times = snapshots.shabbatTimes(jerusalem, jerusalem.currentLocation, LocalDate.of(2026, 10, 31))!!
        assertEquals(LocalDate.of(2026, 10, 30), times.candleLightingDate)
        assertEquals(LocalDate.of(2026, 10, 31), times.havdalahDate)
    }

    @Test
    fun `Sunday rolls to the next Shabbat`() {
        val times = snapshots.shabbatTimes(jerusalem, jerusalem.currentLocation, LocalDate.of(2026, 11, 1))!!
        assertEquals(LocalDate.of(2026, 11, 6), times.candleLightingDate)
        assertEquals(LocalDate.of(2026, 11, 7), times.havdalahDate)
    }

    @Test
    fun `Rosh Hashanah after Shabbat is one run from Friday to Sunday havdalah`() {
        // Rosh Hashanah 5787 falls on Shabbat 12 and Sunday 13 September 2026
        for (day in 10..13) {
            val times = snapshots.shabbatTimes(jerusalem, jerusalem.currentLocation, LocalDate.of(2026, 9, day))!!
            assertEquals(LocalDate.of(2026, 9, 11), times.candleLightingDate)
            assertEquals(LocalDate.of(2026, 9, 13), times.havdalahDate)
        }
        val after = snapshots.shabbatTimes(jerusalem, jerusalem.currentLocation, LocalDate.of(2026, 9, 14))!!
        assertEquals(LocalDate.of(2026, 9, 18), after.candleLightingDate)
    }

    @Test
    fun `midweek Pesach ends on Yom Tov in Israel but joins Shabbat in the Diaspora`() {
        // 15 Nisan 5786 is Thursday 2 April 2026
        val today = LocalDate.of(2026, 3, 30)
        val israel = snapshots.shabbatTimes(jerusalem, jerusalem.currentLocation, today)!!
        assertEquals(LocalDate.of(2026, 4, 1), israel.candleLightingDate)
        assertEquals(LocalDate.of(2026, 4, 2), israel.havdalahDate)

        val diaspora = snapshots.shabbatTimes(newYork, newYork.currentLocation, today)!!
        assertEquals(LocalDate.of(2026, 4, 1), diaspora.candleLightingDate)
        assertEquals(LocalDate.of(2026, 4, 4), diaspora.havdalahDate)
    }

    @Test
    fun `high latitude keeps the dates when times are missing`() {
        val times = snapshots.shabbatTimes(tromso, tromso.currentLocation, LocalDate.of(2026, 6, 17))!!
        assertEquals(LocalDate.of(2026, 6, 19), times.candleLightingDate)
        assertEquals(LocalDate.of(2026, 6, 20), times.havdalahDate)
        assertNull(times.havdalah)
    }

    // --- Refresh time ---

    @Test
    fun `refresh is at the next zman, or midnight when none is left today`() {
        val today = LocalDate.of(2026, 10, 29)
        val zone = ZoneId.of("Asia/Jerusalem")
        val noon = snapshots.build(jerusalem, device(at(today, "12:00", jerusalem), zone, jerusalem))
        assertEquals(noon.nextZman!!.at.toInstant(), noon.refreshAt)

        val late = snapshots.build(jerusalem, device(at(today, "23:00", jerusalem), zone, jerusalem))
        assertEquals(today.plusDays(1).atStartOfDay(zone).toInstant(), late.refreshAt)
    }

    // --- Device rule (TASK-5) ---

    private fun device(now: Instant, zone: ZoneId, prefs: UserPreferences, shift: Long = 0) =
        DeviceNow(now, zone, prefs.currentLocation, shift)

    @Test
    fun `the date is the device's, in the device zone`() {
        // 23:30 Thursday in New York is Friday in Jerusalem; the widget shows Thursday
        val ny = ZoneId.of("America/New_York")
        val thursday = LocalDate.of(2026, 10, 29)
        val now = thursday.atTime(23, 30).atZone(ny).toInstant()
        val snapshot = snapshots.build(newYork, device(now, ny, newYork))
        assertEquals(thursday, snapshot.today)
        assertEquals(thursday.plusDays(1).atStartOfDay(ny).toInstant(), snapshot.refreshAt)
    }

    @Test
    fun `with the developer override the date shifts and the refresh is on the real clock`() {
        val zone = ZoneId.of("Asia/Jerusalem")
        val real = LocalDate.of(2026, 10, 29)
        // The override moves the device clock three days ahead, still 23:00 there
        val asIf = at(real.plusDays(3), "23:00", jerusalem)
        val snapshot = snapshots.build(jerusalem, device(asIf, zone, jerusalem, shift = 3))
        assertEquals(real.plusDays(3), snapshot.today)
        // As-if midnight is the real midnight three days earlier
        assertEquals(real.plusDays(1).atStartOfDay(zone).toInstant(), snapshot.refreshAt)
    }

    // Owner decision (TASK-5): the widget shows the device position's times, not the selected city's
    @Test
    fun `zman and Shabbat rows are the device position's, whatever city is selected`() {
        val nySelected = jerusalem.copy(
            savedLocations = listOf(SavedLocation("ny", newYork.currentLocation)),
            activeLocationId = "ny",
        )
        val today = LocalDate.of(2026, 10, 29)
        val zone = ZoneId.of("Asia/Jerusalem")
        val now = at(today, "12:00", jerusalem)
        val selected = snapshots.build(nySelected, device(now, zone, nySelected))
        assertEquals(snapshots.build(jerusalem, device(now, zone, jerusalem)), selected)
        assertEquals("Jerusalem", selected.locationName)
        assertEquals(snapshots.shabbatTimes(jerusalem, Location.JERUSALEM, today), selected.shabbat)
    }
}
