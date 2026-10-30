package ninja.doskey.app.levana.ui.walkthrough

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Whether the walkthrough opens by itself when the app starts. Off under Robolectric and
 * instrumentation, so screenshot and UI tests see the app as before; a test that wants the
 * walkthrough binds `WalkthroughGate(showOnLaunch = true)` in Koin.
 */
class WalkthroughGate(val showOnLaunch: Boolean) {
    companion object {
        fun forThisProcess(): WalkthroughGate = WalkthroughGate(showOnLaunch = !isAutomatedTest())

        private fun isAutomatedTest(): Boolean =
            Build.FINGERPRINT == "robolectric" ||
                runCatching { Class.forName("androidx.test.runner.AndroidJUnitRunner") }.isSuccess
    }
}

/** What the walkthrough asks of the device: permissions and feature availability. */
interface WalkthroughDevice {
    fun isGranted(permission: WalkthroughPermission): Boolean
    fun isAvailable(screen: FeatureScreen): Boolean
}

class AndroidWalkthroughDevice(private val context: Context) : WalkthroughDevice {
    override fun isGranted(permission: WalkthroughPermission): Boolean =
        permission.isGranted(
            permission.permissions.associateWith {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            }
        )

    override fun isAvailable(screen: FeatureScreen): Boolean = screen.isAvailable(context)
}
