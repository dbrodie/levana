package ninja.doskey.app.levana.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.device.zoneMismatchKey
import org.koin.compose.koinInject

/**
 * Tells the user once when the location fix doesn't fit the phone's time zone (TASK-46). The
 * preferences are re-read on a new fix and on every time-zone change, so it shows when either
 * happens in the app, and on the next open for a mismatch that arose while the app was closed.
 */
@Composable
internal fun ZoneMismatchNotice() {
    val preferencesRepository: PreferencesRepository = koinInject()
    val deviceContext: DeviceContext = koinInject()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            preferencesRepository.preferences.collect { prefs ->
                val key = zoneMismatchKey(prefs, deviceContext.now(prefs))
                val shown = preferencesRepository.getZoneMismatchNotice()
                if (key != shown) {
                    // Recorded when shown, so dismissing it or leaving the app never repeats it
                    preferencesRepository.setZoneMismatchNotice(key)
                    showing = key != null
                } else if (key == null) {
                    showing = false
                }
            }
        }
    }

    if (showing) {
        val context = LocalContext.current
        ZoneMismatchDialog(
            onOpenSettings = {
                showing = false
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_DATE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: ActivityNotFoundException) {
                    // No settings screen for it on this device; nothing else to offer
                }
            },
            onDismiss = { showing = false }
        )
    }
}

@Composable
internal fun ZoneMismatchDialog(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.zone_mismatch_title)) },
        text = { Text(stringResource(R.string.zone_mismatch_text)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.zone_mismatch_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.zone_mismatch_dismiss)) }
        }
    )
}
