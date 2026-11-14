package ninja.doskey.app.levana.screenshots

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.LocationService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Permissions the system no longer asks for (denied twice): the request comes back denied at
 * once, with no rationale, and each control then points to the app's system settings page.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class PermissionBlockedScreenshotTest(variant: Variant) : ScreenshotTest(variant) {

    /**
     * Answers the pending permission request as the system does once it stops asking: denied,
     * with no rationale (Robolectric's default). The platform allows one request at a time, so
     * its pending flag is cleared as the real dispatch would.
     */
    private fun denyPermanently() = onActivity { activity ->
        val request = checkNotNull(shadowOf(activity).lastRequestedPermission) { "no permission request" }
        Activity::class.java.getDeclaredField("mHasCurrentPermissionsRequest").apply { isAccessible = true }
            .setBoolean(activity, false)
        @Suppress("DEPRECATION")
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            IntArray(request.requestedPermissions.size) { PackageManager.PERMISSION_DENIED }
        )
    }

    /** The newest started activity (Robolectric returns newest first), after the permission dialogs. */
    private fun assertOpenedSettings(action: String) {
        val started = generateSequence { shadowOf(app).nextStartedActivity }.toList()
        assertEquals(action, started.firstOrNull()?.action)
    }

    @Test
    fun locations() {
        shadowOf(app).denyPermissions(*LocationService.PERMISSIONS)
        openSettings()
        // The app asks for location at startup; refused, so Cities can ask again
        denyPermanently()
        clickable(string(R.string.settings_location_label)).performClick()
        clickable(string(R.string.location_permission_needed)).performClick()
        denyPermanently()
        awaitText(string(R.string.location_permission_blocked))
        capture("settings_locations_permission_blocked")

        clickable(string(R.string.location_permission_blocked)).performClick()
        assertOpenedSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    }

    @Test
    fun systemCalendars() {
        shadowOf(app).denyPermissions(Manifest.permission.READ_CALENDAR)
        openSettings()
        clickable(string(R.string.settings_system_calendars_label)).performClick()
        clickable(string(R.string.common_grant_permission)).performClick()
        denyPermanently()
        awaitText(string(R.string.permission_blocked_explanation))
        capture("settings_system_calendars_permission_blocked")

        clickable(string(R.string.common_open_settings)).performClick()
        assertOpenedSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    }

    @Test
    fun birthdays() {
        shadowOf(app).denyPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        launchCalendar()
        openDrawer()
        clickable(string(R.string.design_my_events)).performClick()
        clickable(string(R.string.common_grant_permission)).performClick()
        denyPermanently()
        awaitText(string(R.string.permission_blocked_explanation))
        capture("app_events_birthdays_permission_blocked")

        clickable(string(R.string.common_open_settings)).performClick()
        assertOpenedSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    }

    @Test
    fun notifications() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        openSettings()
        clickable(string(R.string.settings_notify_fasts)).performScrollTo().performClick()
        denyPermanently()
        awaitText(string(R.string.notifications_blocked_title))
        capture("settings_notifications_blocked")

        clickable(string(R.string.common_open_settings)).performClick()
        assertOpenedSettings(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = variants()
    }
}
