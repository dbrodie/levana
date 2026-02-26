package com.levana.app.ui.calendar

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "en-rUS-w400dp-h800dp-mdpi",
    application = Application::class
)
class DayOfWeekHeaderScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dayOfWeekHeader_gregorian_ltr() {
        composeTestRule.setContent {
            DayOfWeekHeader(rtl = false)
        }
        composeTestRule.onNode(isRoot())
            .captureRoboImage("src/test/screenshots/dayOfWeekHeader_gregorian_ltr.png")
    }

    @Test
    fun dayOfWeekHeader_hebrew_rtl() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                DayOfWeekHeader(rtl = true)
            }
        }
        composeTestRule.onNode(isRoot())
            .captureRoboImage("src/test/screenshots/dayOfWeekHeader_hebrew_rtl.png")
    }
}
