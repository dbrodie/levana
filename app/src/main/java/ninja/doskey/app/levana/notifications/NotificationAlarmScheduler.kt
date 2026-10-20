package ninja.doskey.app.levana.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationAlarmScheduler(
    private val context: Context,
    private val now: () -> Long = System::currentTimeMillis
) {

    companion object {
        const val ACTION_CANDLE_LIGHTING = "ninja.doskey.app.levana.CANDLE_LIGHTING_ALARM"
        const val ACTION_OMER = "ninja.doskey.app.levana.OMER_ALARM"
        const val ACTION_OMER_MORNING = "ninja.doskey.app.levana.OMER_MORNING_ALARM"

        const val EXTRA_DATE_EPOCH_DAY = "date_epoch_day"
        const val EXTRA_CANDLE_LIGHTING_TIME = "candle_lighting_time"
        const val EXTRA_END_TIME = "end_time"
        const val EXTRA_END_DATE_EPOCH_DAY = "end_date_epoch_day"
        const val EXTRA_END_INCLUDES_YOM_TOV = "end_includes_yom_tov"
        const val EXTRA_OMER_DAY = "omer_day"

        private const val REQUEST_CANDLE_LIGHTING = 100000
        private const val REQUEST_OMER = 200000
        private const val REQUEST_OMER_MORNING = 201000

        private const val ALARM_SCHEME = "levana-alarm"
        private val ACTIONS = listOf(ACTION_CANDLE_LIGHTING, ACTION_OMER, ACTION_OMER_MORNING)

        /** Days either side of the given date whose alarms [cancelAll] cancels. */
        private const val CANCEL_WINDOW_DAYS = 7L

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

    internal fun schedule(alarm: ReminderAlarm) {
        val trigger = alarm.triggerAt.atZone(ZoneId.systemDefault())
        schedule(actionFor(alarm), alarm.date, trigger) {
            when (alarm) {
                is ReminderAlarm.CandleLighting -> {
                    putExtra(EXTRA_CANDLE_LIGHTING_TIME, alarm.candleLightingTime.toString())
                    putExtra(EXTRA_END_TIME, alarm.end?.time?.toString())
                    alarm.end?.let {
                        putExtra(EXTRA_END_DATE_EPOCH_DAY, it.date.toEpochDay())
                        putExtra(EXTRA_END_INCLUDES_YOM_TOV, it.includesYomTov)
                    }
                }
                is ReminderAlarm.OmerEvening -> putExtra(EXTRA_OMER_DAY, alarm.omerDay)
                is ReminderAlarm.OmerMorning -> putExtra(EXTRA_OMER_DAY, alarm.omerDay)
            }
        }
    }

    /**
     * Reconciles the alarms for [plan]'s window, and the evening Omer alarm of its previous
     * evening: schedules every planned alarm and cancels the ones the plan no longer wants (e.g. the Omer has ended, or a reminder was turned off).
     * Overdue alarms, including today's due candle-lighting notice, are left alone, so one
     * Android has not delivered yet still fires. Use this rather than [cancelAll] followed by a
     * rebuild, which would drop such alarms.
     */
    internal fun scheduleAll(plan: ReminderAlarmPlan) {
        val wanted = (plan.alarms + plan.overdue + listOfNotNull(plan.dueCandleLighting))
            .map { actionFor(it) to it.date }
            .toSet()
        for (date in plan.window) {
            for (action in ACTIONS) {
                if (action to date !in wanted) cancel(action, date)
            }
        }
        plan.previousEvening?.let { date ->
            if (ACTION_OMER to date !in wanted) cancel(ACTION_OMER, date)
        }
        plan.alarms.forEach(::schedule)
    }

    /** Cancels the pending alarm for [alarm]'s reminder and date, if any. */
    internal fun cancel(alarm: ReminderAlarm) = cancel(actionFor(alarm), alarm.date)

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
        schedule(ACTION_CANDLE_LIGHTING, date, triggerZoned) {
            putExtra(EXTRA_CANDLE_LIGHTING_TIME, candleLightingTime.toString())
            putExtra(EXTRA_END_TIME, end?.time?.toString())
            end?.let {
                putExtra(EXTRA_END_DATE_EPOCH_DAY, it.date.toEpochDay())
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
        schedule(ACTION_OMER, date, triggerZoned) {
            putExtra(EXTRA_OMER_DAY, omerDay)
        }
    }

    fun scheduleOmerMorningReminder(
        date: LocalDate,
        morningTime: LocalTime,
        omerDay: Int
    ) {
        val triggerZoned = ZonedDateTime.of(date, morningTime, ZoneId.systemDefault())
        schedule(ACTION_OMER_MORNING, date, triggerZoned) {
            putExtra(EXTRA_OMER_DAY, omerDay)
        }
    }

    private fun schedule(
        action: String,
        date: LocalDate,
        trigger: ZonedDateTime,
        extras: Intent.() -> Unit
    ) {
        val triggerMillis = trigger.toInstant().toEpochMilli()

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
    fun cancelAll(around: LocalDate = LocalDate.now()) {
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
