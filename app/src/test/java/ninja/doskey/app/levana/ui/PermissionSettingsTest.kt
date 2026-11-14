package ninja.doskey.app.levana.ui

import android.app.Application
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PermissionSettingsTest {

    private val fine = "FINE"
    private val coarse = "COARSE"

    @Test
    fun deniedWithoutRationaleIsPermanent() {
        assertTrue(isPermanentlyDenied(mapOf(fine to false, coarse to false)) { false })
    }

    @Test
    fun deniedWithRationaleCanStillBeAsked() {
        // The first refusal: the system shows its dialog again next time
        assertFalse(isPermanentlyDenied(mapOf(fine to false)) { true })
        // One of several still has a rationale
        assertFalse(isPermanentlyDenied(mapOf(fine to false, coarse to false)) { it == coarse })
    }

    @Test
    fun anyGrantIsNotDenied() {
        // Approximate location alone still counts as allowed
        assertFalse(isPermanentlyDenied(mapOf(fine to false, coarse to true)) { false })
    }

    @Test
    fun cancelledRequestIsNotDenied() {
        // An interrupted request (e.g. the activity recreated) returns no results
        assertFalse(isPermanentlyDenied(emptyMap()) { false })
    }

    @Test
    fun settingsIntentsTargetThisApp() {
        val details = appSettingsIntent("ninja.example")
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, details.action)
        assertEquals("package:ninja.example", details.dataString)

        val notifications = appNotificationSettingsIntent("ninja.example")
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, notifications.action)
        assertEquals("ninja.example", notifications.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }
}
