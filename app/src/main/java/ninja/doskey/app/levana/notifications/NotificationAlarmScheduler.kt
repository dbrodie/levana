package ninja.doskey.app.levana.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotificationAlarmScheduler internal constructor(
    private val context: Context,
    private val registry: ScheduledAlarmRegistry,
    // The real clock: AlarmManager triggers are real instants (see DeviceNow.toSystemInstant)
    private val now: () -> Long = { DeviceContext.realClock().millis() }
) {

    constructor(context: Context, now: () -> Long = { DeviceContext.realClock().millis() }) :
        this(context, ScheduledAlarmRegistry(context), now)

    private val mutex = Mutex()

    companion object {
        const val ACTION_CANDLE_LIGHTING = "ninja.doskey.app.levana.CANDLE_LIGHTING_ALARM"
        const val ACTION_OMER = "ninja.doskey.app.levana.OMER_ALARM"
        const val ACTION_OMER_MORNING = "ninja.doskey.app.levana.OMER_MORNING_ALARM"

        const val EXTRA_DATE_EPOCH_DAY = "date_epoch_day"
        const val EXTRA_CANDLE_LIGHTING_TIME = "candle_lighting_time"
        const val EXTRA_END_TIME = "end_time"
        /** The last holy day; also the end's date for alarms set by previous versions. */
        const val EXTRA_END_DATE_EPOCH_DAY = "end_date_epoch_day"
        /** The civil date havdalah falls on, after [EXTRA_END_DATE_EPOCH_DAY] past midnight. */
        const val EXTRA_END_CIVIL_DATE_EPOCH_DAY = "end_civil_date_epoch_day"
        const val EXTRA_END_INCLUDES_YOM_TOV = "end_includes_yom_tov"
        const val EXTRA_OMER_DAY = "omer_day"

        private const val REQUEST_CANDLE_LIGHTING = 100000
        private const val REQUEST_OMER = 200000
        private const val REQUEST_OMER_MORNING = 201000

        private const val ALARM_SCHEME = "levana-alarm"
        private val ACTIONS = listOf(ACTION_CANDLE_LIGHTING, ACTION_OMER, ACTION_OMER_MORNING)

        /** Days either side of the given date whose alarms [cancelAll] cancels. */
        private const val CANCEL_WINDOW_DAYS = 7L

        /**
         * Days either side of a plan's window searched once for alarms set before the
         * registry existed. Earlier versions only scheduled inside their own window.
         */
        private const val LEGACY_SWEEP_DAYS = 14L

        private fun requestCode(action: String) = when (action) {
            ACTION_CANDLE_LIGHTING -> REQUEST_CANDLE_LIGHTING
            ACTION_OMER -> REQUEST_OMER
            else -> REQUEST_OMER_MORNING
        }

        /**
         * Identity of the alarm for [action] on [date]. The full date is in the data URI, which
         * is part of PendingIntent identity (extras are not), so alarms for different dates —
         * including the same day of different years — never replace each other, while
         * rescheduling the same reminder for the same date replaces its one pending alarm.
         */
        internal fun alarmUri(action: String, date: LocalDate): Uri =
            Uri.Builder()
                .scheme(ALARM_SCHEME)
                .authority("alarm")
                .appendPath(action.substringAfterLast('.').lowercase())
                .appendPath(date.toString())
                .build()
    }

    private fun schedule(alarm: ReminderAlarm) {
        schedule(actionFor(alarm), alarm.date, alarm.triggerAt) {
            when (alarm) {
                is ReminderAlarm.CandleLighting -> {
                    putExtra(EXTRA_CANDLE_LIGHTING_TIME, alarm.candleLightingTime.toString())
                    putExtra(EXTRA_END_TIME, alarm.end?.time?.toString())
                    alarm.end?.let {
                        putExtra(EXTRA_END_DATE_EPOCH_DAY, it.lastDay.toEpochDay())
                        putExtra(EXTRA_END_CIVIL_DATE_EPOCH_DAY, it.date.toEpochDay())
                        putExtra(EXTRA_END_INCLUDES_YOM_TOV, it.includesYomTov)
                    }
                }
                is ReminderAlarm.OmerEvening -> putExtra(EXTRA_OMER_DAY, alarm.omerDay)
                is ReminderAlarm.OmerMorning -> putExtra(EXTRA_OMER_DAY, alarm.omerDay)
            }
        }
    }

    /**
     * Reconciles every reminder alarm with [plan]: schedules each planned alarm and cancels
     * every other one that may be pending — on any date, not only in the plan's window (e.g.
     * after a date-changing time-zone move), for reminders that were turned off too.
     * Overdue alarms, including today's due candle-lighting notice, are left alone, so one
     * Android has not delivered yet still fires. Use this rather than [cancelAll] followed by a
     * rebuild, which would drop such alarms.
     */
    internal suspend fun scheduleAll(plan: ReminderAlarmPlan) = mutex.withLock {
        val wantedAlarms = plan.alarms + plan.overdue + listOfNotNull(plan.dueCandleLighting)
        val wanted = wantedAlarms.map { it.id() }.toSet()
        val candidates = buildSet {
            addAll(registry.ids())
            // Previous versions kept no registry; their alarms are around the planned dates
            for (date in plan.window + listOfNotNull(plan.previousEvening)) {
                ACTIONS.forEach { add(AlarmId(it, date)) }
            }
            if (!registry.isSwept()) addAll(pendingLegacyAlarms(plan))
        }
        val stale = candidates - wanted
        stale.forEach { cancel(it.action, it.date) }
        registry.remove(stale)
        // Recorded before being set, so a crash cannot leave an alarm the registry misses
        registry.add(wanted)
        plan.alarms.forEach(::schedule)
        registry.markSwept()
    }

    /** Cancels the pending alarm for [alarm]'s reminder and date, if any. */
    internal suspend fun cancel(alarm: ReminderAlarm) = mutex.withLock {
        val id = alarm.id()
        cancel(id.action, id.date)
        registry.remove(listOf(id))
    }

    /** Alarms pending on the dates around [plan]'s window, found without a registry. */
    private fun pendingLegacyAlarms(plan: ReminderAlarmPlan): List<AlarmId> {
        val dates = plan.window + listOfNotNull(plan.previousEvening)
        val first = dates.min().minusDays(LEGACY_SWEEP_DAYS)
        val last = dates.max().plusDays(LEGACY_SWEEP_DAYS)
        return generateSequence(first) { it.plusDays(1) }
            .takeWhile { !it.isAfter(last) }
            .flatMap { date -> ACTIONS.map { AlarmId(it, date) } }
            .filter { isPending(it) }
            .toList()
    }

    private fun isPending(id: AlarmId): Boolean = PendingIntent.getBroadcast(
        context,
        requestCode(id.action),
        alarmIntent(id.action, id.date),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    ) != null

    private fun ReminderAlarm.id() = AlarmId(actionFor(this), date)

    private fun cancel(action: String, date: LocalDate) {
        cancelExisting(context.getSystemService(AlarmManager::class.java),
            legacyAlarmIntent(action), legacyRequestCode(action, date))
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(action),
            alarmIntent(action, date),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun actionFor(alarm: ReminderAlarm): String = when (alarm) {
        is ReminderAlarm.CandleLighting -> ACTION_CANDLE_LIGHTING
        is ReminderAlarm.OmerEvening -> ACTION_OMER
        is ReminderAlarm.OmerMorning -> ACTION_OMER_MORNING
    }

    fun scheduleCandleLighting(
        date: LocalDate,
        triggerTime: LocalTime,
        location: Location,
        candleLightingTime: LocalTime,
        end: ObservanceEnd?
    ) {
        val zoneId = ZoneId.of(location.timezoneId)
        val triggerZoned = ZonedDateTime.of(date, triggerTime, zoneId)
        schedule(ACTION_CANDLE_LIGHTING, date, triggerZoned.toInstant()) {
            putExtra(EXTRA_CANDLE_LIGHTING_TIME, candleLightingTime.toString())
            putExtra(EXTRA_END_TIME, end?.time?.toString())
            end?.let {
                putExtra(EXTRA_END_DATE_EPOCH_DAY, it.lastDay.toEpochDay())
                putExtra(EXTRA_END_CIVIL_DATE_EPOCH_DAY, it.date.toEpochDay())
                putExtra(EXTRA_END_INCLUDES_YOM_TOV, it.includesYomTov)
            }
        }
    }

    fun scheduleOmerReminder(
        date: LocalDate,
        tzaitTime: LocalTime,
        location: Location,
        omerDay: Int
    ) {
        val zoneId = ZoneId.of(location.timezoneId)
        val triggerZoned = ZonedDateTime.of(date, tzaitTime, zoneId)
        schedule(ACTION_OMER, date, triggerZoned.toInstant()) {
            putExtra(EXTRA_OMER_DAY, omerDay)
        }
    }

    fun scheduleOmerMorningReminder(
        date: LocalDate,
        morningTime: LocalTime,
        omerDay: Int,
        deviceZone: ZoneId = DeviceContext.realZone()
    ) {
        val triggerZoned = ZonedDateTime.of(date, morningTime, deviceZone)
        schedule(ACTION_OMER_MORNING, date, triggerZoned.toInstant()) {
            putExtra(EXTRA_OMER_DAY, omerDay)
        }
    }

    private fun schedule(
        action: String,
        date: LocalDate,
        trigger: Instant,
        extras: Intent.() -> Unit
    ) {
        val triggerMillis = trigger.toEpochMilli()

        if (triggerMillis <= now()) return

        val intent = alarmIntent(action, date).apply {
            putExtra(EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
            extras()
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(action),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // An alarm a previous version set for this same reminder would fire it a second time
        cancelExisting(alarmManager, legacyAlarmIntent(action), legacyRequestCode(action, date))
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    /**
     * Cancels the alarms for the days around [around] (alarms are only set for the current
     * date) and every alarm set by a previous version, which used day-of-year request codes.
     */
    fun cancelAll(around: LocalDate) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        for (action in ACTIONS) {
            for (offset in -CANCEL_WINDOW_DAYS..CANCEL_WINDOW_DAYS) {
                cancelExisting(
                    alarmManager,
                    alarmIntent(action, around.plusDays(offset)),
                    requestCode(action)
                )
            }
            for (dayOfYear in 1..366) {
                cancelExisting(
                    alarmManager,
                    legacyAlarmIntent(action),
                    requestCode(action) + dayOfYear
                )
            }
        }
    }

    private fun alarmIntent(action: String, date: LocalDate) =
        Intent(context, NotificationAlarmReceiver::class.java).apply {
            this.action = action
            data = alarmUri(action, date)
        }

    /** Previous versions keyed alarms on `REQUEST_x + dayOfYear` with no data URI. */
    private fun legacyAlarmIntent(action: String) =
        Intent(context, NotificationAlarmReceiver::class.java).apply {
            this.action = action
        }

    private fun legacyRequestCode(action: String, date: LocalDate) =
        requestCode(action) + date.dayOfYear

    private fun cancelExisting(alarmManager: AlarmManager, intent: Intent, requestCode: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
