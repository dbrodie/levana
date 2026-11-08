package ninja.doskey.app.levana.device

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Emits while collected whenever the app's locales change: Settings → App language or the
 * device language. Other configuration changes (rotation, folding) don't emit.
 */
fun localeChanges(context: Context): Flow<Unit> = callbackFlow {
    var locales = context.resources.configuration.locales
    val callbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            if (newConfig.locales != locales) {
                locales = newConfig.locales
                trySend(Unit)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() = Unit
    }
    context.registerComponentCallbacks(callbacks)
    awaitClose { context.unregisterComponentCallbacks(callbacks) }
}
