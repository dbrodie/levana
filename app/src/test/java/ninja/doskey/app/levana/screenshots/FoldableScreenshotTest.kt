package ninja.doskey.app.levana.screenshots

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * One sample per foldable window size (TASK-52.1): the month calendar in English light.
 * TASK-52.2 adds the other screens at these sizes.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class FoldableScreenshotTest(private val size: FoldableSize) : ScreenshotTest(Variant.EN_LIGHT, size.qualifiers) {

    @Test
    fun calendar() {
        launchCalendar()
        capture("foldable_calendar_${size.tag}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = FoldableSize.entries.map { arrayOf<Any>(it) }
    }
}
