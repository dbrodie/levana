package ninja.doskey.app.levana.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.R

/**
 * Whether a denied request was final: nothing in [results] was granted and the system would
 * not show its dialog again for any of them (denied twice, or "Don't ask again"). Then only
 * the app's system settings page can grant it.
 */
fun isPermanentlyDenied(results: Map<String, Boolean>, shouldShowRationale: (String) -> Boolean): Boolean =
    results.isNotEmpty() && results.values.none { it } && results.keys.none(shouldShowRationale)

/** [isPermanentlyDenied] for a request made from this context's activity. */
fun Context.isPermanentlyDenied(results: Map<String, Boolean>): Boolean {
    val activity = findActivity() ?: return false
    return isPermanentlyDenied(results, activity::shouldShowRequestPermissionRationale)
}

/** The app's page in system settings, where its permissions can be allowed. */
fun appSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** The app's notification page in system settings. */
fun appNotificationSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** Opens the app's system settings page, or its notification page for [notifications]. */
fun Context.openAppSettings(notifications: Boolean = false) {
    val intent = if (notifications) appNotificationSettingsIntent(packageName) else appSettingsIntent(packageName)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // A device without the notification page still has the app details page
        if (notifications) startActivity(appSettingsIntent(packageName))
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Asks for a permission, or once the system stops asking, sends the user to system settings.
 * [blocked] turns true when a request comes back permanently denied (see [isPermanentlyDenied]);
 * screens then show "Open settings" with a short explanation. A grant made in system settings
 * reaches the screen through its own recheck on resume.
 */
@Stable
class PermissionRequester internal constructor(
    private val blockedState: MutableState<Boolean>,
    private val launch: () -> Unit,
    private val openSettingsPage: () -> Unit,
) {
    val blocked: Boolean get() = blockedState.value

    /** Shows the system dialog, or opens system settings when the permission is [blocked]. */
    fun request() = if (blocked) openSettings() else launch()

    fun openSettings() = openSettingsPage()
}

/**
 * A [PermissionRequester] for [permissions]; [onResult] gets whether any was granted, after
 * [onBlocked] when the request came back permanently denied.
 * [notifications] opens the app's notification settings page instead of its details page.
 */
@Composable
fun rememberPermissionRequester(
    vararg permissions: String,
    notifications: Boolean = false,
    onBlocked: () -> Unit = {},
    onResult: (granted: Boolean) -> Unit = {},
): PermissionRequester {
    val context = LocalContext.current
    val blocked = rememberSaveable { mutableStateOf(false) }
    val currentOnBlocked by rememberUpdatedState(onBlocked)
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        blocked.value = context.isPermanentlyDenied(results)
        if (blocked.value) currentOnBlocked()
        currentOnResult(results.values.any { it })
    }
    return PermissionRequester(
        blockedState = blocked,
        launch = { launcher.launch(arrayOf(*permissions)) },
        openSettingsPage = { context.openAppSettings(notifications) },
    )
}

/** "Grant permission", or "Open settings" once the permission is [blocked]. */
@Composable
fun PermissionButton(blocked: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(stringResource(if (blocked) R.string.common_open_settings else R.string.common_grant_permission))
    }
}

/** Under a permission explanation: why the button now opens system settings. */
@Composable
fun PermissionBlockedNote(blocked: Boolean) {
    if (!blocked) return
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.permission_blocked_explanation),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
