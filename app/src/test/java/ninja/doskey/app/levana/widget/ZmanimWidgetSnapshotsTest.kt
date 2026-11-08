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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZmanimWidgetSnapshotsTest {

    private val snapshots = ZmanimWidgetSnapshots(ZmanimRepository(), CalendarRepository())
    private val todaySnapshots = TodayWidgetSnapshots(ZmanimRepository(), CalendarRepository())
    private val jerusalemZone = ZoneId.of("Asia/Jerusalem")
    private val jerusalem = UserPreferences(
        currentLocation = Location.JERUSALEM,
        isInIsrael = true,
        selectedZmanim = setOf("Alot hashachar", "Netz hachama", "Chatzot hayom", "Shkia", "Tzeit hakochavim", "Chatzot halaylah"),
    )
    private val newYork = Location(40.7128, -74.006, 10.0, "America/New_York", "New York")
    private val tromso = UserPreferences(
        currentLocation = Location(69.6492, 18.9553, 0.0, "Europe/Oslo", "Tromsø"),
        selectedZmanim = setOf("Netz hachama", "Chatzot hayom", "Shkia"),
    )

    private fun at(date: LocalDate, time: String, zone: ZoneId = jerusalemZone): Instant =
        date.atTime(LocalTime.parse(time)).atZone(zone).toInstant()

    private fun device(now: Instant, prefs: UserPreferences, zone: ZoneId = jerusalemZone, shift: Long = 0) =
        DeviceNow(now, zone, prefs.currentLocation, shift)

    private fun ZmanimWidgetSnapshot.state(name: String) = rows.single { it.name == name }.state

    @Test
    fun `rows are the day-details selection in order, past dimmed and next highlighted`() {
        val today = LocalDate.of(2026, 10, 29)
        val snapshot = snapshots.build(jerusalem, device(at(today, "11:00"), jerusalem))
        assertEquals(
            listOf("Alot hashachar", "Netz hachama", "Chatzot hayom", "Shkia", "Tzeit hakochavim", "Chatzot halaylah"),
            snapshot.rows.map { it.name },
        )
        assertEquals(
            listOf(ZmanState.PAST, ZmanState.PAST, ZmanState.NEXT, ZmanState.UPCOMING, ZmanState.UPCOMING, ZmanState.UPCOMING),
            snapshot.rows.map { it.state },
        )
        assertEquals(today, snapshot.today)
        assertEquals("Jerusalem", snapshot.locationName)
    }

    @Test
    fun `the highlight is the Today widget's next zman and the refresh is when it passes`() {
        val today = LocalDate.of(2026, 10, 29)
        val now = device(at(today, "15:00"), jerusalem)
        val snapshot = snapshots.build(jerusalem, now)
        val next = todaySnapshots.build(jerusalem, now).nextZman!!
        val highlighted = snapshot.rows.single { it.state == ZmanState.NEXT }
        assertEquals(next.name, highlighted.name)
        assertEquals(next.at, highlighted.at)
        assertEquals(next.at.toInstant(), snapshot.refreshAt)

        // Once it passes, the highlight moves on
        val later = snapshots.build(jerusalem, device(next.at.toInstant().plusSeconds(60), jerusalem))
        assertEquals(ZmanState.PAST, later.state(next.name))
        assertEquals(1, later.rows.count { it.state == ZmanState.NEXT })
    }

    @Test
    fun `halachic midnight after civil midnight is still ahead late in the evening`() {
        // In Jerusalem in July, Chatzot HaLaylah is about 00:45 on the next civil day
        val today = LocalDate.of(2026, 7, 15)
        val snapshot = snapshots.build(jerusalem, device(at(today, "23:30"), jerusalem))
        val midnight = snapshot.rows.single { it.name == "Chatzot halaylah" }
        assertEquals(today.plusDays(1), midnight.at!!.toLocalDate())
        assertEquals(ZmanState.NEXT, midnight.state)
        assertEquals(ZmanState.PAST, snapshot.state("Tzeit hakochavim"))
        // It is after the device's midnight, so the widget refreshes at midnight first
        assertEquals(today.plusDays(1).atStartOfDay(jerusalemZone).toInstant(), snapshot.refreshAt)
    }

    @Test
    fun `when every zman has passed, nothing is highlighted and the refresh is at midnight`() {
        val prefs = jerusalem.copy(selectedZmanim = setOf("Netz hachama", "Shkia"))
        val today = LocalDate.of(2026, 10, 29)
        val snapshot = snapshots.build(prefs, device(at(today, "22:00"), prefs))
        assertTrue(snapshot.rows.all { it.state == ZmanState.PAST })
        assertEquals(today.plusDays(1).atStartOfDay(jerusalemZone).toInstant(), snapshot.refreshAt)
    }

    @Test
    fun `an empty selection has no rows and refreshes at midnight`() {
        val prefs = jerusalem.copy(selectedZmanim = emptySet())
        val today = LocalDate.of(2026, 10, 29)
        val snapshot = snapshots.build(prefs, device(at(today, "12:00"), prefs))
        assertTrue(snapshot.rows.isEmpty())
        assertEquals(today.plusDays(1).atStartOfDay(jerusalemZone).toInstant(), snapshot.refreshAt)
    }

    @Test
    fun `an uncomputable zman at a polar location stays listed without a time and is not dimmed`() {
        val oslo = ZoneId.of("Europe/Oslo")
        val today = LocalDate.of(2026, 6, 21)
        val snapshot = snapshots.build(tromso, device(at(today, "15:00", oslo), tromso, oslo))
        assertEquals(listOf("Netz hachama", "Chatzot hayom", "Shkia"), snapshot.rows.map { it.name })
        assertNull(snapshot.rows.single { it.name == "Netz hachama" }.at)
        assertNull(snapshot.rows.single { it.name == "Shkia" }.at)
        assertEquals(ZmanState.UPCOMING, snapshot.state("Netz hachama"))
        assertEquals(ZmanState.UPCOMING, snapshot.state("Shkia"))
        assertEquals(ZmanState.PAST, snapshot.state("Chatzot hayom"))
    }

    @Test
    fun `with the developer override the list is the fixed date's and the refresh is on the real clock`() {
        val real = LocalDate.of(2026, 10, 29)
        val override = LocalDate.of(2026, 12, 25)
        val shift = java.time.temporal.ChronoUnit.DAYS.between(real, override)
        val snapshot = snapshots.build(jerusalem, device(at(override, "11:00"), jerusalem, shift = shift))
        assertEquals(override, snapshot.today)
        val chatzot = snapshot.rows.single { it.state == ZmanState.NEXT }
        assertEquals("Chatzot hayom", chatzot.name)
        assertEquals(override, chatzot.at!!.toLocalDate())
        // The as-if Chatzot is set on the real clock, at the same time of day on the real date
        assertEquals(chatzot.at!!.minusDays(shift).toInstant(), snapshot.refreshAt)
    }

    // Owner decision (TASK-5): the widget shows the device position's times, not the selected city's
    @Test
    fun `rows and header are the device position's, whatever city is selected`() {
        val nySelected = jerusalem.copy(
            savedLocations = listOf(SavedLocation("ny", newYork)),
            activeLocationId = "ny",
        )
        val today = LocalDate.of(2026, 10, 29)
        val now = at(today, "12:00")
        assertEquals(
            snapshots.build(jerusalem, device(now, jerusalem)),
            snapshots.build(nySelected, device(now, nySelected)),
        )
    }
}
