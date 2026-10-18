package ninja.doskey.app.levana

import android.app.Application
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LauncherActivityTest {

    @Test
    fun forwardsLauncherIntentToMainActivityAndFinishes() {
        val original = Intent(Intent.ACTION_VIEW, Uri.parse("levana://calendar/day")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("epoch_day", 20_000L)
        }

        val activity = Robolectric.buildActivity(LauncherActivity::class.java, original)
            .create()
            .get()
        val forwarded = shadowOf(activity).nextStartedActivity

        assertEquals(MainActivity::class.java.name, forwarded.component?.className)
        assertEquals(original.action, forwarded.action)
        assertEquals(original.data, forwarded.data)
        assertEquals(original.flags, forwarded.flags)
        assertEquals(20_000L, forwarded.getLongExtra("epoch_day", 0L))
        assertTrue(activity.isFinishing)
    }
}
