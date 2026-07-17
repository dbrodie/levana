package ninja.doskey.app.levana.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ninja.doskey.app.levana.icon.DynamicIconManager
import ninja.doskey.app.levana.update.UpdateCheckWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyNotificationWorker.enqueueImmediate(context)
            DynamicIconManager.update(context)
            UpdateCheckWorker.enqueueDaily(context)
        }
    }
}
