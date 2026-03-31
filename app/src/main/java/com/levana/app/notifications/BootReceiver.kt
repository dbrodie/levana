package com.levana.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.levana.app.icon.DynamicIconManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyNotificationWorker.enqueueImmediate(context)
            DynamicIconManager.update(context)
        }
    }
}
