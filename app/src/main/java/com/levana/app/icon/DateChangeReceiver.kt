package com.levana.app.icon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Updates the launcher icon when the system date rolls over to a new day.
 * Registered in the manifest for ACTION_DATE_CHANGED so it fires at midnight
 * even when the app is not running.
 */
class DateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_DATE_CHANGED) {
            DynamicIconManager.update(context)
        }
    }
}
