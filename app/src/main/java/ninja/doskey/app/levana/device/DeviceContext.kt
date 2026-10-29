package ninja.doskey.app.levana.device

import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * The device's own moment: what the app treats as "now", in the device time zone, at the
 * device position. Every date, every notification's timing and text, and Israel/Diaspora
 * auto-detection are decided from this. The selected (display) city is never part of it;
 * see [ninja.doskey.app.levana.domain.model.displayCity].
 *
 * [instant] already includes [devDayShift], the test-only developer override.
 */
data class DeviceNow(
    val instant: Instant,
    val zone: ZoneId,
    /** The current (GPS) location, or the time-zone fallback city without a fix. */
    val position: Location,
    /** Whole days the developer override moves the clock; 0 when the override is off. */
    val devDayShift: Long = 0
) {
    val dateTime: ZonedDateTime get() = instant.atZone(zone)
    val today: LocalDate get() = dateTime.toLocalDate()

    /** A clock fixed at this moment, for APIs that take a [Clock]. */
    val clock: Clock get() = Clock.fixed(instant, zone)

    /**
     * The real instant at which the device clock reads [deviceInstant]. Alarms are planned on
     * the device clock but set on the real one; the two differ only while the developer
     * override is on.
     */
    fun toSystemInstant(deviceInstant: Instant): Instant =
        if (devDayShift == 0L) deviceInstant
        else deviceInstant.atZone(zone).minusDays(devDayShift).toInstant()
}

/**
 * The one source of now, today, the device time zone and the device position. Everything
 * except the Zmanim screen and the day panel's times reads the device through this.
 *
 * The developer override (test only) is an "as if" clock: the real clock shifted by whole
 * days, so the time of day and midnight roll-overs stay real while the date is moved.
 * [systemClock] is read on every call, so a time-zone change is seen immediately.
 */
class DeviceContext(
    private val preferences: Flow<UserPreferences>,
    private val systemClock: () -> Clock = { realClock() }
) {
    suspend fun now(): DeviceNow = now(preferences.first())

    /** The device moment under [prefs], for callers that have already read them. */
    fun now(prefs: UserPreferences): DeviceNow {
        val clock = systemClock()
        val shift = prefs.devDayShift
        val real = ZonedDateTime.now(clock)
        val instant = if (shift == 0L) real.toInstant() else real.plusDays(shift).toInstant()
        return DeviceNow(instant, clock.zone, prefs.currentLocation, shift)
    }

    suspend fun today(): LocalDate = now().today

    /** The developer-override shift that makes the device date read [date]; 0 for null. */
    fun devDayShiftFor(date: LocalDate?): Long =
        date?.let { ChronoUnit.DAYS.between(LocalDate.now(systemClock()), it) } ?: 0L

    companion object {
        /** The real device clock in the device's current time zone. */
        fun realClock(): Clock = Clock.systemDefaultZone()

        /** The real date, without the developer override; for storing the override itself. */
        fun realToday(): LocalDate = LocalDate.now(realClock())

        /** The device's current time zone. */
        fun realZone(): ZoneId = ZoneId.systemDefault()
    }
}
