package ninja.doskey.app.levana.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ninja.doskey.app.levana.MainActivity
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.Holiday
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import ninja.doskey.app.levana.ui.events.occurrenceTitle
import ninja.doskey.app.levana.update.GITHUB_RELEASES_PAGE
import java.time.LocalDate
import java.time.LocalTime

object NotificationPoster {

    const val EXTRA_DATE_EPOCH_DAY = "EXTRA_DATE_EPOCH_DAY"

    private const val CHANNEL_CANDLE_LIGHTING_ORDINAL = 0
    private const val CHANNEL_HOLIDAYS_ORDINAL = 1
    private const val CHANNEL_FASTS_ORDINAL = 2
    private const val CHANNEL_PERSONAL_EVENTS_ORDINAL = 3
    private const val CHANNEL_OMER_ORDINAL = 4
    private const val NOTIFICATION_ID_UPDATE_AVAILABLE = 9001

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

    /** Posts the candle-lighting reminder; [context] should already carry the app language. */
    fun postCandleLighting(
        context: Context,
        date: LocalDate,
        candleLightingTime: LocalTime,
        end: ObservanceEnd?
    ) {
        val text = candleLightingNotificationText(context, date, candleLightingTime, end)
        post(
            context,
            NotificationChannels.CANDLE_LIGHTING,
            notificationId(CHANNEL_CANDLE_LIGHTING_ORDINAL, date),
            text.title,
            text.body,
            date
        )
    }

    /** Posts a holiday reminder; [context] should already carry the app language. */
    fun postHoliday(context: Context, date: LocalDate, holiday: Holiday, daysUntil: Int) {
        val text = holidayNotificationText(context, holiday, daysUntil)
        post(
            context,
            NotificationChannels.HOLIDAYS,
            notificationId(CHANNEL_HOLIDAYS_ORDINAL, date),
            text.title,
            text.body,
            date
        )
    }

    /** Posts a fast reminder; [context] should already carry the app language. */
    fun postFast(
        context: Context,
        fastDate: LocalDate,
        fast: Holiday,
        startTime: LocalTime?,
        endTime: LocalTime?
    ) {
        val text = fastNotificationText(context, fast, startTime, endTime)
        post(
            context,
            NotificationChannels.FASTS,
            notificationId(CHANNEL_FASTS_ORDINAL, fastDate),
            text.title,
            text.body,
            fastDate
        )
    }

    /**
     * Posts a personal event; [context] should already carry the app language. The id is
     * keyed on [title] and [label], not the localized text, so a language change replaces
     * the notification instead of adding a second one.
     */
    fun postPersonalEvent(
        context: Context,
        date: LocalDate,
        title: String,
        label: OccurrenceLabel?
    ) {
        val text = personalEventNotificationText(context, occurrenceTitle(context, title, label))
        val key = "event|$title|$label"
        post(
            context,
            NotificationChannels.PERSONAL_EVENTS,
            notificationId(CHANNEL_PERSONAL_EVENTS_ORDINAL, date) + key.hashCode() % 1000,
            text.title,
            text.body,
            date
        )
    }

    /** Posts a Hebrew birthday; [context] should already carry the app language. */
    fun postBirthday(
        context: Context,
        date: LocalDate,
        contactName: String,
        label: OccurrenceLabel?
    ) {
        val text = birthdayNotificationText(context, contactName, label)
        // Keyed on the contact and label, not the localized text, so a language change
        // replaces the notification instead of adding a second one
        val key = "birthday|$contactName|$label"
        post(
            context,
            NotificationChannels.PERSONAL_EVENTS,
            notificationId(CHANNEL_PERSONAL_EVENTS_ORDINAL, date) + key.hashCode() % 1000,
            text.title,
            text.body,
            date
        )
    }

    /** Posts the tzait Omer reminder; [context] should already carry the app language. */
    fun postOmer(context: Context, date: LocalDate, omerDay: Int) {
        val text = omerNotificationText(context, omerDay, tonight = true)
        post(
            context,
            NotificationChannels.OMER,
            notificationId(CHANNEL_OMER_ORDINAL, date),
            text.title,
            text.body,
            date
        )
    }

    /** Posts the morning Omer reminder; [context] should already carry the app language. */
    fun postOmerMorning(context: Context, date: LocalDate, omerDay: Int) {
        val text = omerNotificationText(context, omerDay, tonight = false)
        post(
            context,
            NotificationChannels.OMER,
            notificationId(CHANNEL_OMER_ORDINAL, date) + 1,
            text.title,
            text.body,
            date
        )
    }

    /** Posts the update notice; [context] should already carry the app language. */
    fun postUpdateAvailable(context: Context, latestVersion: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return false

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_UPDATE_AVAILABLE,
            Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASES_PAGE)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = updateAvailableNotificationText(context, latestVersion)
        val notification = NotificationCompat.Builder(context, NotificationChannels.UPDATE_AVAILABLE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UPDATE_AVAILABLE, notification)
        return true
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
