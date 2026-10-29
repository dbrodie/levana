package ninja.doskey.app.levana.icon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ninja.doskey.app.levana.widget.WidgetUpdater
import android.util.Log
import ninja.doskey.app.levana.device.DeviceContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Updates the launcher icon and home-screen widgets when the system date rolls over to a new day.
 * Registered in the manifest for ACTION_DATE_CHANGED so it fires at midnight
 * even when the app is not running.
 */
class DateChangeReceiver : BroadcastReceiver(), KoinComponent {

    private val deviceContext: DeviceContext by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_DATE_CHANGED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                DynamicIconManager.update(context, deviceContext.today())
                WidgetUpdater.requestUpdate(context)
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
