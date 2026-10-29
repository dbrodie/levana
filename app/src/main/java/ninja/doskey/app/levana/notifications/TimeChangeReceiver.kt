package ninja.doskey.app.levana.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ninja.doskey.app.levana.widget.WidgetUpdater
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.icon.DynamicIconManager
import ninja.doskey.app.levana.quicktile.requestHebrewDateTileRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Re-plans the reminder alarms, and redraws the home-screen widgets, launcher icon and quick
 * tile, when the device time zone or clock is changed. Registered in the manifest so it also
 * runs when the app is not open. Only the alarms are reconciled; no notification is posted.
 */
class TimeChangeReceiver : BroadcastReceiver(), KoinComponent {

    private val alarmRebuilder: NotificationAlarmRebuilder by inject()
    private val deviceContext: DeviceContext by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIMEZONE_CHANGED &&
            intent.action != Intent.ACTION_TIME_CHANGED
        ) return
        WidgetUpdater.requestUpdate(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                alarmRebuilder.rebuild()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rebuild reminder alarms after ${intent.action}", e)
            }
            try {
                // A new zone or clock can move the device date
                DynamicIconManager.update(context, deviceContext.today())
                requestHebrewDateTileRefresh(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update the launcher icon after ${intent.action}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "TimeChangeReceiver"
    }
}
