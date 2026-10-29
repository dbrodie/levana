package ninja.doskey.app.levana.notifications

import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.DeviceNow
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Plans for a device at [devicePosition] in [deviceZone] whose clock reads [now]; [today] is
 * stated for readability and must be that moment's device date.
 */
internal fun planReminderAlarms(
    today: LocalDate,
    now: Instant,
    deviceZone: ZoneId,
    devicePosition: Location,
    prefs: UserPreferences,
    zmanimRepository: ZmanimRepository,
    calendarRepository: CalendarRepository,
    lookaheadDays: Int = REMINDER_LOOKAHEAD_DAYS
): ReminderAlarmPlan {
    val device = DeviceNow(now, deviceZone, devicePosition)
    require(device.today == today) { "$now in $deviceZone is ${device.today}, not $today" }
    return planReminderAlarms(device, prefs, zmanimRepository, calendarRepository, lookaheadDays)
}
