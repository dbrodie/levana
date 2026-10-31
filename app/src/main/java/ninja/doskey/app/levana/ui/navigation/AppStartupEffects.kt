package ninja.doskey.app.levana.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ninja.doskey.app.levana.data.LocationService
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.icon.DateRollover
import ninja.doskey.app.levana.icon.DynamicIconManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * The app shell's side effects: the location-permission ask (unless [askForLocation] is off),
 * the current-location refresh on resume, the launcher-icon refresh, and the notification and
 * widget deep links.
 */
@Composable
internal fun LevanaStartupEffects(
    navController: NavHostController,
    deepLinkEpochDay: Long,
    openZmanim: Boolean,
    askForLocation: Boolean = true,
) {
    val preferencesRepository: PreferencesRepository = koinInject()
    val locationService: LocationService = koinInject()
    val deviceContext: DeviceContext = koinInject()

    // Ask for location once per launch; the dialog pauses the activity, so a grant is
    // picked up by the ON_RESUME refresh below.
    var askedForLocation by rememberSaveable { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    LaunchedEffect(Unit) {
        if (askForLocation && !askedForLocation && !locationService.hasPermission()) {
            askedForLocation = true
            locationPermissionLauncher.launch(LocationService.PERMISSIONS)
        }
    }

    // Current-location refresh on resume; not when a fold, rotation or resize recreated the
    // activity (each new composition sees a resume), so no new fix or geocoder call for it
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val shell: ShellViewModel = koinViewModel()
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (shell.shouldRefreshLocation(SystemClock.elapsedRealtime())) {
                    scope.launch {
                        locationService.getCurrentLocationOrNull()
                            ?.let { preferencesRepository.updateCurrentLocation(it) }
                    }
                }
                Lifecycle.Event.ON_STOP -> shell.onStop(context.findActivity()?.isChangingConfigurations == true)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The launcher icon shows the device's Hebrew day; kept current while the app is started
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            deviceContext.todayUpdates.collect { today ->
                withContext(Dispatchers.IO) { DynamicIconManager.update(context, today) }
                // Keeps the icon turning over at midnight once the app is in the background
                DateRollover.schedule(context, deviceContext.now())
            }
        }
    }

    // Handle notification and widget deep links: open the calendar on that day (day detail is inline)
    LaunchedEffect(deepLinkEpochDay) {
        if (deepLinkEpochDay != 0L) {
            navController.navigate(CalendarRoute) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            // The home-screen widget's zmanim rows open that day's Zmanim screen over it
            if (openZmanim) navController.navigate(ZmanimRoute(deepLinkEpochDay))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
