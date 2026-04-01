package com.levana.app.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.levana.app.MainActivity
import com.levana.app.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object NotificationPoster {

    const val EXTRA_DATE_EPOCH_DAY = "EXTRA_DATE_EPOCH_DAY"

    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

    private const val CHANNEL_CANDLE_LIGHTING_ORDINAL = 0
    private const val CHANNEL_HOLIDAYS_ORDINAL = 1
    private const val CHANNEL_FASTS_ORDINAL = 2
    private const val CHANNEL_PERSONAL_EVENTS_ORDINAL = 3
    private const val CHANNEL_OMER_ORDINAL = 4

    private fun notificationId(channelOrdinal: Int, date: LocalDate): Int {
        return channelOrdinal * 10000 + date.dayOfYear
    }

    private fun deepLinkIntent(context: Context, date: LocalDate): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            date.toEpochDay().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postCandleLighting(
        context: Context,
        date: LocalDate,
        candleLightingTime: LocalTime,
        endTime: LocalTime?
    ) {
        val timeStr = candleLightingTime.format(timeFormat)
        val body = if (endTime != null) {
            "Candle Lighting at $timeStr \u00b7 Shabbat ends ${endTime.format(timeFormat)}"
        } else {
            "Candle Lighting at $timeStr"
        }

        post(
            context,
            NotificationChannels.CANDLE_LIGHTING,
            notificationId(CHANNEL_CANDLE_LIGHTING_ORDINAL, date),
            "Candle Lighting",
            body,
            date
        )
    }

    fun postHoliday(context: Context, date: LocalDate, holidayName: String, daysUntil: Int) {
        val body = when (daysUntil) {
            0 -> "Today is $holidayName"
            1 -> "$holidayName begins tomorrow"
            else -> "$holidayName begins in $daysUntil days"
        }

        post(
            context,
            NotificationChannels.HOLIDAYS,
            notificationId(CHANNEL_HOLIDAYS_ORDINAL, date),
            "Holiday",
            body,
            date
        )
    }

    fun postFast(
        context: Context,
        fastDate: LocalDate,
        fastName: String,
        startTime: LocalTime?,
        endTime: LocalTime?
    ) {
        val body = buildString {
            append(fastName)
            if (startTime != null) {
                append(" \u00b7 Begins ${startTime.format(timeFormat)}")
            }
            if (endTime != null) {
                append(" \u00b7 Ends ${endTime.format(timeFormat)}")
            }
        }

        post(
            context,
            NotificationChannels.FASTS,
            notificationId(CHANNEL_FASTS_ORDINAL, fastDate),
            "Fast Day",
            body,
            fastDate
        )
    }

    fun postPersonalEvent(context: Context, date: LocalDate, eventTitle: String) {
        post(
            context,
            NotificationChannels.PERSONAL_EVENTS,
            notificationId(CHANNEL_PERSONAL_EVENTS_ORDINAL, date) + eventTitle.hashCode() % 1000,
            "Personal Event",
            "Today is $eventTitle",
            date
        )
    }

    fun postOmer(context: Context, date: LocalDate, omerDay: Int) {
        val weeks = omerDay / 7
        val remaining = omerDay % 7
        val body = if (weeks == 0) {
            "Tonight is day $omerDay of the Omer"
        } else if (remaining == 0) {
            "Tonight is day $omerDay of the Omer ($weeks weeks)"
        } else {
            "Tonight is day $omerDay of the Omer ($weeks weeks and $remaining days)"
        }

        post(
            context,
            NotificationChannels.OMER,
            notificationId(CHANNEL_OMER_ORDINAL, date),
            "Sefirat HaOmer",
            body,
            date
        )
    }

    fun postOmerMorning(context: Context, date: LocalDate, omerDay: Int) {
        val weeks = omerDay / 7
        val remaining = omerDay % 7
        val body = if (weeks == 0) {
            "Today is day $omerDay of the Omer"
        } else if (remaining == 0) {
            "Today is day $omerDay of the Omer ($weeks weeks)"
        } else {
            "Today is day $omerDay of the Omer ($weeks weeks and $remaining days)"
        }

        post(
            context,
            NotificationChannels.OMER,
            notificationId(CHANNEL_OMER_ORDINAL, date) + 1,
            "Sefirat HaOmer",
            body,
            date
        )
    }

    private fun post(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        date: LocalDate
    ) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(deepLinkIntent(context, date))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
