package ninja.doskey.app.levana.icon

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ninja.doskey.app.levana.device.DeviceNow

/**
 * Wakes [DateChangeReceiver] just after the device's next midnight. Android no longer delivers
 * ACTION_DATE_CHANGED to manifest receivers of an app that isn't running, so without this the
 * launcher icon and tile would keep yesterday's date until the app is next opened.
 */
object DateRollover {
    const val ACTION = "ninja.doskey.app.levana.DATE_ROLLOVER"
    private const val WINDOW_MS = 10 * 60 * 1000L

    /** Schedules the next roll-over for [device]'s next midnight, replacing any earlier one. */
    fun schedule(context: Context, device: DeviceNow) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        // On the real clock: the developer override shifts the device clock by whole days
        val at = device.toSystemInstant(device.nextMidnight)
        // Not a wakeup alarm: the icon only matters once the device is in use
        alarmManager.setWindow(AlarmManager.RTC, at.toEpochMilli(), WINDOW_MS, pendingIntent(context))
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, DateChangeReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
