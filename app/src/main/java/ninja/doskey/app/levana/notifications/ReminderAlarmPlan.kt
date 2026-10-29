package ninja.doskey.app.levana.notifications

import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.DeviceNow
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

    /**
     * Candle lighting on the evening of [date], at [candleLightingAt], which may be after the
     * following civil midnight; [candleLightingTime] is its wall time in the device zone.
     */
    data class CandleLighting(
        override val date: LocalDate,
        override val triggerAt: Instant,
        val candleLightingTime: LocalTime,
        val end: ObservanceEnd?,
        val candleLightingAt: Instant = triggerAt
    ) : ReminderAlarm

    /**
     * Fires at the nightfall of the evening of [date], which may be after the following civil
     * midnight; [omerDay] is the count of the Hebrew day starting then.
     */
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
    val overdue: List<ReminderAlarm> = emptyList(),
    /**
     * The civil date before [window] whose evening Omer and candle-lighting alarms are also
     * covered by this plan: nightfall can fall after midnight, inside the window's first day.
     */
    val previousEvening: LocalDate? = null
)

/**
 * Plans the candle-lighting and Omer reminder alarms for the civil dates from the [device]'s
 * today to [lookaheadDays] after it. Everything follows the device: candle lighting and
 * nightfall at its position, morning times and notice texts in its zone; the selected city
 * plays no part. Triggers are on the device clock (see [DeviceNow.toSystemInstant]). Each is
 * resolved in its own date's offset, so DST changes inside the window are handled. Triggers
 * after the device's now go to [ReminderAlarmPlan.alarms] and passed ones to
 * [ReminderAlarmPlan.overdue]; the exception is today's morning-mode candle-lighting notice,
 * returned as [ReminderAlarmPlan.dueCandleLighting] so it can still be posted late. Never
 * yields notices for future dates. The evening of the day before the window is planned too,
 * as [ReminderAlarmPlan.previousEvening]: its Omer alarm, and its candle-lighting alarm while
 * the candles are still to be lit.
 */
internal fun planReminderAlarms(
    device: DeviceNow,
    prefs: UserPreferences,
    zmanimRepository: ZmanimRepository,
    calendarRepository: CalendarRepository,
    lookaheadDays: Int = REMINDER_LOOKAHEAD_DAYS
): ReminderAlarmPlan {
    val today = device.today
    val now = device.instant
    val deviceZone = device.zone
    val location = device.position
    val start = today
    // The override's fixed date never advances, so later days would never come
    val end = today.plusDays(if (device.devOverride) 0L else lookaheadDays.toLong())
    val window = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    val alarms = mutableListOf<ReminderAlarm>()
    val overdue = mutableListOf<ReminderAlarm>()
    var dueCandleLighting: ReminderAlarm.CandleLighting? = null

    // Yesterday's nightfall can still be ahead when it falls after civil midnight
    val previousEvening = start.minusDays(1)
    if (prefs.notifyOmer && prefs.notifyOmerTzait) {
        planOmerReminders(previousEvening, notifyTzait = true, notifyMorning = false, calendarRepository) {
            zmanimRepository.getTzaitInstant(previousEvening, location)
        }.evening?.let {
            val alarm = ReminderAlarm.OmerEvening(previousEvening, it.tzaitAt, it.omerDay)
            if (alarm.triggerAt.isAfter(now)) alarms += alarm else overdue += alarm
        }
    }
    // ... and so can its candle lighting at nightfall; once lit, its reminder is stale
    if (prefs.notifyCandleLighting) {
        candleLightingAlarm(previousEvening, deviceZone, location, prefs, zmanimRepository)
            ?.takeIf { it.candleLightingAt.isAfter(now) }
            ?.let { if (it.triggerAt.isAfter(now)) alarms += it else overdue += it }
    }

    for (date in window) {
        if (prefs.notifyCandleLighting) {
            candleLightingAlarm(date, deviceZone, location, prefs, zmanimRepository)?.let {
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
            ) { zmanimRepository.getTzaitInstant(date, location) }

            plan.evening?.let { evening ->
                val alarm = ReminderAlarm.OmerEvening(date, evening.tzaitAt, evening.omerDay)
                if (alarm.triggerAt.isAfter(now)) alarms += alarm else overdue += alarm
            }
            plan.morningOmerDay?.let { omerDay ->
                val morningTime = minutesToTime(prefs.notifyOmerMorningTime)
                val triggerAt = ZonedDateTime.of(date, morningTime, deviceZone).toInstant()
                val alarm = ReminderAlarm.OmerMorning(date, triggerAt, omerDay)
                if (triggerAt.isAfter(now)) alarms += alarm else overdue += alarm
            }
        }
    }

    return ReminderAlarmPlan(window, alarms, dueCandleLighting, overdue, previousEvening)
}

private fun candleLightingAlarm(
    date: LocalDate,
    deviceZone: ZoneId,
    location: Location,
    prefs: UserPreferences,
    zmanimRepository: ZmanimRepository
): ReminderAlarm.CandleLighting? {
    if (!zmanimRepository.hasCandles(date, prefs.isInIsrael)) return null
    // The actual instant: at nightfall it can be after midnight, on the next civil date
    val candleLightingAt = zmanimRepository.getCandleLightingInstant(
        date,
        location,
        prefs.candleLightingOffset,
        prefs.isInIsrael
    ) ?: return null

    // Havdalah after the last of consecutive Shabbat / Yom Tov days
    val end = zmanimRepository.getObservanceEnd(date, location, prefs.isInIsrael)

    val trigger = when (prefs.candleLightingNotifyMode) {
        "morning" -> ZonedDateTime.of(
            date,
            minutesToTime(prefs.candleLightingMorningTime),
            deviceZone
        )
        "hours_before" -> candleLightingAt.atZone(deviceZone)
            .minusHours(prefs.candleLightingHoursBefore.toLong())
        else -> return null
    }
    return ReminderAlarm.CandleLighting(
        date,
        trigger.toInstant(),
        candleLightingAt.atZone(deviceZone).toLocalTime(),
        end,
        candleLightingAt
    )
}

/** This plan on the real clock: triggers on [device]'s override date mapped to the real day. */
internal fun ReminderAlarmPlan.onSystemClock(device: DeviceNow): ReminderAlarmPlan {
    if (device.devDayShift == 0L) return this
    fun ReminderAlarm.mapped(): ReminderAlarm = when (this) {
        is ReminderAlarm.CandleLighting -> copy(
            triggerAt = device.toSystemInstant(triggerAt),
            candleLightingAt = device.toSystemInstant(candleLightingAt)
        )
        is ReminderAlarm.OmerEvening -> copy(triggerAt = device.toSystemInstant(triggerAt))
        is ReminderAlarm.OmerMorning -> copy(triggerAt = device.toSystemInstant(triggerAt))
    }
    return copy(
        alarms = alarms.map { it.mapped() },
        dueCandleLighting = dueCandleLighting?.mapped() as ReminderAlarm.CandleLighting?,
        overdue = overdue.map { it.mapped() }
    )
}

private fun minutesToTime(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)
