package ninja.doskey.app.levana.widget

import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.device.DeviceNow
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.screenshots.ScreenshotTest
import ninja.doskey.app.levana.ui.theme.levanaColorScheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Screenshot baselines for the month widget (TASK-57.24): its Glance content rendered to
 * RemoteViews and applied as a launcher would, at both of its sizes, for 18 Cheshvan 5787.
 *
 * Record: `./gradlew :app:recordRoborazziDebug --tests '*.MonthWidgetScreenshotTest'`.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class MonthWidgetScreenshotTest(private val variant: ScreenshotTest.Variant) {

    @Test
    fun full() = capture("full", MonthWidget.FULL)

    @Test
    fun compact() = capture("compact", MonthWidget.COMPACT)

    private fun capture(name: String, size: DpSize) {
        RuntimeEnvironment.setQualifiers(variant.qualifiers)
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val zone = ZoneId.of("Asia/Jerusalem")
        val now = DeviceNow(LocalDate.of(2026, 10, 29).atTime(LocalTime.NOON).atZone(zone).toInstant(), zone, Location.JERUSALEM)
        val snapshot = MonthWidgetSnapshots(CalendarRepository())
            .build(UserPreferences(currentLocation = Location.JERUSALEM, isInIsrael = true), now)
        val light = levanaColorScheme(activity, darkTheme = false, snapshot.holidayTheme, materialYou = false)
        val dark = levanaColorScheme(activity, darkTheme = true, snapshot.holidayTheme, materialYou = false)

        val remoteViews = runBlocking {
            GlanceRemoteViews().compose(activity, size) { MonthWidgetContent(snapshot, light, dark) }.remoteViews
        }
        val density = activity.resources.displayMetrics.density
        val host = FrameLayout(activity)
        activity.setContentView(
            host,
            ViewGroup.LayoutParams((size.width.value * density).toInt(), (size.height.value * density).toInt()),
        )
        host.addView(remoteViews.apply(activity, host))
        shadowOf(Looper.getMainLooper()).idle()
        host.captureRoboImage("${ScreenshotTest.OUTPUT_DIR}/widget_month_${name}_${variant.tag}.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotTest.Variant.entries.map { arrayOf(it) }
    }
}
