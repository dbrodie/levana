package ninja.doskey.app.levana

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.ThemeMode
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.notifications.NotificationPoster
import ninja.doskey.app.levana.ui.navigation.LevanaApp
import ninja.doskey.app.levana.ui.theme.HolidayTheme
import ninja.doskey.app.levana.ui.theme.LevanaTheme
import ninja.doskey.app.levana.ui.theme.activeHolidayTheme
import java.time.LocalDate
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    companion object {
        /** With [NotificationPoster.EXTRA_DATE_EPOCH_DAY], opens that day's Zmanim screen. */
        const val EXTRA_OPEN_ZMANIM = "EXTRA_OPEN_ZMANIM"

        // enableEdgeToEdge's default navigation bar scrims (shown only with button navigation)
        private val LIGHT_NAV_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val DARK_NAV_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkEpochDay = intent?.getLongExtra(
            NotificationPoster.EXTRA_DATE_EPOCH_DAY,
            0L
        ) ?: 0L
        val openZmanim = intent?.getBooleanExtra(EXTRA_OPEN_ZMANIM, false) ?: false
        setContent {
            val preferencesRepository: PreferencesRepository = koinInject()
            val deviceContext: DeviceContext = koinInject()
            val prefs by preferencesRepository.preferences.collectAsState(
                initial = null
            )
            // Follows midnight, clock and time-zone changes while the app is open
            val deviceToday by deviceContext.todayUpdates.collectAsStateWithLifecycle(initialValue = null)
            val holidayTheme = resolveHolidayTheme(prefs, deviceToday)
            val darkTheme = resolveDarkTheme(prefs, isSystemInDarkTheme())
            // The bars' icons follow the app's mode, which may differ from the system's
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_NAV_SCRIM, DARK_NAV_SCRIM) { darkTheme },
                )
                onDispose { }
            }
            val configuration = LocalConfiguration.current
            val layoutDirection = if (configuration.layoutDirection == android.util.LayoutDirection.RTL) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LevanaTheme(
                    darkTheme = darkTheme,
                    materialYou = prefs?.materialYou == true,
                    holidayTheme = holidayTheme,
                ) {
                    LevanaApp(deepLinkEpochDay = deepLinkEpochDay, openZmanim = openZmanim)
                }
            }
        }
    }
}

/** The app's theme for the device's [today]; none until both the preferences and today have loaded. */
internal fun resolveHolidayTheme(prefs: UserPreferences?, today: LocalDate?): HolidayTheme? =
    prefs?.let { p -> today?.let { activeHolidayTheme(p, it) } }

/** Whether the app is dark: the Theme setting, following the system until the preferences load. */
internal fun resolveDarkTheme(prefs: UserPreferences?, systemDark: Boolean): Boolean =
    (prefs?.themeMode ?: ThemeMode.SYSTEM).isDark(systemDark)
