package ninja.doskey.app.levana.device

import ninja.doskey.app.levana.domain.model.IsraelMode
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transformLatest

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

    /** The start of the device's next day, on the device clock. */
    val nextMidnight: Instant get() = today.plusDays(1).atStartOfDay(zone).toInstant()

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
 * Israel/Diaspora for [mode]: [IsraelMode.AUTO] is Israel when the device position is in
 * Israel's time zone. The selected city is never consulted.
 */
fun resolveIsInIsrael(mode: IsraelMode, devicePosition: Location): Boolean = when (mode) {
    IsraelMode.ISRAEL -> true
    IsraelMode.DIASPORA -> false
    IsraelMode.AUTO -> devicePosition.timezoneId == ISRAEL_ZONE_ID
}

private const val ISRAEL_ZONE_ID = "Asia/Jerusalem"

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
    /** Signals that the system date, clock or time zone changed (see [systemTimeChanges]). */
    private val systemTimeChanges: Flow<Unit> = emptyFlow(),
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

    /**
     * The device date, kept current: re-read when the preferences (override, position) change,
     * when the system date, clock or time zone changes, and at each device midnight.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayUpdates: Flow<LocalDate> =
        combine(preferences, merge(flowOf(Unit), systemTimeChanges)) { prefs, _ -> prefs }
            .transformLatest { prefs ->
                while (true) {
                    val now = now(prefs)
                    emit(now.today)
                    delay(untilNextDay(now))
                }
            }
            .distinctUntilChanged()

    /** The developer-override shift that makes the device date read [date]; 0 for null. */
    fun devDayShiftFor(date: LocalDate?): Long =
        date?.let { ChronoUnit.DAYS.between(LocalDate.now(systemClock()), it) } ?: 0L

    companion object {
        /** Time from [now] to just after the next device midnight, never under a second. */
        internal fun untilNextDay(now: DeviceNow): Duration {
            val wait = java.time.Duration.between(now.instant, now.nextMidnight).toMillis() + 1_000
            return wait.coerceAtLeast(1_000).milliseconds
        }

        /** The real device clock in the device's current time zone. */
        fun realClock(): Clock = Clock.systemDefaultZone()

        /** The real date, without the developer override; for storing the override itself. */
        fun realToday(): LocalDate = LocalDate.now(realClock())

        /** The device's current time zone. */
        fun realZone(): ZoneId = ZoneId.systemDefault()
    }
}
