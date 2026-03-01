package com.levana.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.levana.app.domain.model.Location
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationAlarmScheduler(private val context: Context) {

    companion object {
        const val ACTION_CANDLE_LIGHTING = "com.levana.app.CANDLE_LIGHTING_ALARM"
        const val ACTION_OMER = "com.levana.app.OMER_ALARM"

        const val EXTRA_DATE_EPOCH_DAY = "date_epoch_day"
        const val EXTRA_CANDLE_LIGHTING_TIME = "candle_lighting_time"
        const val EXTRA_END_TIME = "end_time"
        const val EXTRA_OMER_DAY = "omer_day"

        private const val REQUEST_CANDLE_LIGHTING = 100000
        private const val REQUEST_OMER = 200000
    }

    fun scheduleCandleLighting(
        date: LocalDate,
        triggerTime: LocalTime,
        location: Location,
        candleLightingTime: LocalTime,
        endTime: LocalTime?
    ) {
        val zoneId = ZoneId.of(location.timezoneId)
        val triggerZoned = ZonedDateTime.of(date, triggerTime, zoneId)
        val triggerMillis = triggerZoned.toInstant().toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = ACTION_CANDLE_LIGHTING
            putExtra(EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
            putExtra(EXTRA_CANDLE_LIGHTING_TIME, candleLightingTime.toString())
            putExtra(EXTRA_END_TIME, endTime?.toString())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CANDLE_LIGHTING + date.dayOfYear,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    fun scheduleOmerReminder(
        date: LocalDate,
        sunsetTime: LocalTime,
        location: Location,
        omerDay: Int
    ) {
        val zoneId = ZoneId.of(location.timezoneId)
        val triggerZoned = ZonedDateTime.of(date, sunsetTime, zoneId)
        val triggerMillis = triggerZoned.toInstant().toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = ACTION_OMER
            putExtra(EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
            putExtra(EXTRA_OMER_DAY, omerDay)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_OMER + date.dayOfYear,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    fun cancelAll() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        for (action in listOf(ACTION_CANDLE_LIGHTING, ACTION_OMER)) {
            for (dayOfYear in 1..366) {
                val requestCode = when (action) {
                    ACTION_CANDLE_LIGHTING -> REQUEST_CANDLE_LIGHTING + dayOfYear
                    ACTION_OMER -> REQUEST_OMER + dayOfYear
                    else -> continue
                }
                val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                    this.action = action
                }
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
    }
}
