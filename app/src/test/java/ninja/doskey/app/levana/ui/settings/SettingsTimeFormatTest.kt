package ninja.doskey.app.levana.ui.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import ninja.doskey.app.levana.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the current morning-time labels on the Settings stepper rows, including
 * the noon label "12:00 AM" (TASK-32). Update these when that bug is fixed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsTimeFormatTest {

    private fun label(minutesOfDay: Int): String =
        ApplicationProvider.getApplicationContext<Application>()
            .getString(R.string.settings_at_morning_time, *morningTimeFormatArgs(minutesOfDay))

    @Test
    fun argsAreTwelveHourHourMinutesAndTwentyFourHourHour() {
        assertEquals(listOf(12, 0, 0), morningTimeFormatArgs(0).toList())
        assertEquals(listOf(6, 0, 6), morningTimeFormatArgs(360).toList())
        assertEquals(listOf(9, 30, 9), morningTimeFormatArgs(570).toList())
        assertEquals(listOf(11, 30, 11), morningTimeFormatArgs(690).toList())
        assertEquals(listOf(12, 0, 12), morningTimeFormatArgs(720).toList())
        assertEquals(listOf(1, 30, 13), morningTimeFormatArgs(810).toList())
    }

    @Test
    fun englishLabelsAcrossTheStepperRange() {
        assertEquals("At 6:00 AM", label(360))
        assertEquals("At 6:30 AM", label(390))
        assertEquals("At 9:00 AM", label(540))
        assertEquals("At 11:30 AM", label(690))
    }

    @Test
    fun englishNoonCurrentlyReadsAm() {
        assertEquals("At 12:00 AM", label(720))
    }

    @Test
    @Config(qualifiers = "iw")
    fun hebrewLabelsUseTheTwentyFourHourHour() {
        assertEquals("בשעה 6:00", label(360))
        assertEquals("בשעה 11:30", label(690))
        assertEquals("בשעה 12:00", label(720))
    }
}
