package ninja.doskey.app.levana.device

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import ninja.doskey.app.levana.data.DevDateOverrideMigration
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun theOverrideIsAnAsIfClockShiftedByWholeDays() {
        prefs.value = prefs.value.copy(devDayShift = 3)
        val now = now()
        assertEquals(LocalDateTime.of(2026, 11, 1, 23, 59), now.dateTime.toLocalDateTime())
        // And it keeps running: two minutes later it is the next as-if day
        clock = Clock.offset(clock, java.time.Duration.ofMinutes(2))
        assertEquals(LocalDate.of(2026, 11, 2), now().today)
    }

    @Test
    fun theOverrideKeepsTheTimeOfDayAcrossADstChange() {
        // US clocks fall back on Sun 1 Nov 2026; 3 days after Thu 29 Oct 23:59 is still 23:59
        prefs.value = prefs.value.copy(devDayShift = 3)
        val now = now()
        assertEquals(LocalTime.of(23, 59), now.dateTime.toLocalTime())
        assertEquals(java.time.ZoneOffset.ofHours(-5), now.dateTime.offset)
        // A trigger at 07:00 on the as-if Monday is set for 07:00 on the real Friday
        val asIfMorning = ZonedDateTime.of(LocalDate.of(2026, 11, 2), LocalTime.of(7, 0), nyZone)
        assertEquals(
            ZonedDateTime.of(LocalDate.of(2026, 10, 30), LocalTime.of(7, 0), nyZone).toInstant(),
            now.toSystemInstant(asIfMorning.toInstant())
        )
    }

    @Test
    fun turningTheOverrideOffRestoresTheRealClock() {
        prefs.value = prefs.value.copy(devDayShift = -10)
        assertEquals(LocalDate.of(2026, 10, 19), now().today)
        prefs.value = prefs.value.copy(devDayShift = 0)
        assertEquals(clock.instant(), now().instant)
    }

    @Test
    fun theShiftForAPickedDateIsMeasuredFromTheRealDate() {
        prefs.value = prefs.value.copy(devDayShift = 5)
        assertEquals(2L, device.devDayShiftFor(LocalDate.of(2026, 10, 31)))
        assertEquals(-29L, device.devDayShiftFor(LocalDate.of(2026, 9, 30)))
        assertEquals(0L, device.devDayShiftFor(null))
    }

    @Test
    fun anOldFixedOverrideDateMigratesToAShift() = runBlocking {
        val old = longPreferencesKey("dev_date_override")
        val shift = longPreferencesKey("dev_clock_shift_days")
        val migration = DevDateOverrideMigration { LocalDate.of(2026, 10, 29) }

        val stored = mutablePreferencesOf(old to LocalDate.of(2026, 11, 3).toEpochDay())
        assertTrue(migration.shouldMigrate(stored))
        val migrated = migration.migrate(stored)
        assertEquals(5L, migrated[shift])
        assertNull(migrated[old])
        assertFalse(migration.shouldMigrate(migrated))

        // An override on the migration day itself is no shift at all
        val sameDay = migration.migrate(mutablePreferencesOf(old to LocalDate.of(2026, 10, 29).toEpochDay()))
        assertNull(sameDay[shift])
        assertFalse(migration.shouldMigrate(mutablePreferencesOf()))
    }

    @Test
    fun theRealClockIsUnshifted() {
        val before = Instant.now()
        val real = DeviceContext.realClock().instant()
        assertTrue(!real.isBefore(before))
    }
}
