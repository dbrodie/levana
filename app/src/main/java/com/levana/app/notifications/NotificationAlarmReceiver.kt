package com.levana.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime

class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val dateEpochDay = intent.getLongExtra(
            NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY,
            0L
        )
        val date = LocalDate.ofEpochDay(dateEpochDay)

        when (intent.action) {
            NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING -> {
                val candleLightingTime = intent.getStringExtra(
                    NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME
                )?.let { LocalTime.parse(it) } ?: return

                val endTime = intent.getStringExtra(
                    NotificationAlarmScheduler.EXTRA_END_TIME
                )?.let { LocalTime.parse(it) }

                NotificationPoster.postCandleLighting(
                    context,
                    date,
                    candleLightingTime,
                    endTime
                )
            }
            NotificationAlarmScheduler.ACTION_OMER -> {
                val omerDay = intent.getIntExtra(
                    NotificationAlarmScheduler.EXTRA_OMER_DAY,
                    0
                )
                if (omerDay > 0) {
                    NotificationPoster.postOmer(context, date, omerDay)
                }
            }
        }
    }
}
