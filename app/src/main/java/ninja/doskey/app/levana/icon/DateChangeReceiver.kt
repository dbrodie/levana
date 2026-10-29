package ninja.doskey.app.levana.icon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ninja.doskey.app.levana.widget.WidgetUpdater
import android.util.Log
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.quicktile.requestHebrewDateTileRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Updates the launcher icon, home-screen widgets and the quick tile when the device date rolls
 * over to a new day, then schedules the next roll-over. Runs on ACTION_DATE_CHANGED while the
 * app is running and on the [DateRollover] alarm when it is not.
 */
class DateChangeReceiver : BroadcastReceiver(), KoinComponent {

    private val deviceContext: DeviceContext by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_DATE_CHANGED && intent.action != DateRollover.ACTION) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val device = deviceContext.now()
                DateRollover.schedule(context, device)
                DynamicIconManager.update(context, device.today)
                WidgetUpdater.requestUpdate(context)
                requestHebrewDateTileRefresh(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update the launcher icon", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "DateChangeReceiver"
    }
}
