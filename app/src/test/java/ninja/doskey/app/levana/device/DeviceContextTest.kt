package ninja.doskey.app.levana.device

import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class DeviceContextTest {

    private val nyZone = ZoneId.of("America/New_York")
    private val newYork = Location(40.7128, -74.0060, 0.0, nyZone.id, "New York")
    private val prefs = MutableStateFlow(
        UserPreferences(
            currentLocation = newYork,
            savedLocations = listOf(SavedLocation("j", Location.JERUSALEM)),
            activeLocationId = "j"
        )
    )
    private var clock: Clock = fixedClock(LocalDate.of(2026, 10, 29), LocalTime.of(23, 59), nyZone)
    private val device = DeviceContext(prefs) { clock }

    private fun now() = runBlocking { device.now() }

    @Test
    fun withoutTheOverrideItIsTheSystemClockZoneAndPosition() {
        val now = now()
        assertEquals(clock.instant(), now.instant)
        assertEquals(nyZone, now.zone)
        assertEquals(LocalDate.of(2026, 10, 29), now.today)
        assertEquals(0L, now.devDayShift)
        assertEquals(now.instant, now.toSystemInstant(now.instant))
    }

    @Test
    fun thePositionIsTheDevicesNeverTheSelectedCity() {
        assertEquals(newYork, now().position)
    }

    @Test
    fun todayFollowsTheDeviceZoneAcrossMidnight() {
        // 23:59 in New York is already the next day in Jerusalem; the device date decides
        assertEquals(LocalDate.of(2026, 10, 29), now().today)
        clock = Clock.offset(clock, java.time.Duration.ofMinutes(2))
        assertEquals(LocalDate.of(2026, 10, 30), now().today)
    }

    @Test
    fun aTimeZoneChangeIsSeenOnTheNextRead() {
        clock = clock.withZone(ZoneId.of("Asia/Tokyo"))
        val now = now()
        assertEquals(ZoneId.of("Asia/Tokyo"), now.zone)
        assertEquals(LocalDate.of(2026, 10, 30), now.today)
    }

    @Test
    fun theOverrideIsAFixedDateAtTheRealTimeOfDay() {
        prefs.value = prefs.value.copy(devDateOverride = LocalDate.of(2026, 11, 1))
        val now = now()
        assertEquals(LocalDateTime.of(2026, 11, 1, 23, 59), now.dateTime.toLocalDateTime())
        assertTrue(now.devOverride)
        assertEquals(3L, now.devDayShift)
        // Two minutes later it is past the real midnight: the time moves on, the date does not
        clock = Clock.offset(clock, java.time.Duration.ofMinutes(2))
        val later = now()
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 1), later.dateTime.toLocalDateTime())
        assertEquals(2L, later.devDayShift)
    }

    @Test
    fun overrideTriggersMapToTheSameTimeOnTheRealDay() {
        // US clocks fall back on Sun 1 Nov 2026; a 07:00 trigger on that date is set for 07:00
        // on the real Thursday, whatever the offsets
        prefs.value = prefs.value.copy(devDateOverride = LocalDate.of(2026, 11, 1))
        val now = now()
        val asIfMorning = ZonedDateTime.of(LocalDate.of(2026, 11, 1), LocalTime.of(7, 0), nyZone)
        assertEquals(
            ZonedDateTime.of(LocalDate.of(2026, 10, 29), LocalTime.of(7, 0), nyZone).toInstant(),
            now.toSystemInstant(asIfMorning.toInstant())
        )
    }

    @Test
    fun anOverrideOnTheRealDateIsStillOnAndStaysThere() {
        prefs.value = prefs.value.copy(devDateOverride = LocalDate.of(2026, 10, 29))
        assertTrue(now().devOverride)
        assertEquals(0L, now().devDayShift)
        clock = Clock.offset(clock, java.time.Duration.ofMinutes(2))
        assertEquals(LocalDate.of(2026, 10, 29), now().today)
    }

    @Test
    fun turningTheOverrideOffRestoresTheRealClock() {
        prefs.value = prefs.value.copy(devDateOverride = LocalDate.of(2026, 10, 19))
        assertEquals(LocalDate.of(2026, 10, 19), now().today)
        prefs.value = prefs.value.copy(devDateOverride = null)
        assertEquals(clock.instant(), now().instant)
        assertFalse(now().devOverride)
    }

    @Test
    fun theRealClockIsUnshifted() {
        val before = Instant.now()
        val real = DeviceContext.realClock().instant()
        assertTrue(!real.isBefore(before))
    }
}
