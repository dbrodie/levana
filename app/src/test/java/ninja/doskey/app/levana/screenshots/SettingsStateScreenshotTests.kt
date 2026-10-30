package ninja.doskey.app.levana.screenshots

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.domain.model.ThemeMode
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings states that need their own preferences or permissions before launch. */
abstract class SettingsStateScreenshotTest(variant: Variant) : ScreenshotTest(variant) {

    protected fun openLocations() {
        openSettings()
        clickable(string(R.string.settings_location_label)).performClick()
        awaitText(NEW_YORK.name)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = variants()
    }
}

/** Candle lighting in hours-before mode, the other branch of the notification sub-rows. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class SettingsHoursBeforeScreenshotTest(variant: Variant) : SettingsStateScreenshotTest(variant) {

    override suspend fun seedPreferences(preferences: PreferencesRepository) {
        super.seedPreferences(preferences)
        preferences.saveNotifyCandleLighting(true)
        preferences.saveCandleLightingNotifyMode("hours_before")
        preferences.saveCandleLightingHoursBefore(3)
    }

    @Test
    fun notificationsHoursBefore() {
        openSettings()
        clickable(string(R.string.settings_notify_omer)).performScrollTo()
        capture("settings_notifications_hours_before")
    }
}

/** No saved cities: only the current location and Add location. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class LocationsEmptyScreenshotTest(variant: Variant) : SettingsStateScreenshotTest(variant) {

    override suspend fun seedPreferences(preferences: PreferencesRepository) {
        preferences.updateCurrentLocation(NEW_YORK)
    }

    @Test
    fun locationsEmpty() {
        openLocations()
        capture("settings_locations_empty")
    }
}

/** Location permission denied: the current-location row asks for it. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class LocationsNoPermissionScreenshotTest(variant: Variant) : SettingsStateScreenshotTest(variant) {

    override val locationPermission = false

    @Test
    fun locationsWithoutPermission() {
        openLocations()
        awaitText(string(R.string.location_permission_needed))
        capture("settings_locations_no_permission")
    }
}

/**
 * Settings → Theme set against the system: Dark while the system is light and Light while it
 * is dark. The whole app, its Settings summary included, follows the setting (TASK-13).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class ForcedThemeScreenshotTest(private val variant: Variant) : SettingsStateScreenshotTest(variant) {

    override suspend fun seedPreferences(preferences: PreferencesRepository) {
        super.seedPreferences(preferences)
        val systemDark = variant == Variant.EN_DARK || variant == Variant.HE_DARK
        preferences.saveThemeMode(if (systemDark) ThemeMode.LIGHT else ThemeMode.DARK)
    }

    @Test
    fun forcedAgainstTheSystem() {
        launchCalendar()
        capture("app_calendar_forced_theme")
        openDrawer()
        clickable(string(R.string.nav_settings)).performClick()
        await(hasText(string(R.string.settings_developer_settings)))
        clickable(string(R.string.settings_holiday_theming_label)).performScrollTo()
        capture("settings_appearance_forced_theme")
    }
}
