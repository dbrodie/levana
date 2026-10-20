package ninja.doskey.app.levana.notifications

import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import ninja.doskey.app.levana.domain.model.UserPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Number of civil days after today for which reminder alarms are planned. Periodic work can be
 * deferred by Doze / app standby, so alarms must not depend on a run happening every day.
 */
internal const val REMINDER_LOOKAHEAD_DAYS = 7

/** An AlarmManager reminder for the civil [date], firing at the absolute [triggerAt]. */
internal sealed interface ReminderAlarm {
    val date: LocalDate
    val triggerAt: Instant

    data class CandleLighting(
        override val date: LocalDate,
        override val triggerAt: Instant,
        val candleLightingTime: LocalTime,
        val end: ObservanceEnd?
    ) : ReminderAlarm

    /** Fires at tzait of [date]; [omerDay] is the count of the Hebrew day starting then. */
    data class OmerEvening(
        override val date: LocalDate,
        override val triggerAt: Instant,
        val omerDay: Int
    ) : ReminderAlarm

    data class OmerMorning(
        override val date: LocalDate,
        override val triggerAt: Instant,
        val omerDay: Int
    ) : ReminderAlarm
}

internal data class ReminderAlarmPlan(
    /** Civil dates covered by this plan; alarms outside [alarms] for these dates are stale. */
    val window: List<LocalDate>,
    /** Alarms whose trigger is still in the future. */
    val alarms: List<ReminderAlarm>,
    /** Today's morning-mode candle-lighting notice whose morning time has already passed. */
    val dueCandleLighting: ReminderAlarm.CandleLighting?,
    /**
     * Still-wanted alarms whose trigger has passed. An earlier run may have scheduled them and
     * Android may not have delivered them yet (e.g. deferred by Doze), so they must be kept.
     */
    val overdue: List<ReminderAlarm> = emptyList()
)

/**
 * Plans the candle-lighting and Omer reminder alarms for the civil dates
 * [today] .. [today] + [lookaheadDays]. Each trigger is resolved in its own date's offset, so
 * DST changes inside the window are handled. Triggers after [now] go to
 * [ReminderAlarmPlan.alarms] and passed ones to [ReminderAlarmPlan.overdue]; the exception is
 * today's morning-mode candle-lighting notice, returned as [ReminderAlarmPlan.dueCandleLighting]
 * so it can still be posted late. Never yields notices for future dates.
 */
internal fun planReminderAlarms(
    today: LocalDate,
    now: Instant,
    deviceZone: ZoneId,
    location: Location,
    prefs: UserPreferences,
    zmanimRepository: ZmanimRepository,
    calendarRepository: CalendarRepository,
    lookaheadDays: Int = REMINDER_LOOKAHEAD_DAYS
): ReminderAlarmPlan {
    val locationZone = ZoneId.of(location.timezoneId)
    val window = (0..lookaheadDays).map { today.plusDays(it.toLong()) }
    val alarms = mutableListOf<ReminderAlarm>()
    val overdue = mutableListOf<ReminderAlarm>()
    var dueCandleLighting: ReminderAlarm.CandleLighting? = null

    for (date in window) {
        if (prefs.notifyCandleLighting) {
            candleLightingAlarm(date, locationZone, location, prefs, zmanimRepository)?.let {
                when {
                    it.triggerAt.isAfter(now) -> alarms += it
                    date == today && prefs.candleLightingNotifyMode == "morning" ->
                        dueCandleLighting = it
                    else -> overdue += it
                }
            }
        }

        if (prefs.notifyOmer) {
            val plan = planOmerReminders(
                date,
                prefs.notifyOmerTzait,
                prefs.notifyOmerMorning,
                calendarRepository
            ) { zmanimRepository.getTzaitTime(date, location) }

            plan.evening?.let { evening ->
                val triggerAt = ZonedDateTime.of(date, evening.tzaitTime, locationZone).toInstant()
                val alarm = ReminderAlarm.OmerEvening(date, triggerAt, evening.omerDay)
                if (triggerAt.isAfter(now)) alarms += alarm else overdue += alarm
            }
            plan.morningOmerDay?.let { omerDay ->
                val morningTime = minutesToTime(prefs.notifyOmerMorningTime)
                val triggerAt = ZonedDateTime.of(date, morningTime, deviceZone).toInstant()
                val alarm = ReminderAlarm.OmerMorning(date, triggerAt, omerDay)
                if (triggerAt.isAfter(now)) alarms += alarm else overdue += alarm
            }
        }
    }

    return ReminderAlarmPlan(window, alarms, dueCandleLighting, overdue)
}

private fun candleLightingAlarm(
    date: LocalDate,
    locationZone: ZoneId,
    location: Location,
    prefs: UserPreferences,
    zmanimRepository: ZmanimRepository
): ReminderAlarm.CandleLighting? {
    if (!zmanimRepository.hasCandles(date, prefs.isInIsrael)) return null
    val candleLightingTime = zmanimRepository.getShabbatInfo(
        date,
        location,
        prefs.candleLightingOffset,
        prefs.isInIsrael
    ).candleLightingTime ?: return null

    // Havdalah after the last of consecutive Shabbat / Yom Tov days
    val end = zmanimRepository.getObservanceEnd(date, location, prefs.isInIsrael)

    val trigger = when (prefs.candleLightingNotifyMode) {
        "morning" -> ZonedDateTime.of(
            date,
            minutesToTime(prefs.candleLightingMorningTime),
            locationZone
        )
        "hours_before" -> ZonedDateTime.of(date, candleLightingTime, locationZone)
            .minusHours(prefs.candleLightingHoursBefore.toLong())
        else -> return null
    }
    return ReminderAlarm.CandleLighting(date, trigger.toInstant(), candleLightingTime, end)
}

private fun minutesToTime(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)
