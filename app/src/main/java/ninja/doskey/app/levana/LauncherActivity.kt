package ninja.doskey.app.levana

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Stable launcher entry point for the default and dated icon components.
 *
 * Launcher components may be enabled or disabled while [MainActivity] is visible without
 * affecting the activity that owns the app UI.
 */
class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(
            Intent(intent).setClass(this, MainActivity::class.java)
        )
        finish()
    }
}
