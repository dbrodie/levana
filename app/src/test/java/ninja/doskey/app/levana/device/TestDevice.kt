package ninja.doskey.app.levana.device

import ninja.doskey.app.levana.domain.model.UserPreferences
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.flowOf

/** A real (unshifted) device clock fixed at [time] on [date] in [zone]. */
fun fixedClock(
    date: LocalDate,
    time: LocalTime = LocalTime.NOON,
    zone: ZoneId = ZoneId.systemDefault()
): Clock = Clock.fixed(ZonedDateTime.of(date, time, zone).toInstant(), zone)

/** A device context whose clock reads noon on [date], with [prefs] as the preferences. */
fun deviceContextOn(date: LocalDate, prefs: UserPreferences = UserPreferences()): DeviceContext =
    DeviceContext(flowOf(prefs)) { fixedClock(date) }
