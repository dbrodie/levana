package ninja.doskey.app.levana.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import ninja.doskey.app.levana.R

object NotificationChannels {

    const val CANDLE_LIGHTING = "candle_lighting"
    const val HOLIDAYS = "holidays"
    const val FASTS = "fasts"
    const val PERSONAL_EVENTS = "personal_events"
    const val OMER = "omer"
    const val UPDATE_AVAILABLE = "update_available"

    private class Spec(
        val id: String,
        @StringRes val name: Int,
        @StringRes val description: Int,
        val importance: Int
    )

    private val specs = listOf(
        Spec(
            CANDLE_LIGHTING,
            R.string.channel_candle_lighting_name,
            R.string.channel_candle_lighting_description,
            NotificationManager.IMPORTANCE_HIGH
        ),
        Spec(
            HOLIDAYS,
            R.string.channel_holidays_name,
            R.string.channel_holidays_description,
            NotificationManager.IMPORTANCE_DEFAULT
        ),
        Spec(
            FASTS,
            R.string.channel_fasts_name,
            R.string.channel_fasts_description,
            NotificationManager.IMPORTANCE_DEFAULT
        ),
        Spec(
            PERSONAL_EVENTS,
            R.string.channel_personal_events_name,
            R.string.channel_personal_events_description,
            NotificationManager.IMPORTANCE_DEFAULT
        ),
        Spec(
            OMER,
            R.string.channel_omer_name,
            R.string.channel_omer_description,
            NotificationManager.IMPORTANCE_DEFAULT
        ),
        Spec(
            UPDATE_AVAILABLE,
            R.string.channel_updates_name,
            R.string.channel_updates_description,
            NotificationManager.IMPORTANCE_DEFAULT
        )
    )

    /**
     * Creates the channels, or renames existing ones, in the app language.
     *
     * Android keeps a channel's name and description until the app creates the channel
     * again with the same id, so call this whenever the language may have changed. The
     * user's importance and other channel settings are kept.
     */
    fun createAll(context: Context) {
        val localized = context.withAppLocale()
        val manager = context.getSystemService(NotificationManager::class.java)
        val channels = specs.map { spec ->
            NotificationChannel(spec.id, localized.getString(spec.name), spec.importance).apply {
                description = localized.getString(spec.description)
            }
        }
        manager.createNotificationChannels(channels)
    }
}
