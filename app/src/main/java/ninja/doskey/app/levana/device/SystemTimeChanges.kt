package ninja.doskey.app.levana.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Emits while collected whenever the system date rolls over, the clock is set or the time
 * zone changes. Registered at runtime, so it only covers a running process; the manifest
 * receivers cover the rest.
 */
fun systemTimeChanges(context: Context): Flow<Unit> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(Unit)
        }
    }
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_DATE_CHANGED)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }
    // System broadcasts only
    context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    awaitClose { context.unregisterReceiver(receiver) }
}
