package ninja.doskey.app.levana.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.icon.DateRollover
import ninja.doskey.app.levana.icon.DynamicIconManager
import ninja.doskey.app.levana.update.UpdateCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val deviceContext: DeviceContext by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        DailyNotificationWorker.enqueueImmediate(context)
        UpdateCheckWorker.enqueueDaily(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // The device date, developer override included
                val device = deviceContext.now()
                DateRollover.schedule(context, device)
                DynamicIconManager.update(context, device.today)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update the launcher icon", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
