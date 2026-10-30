package ninja.doskey.app.levana.screenshots

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.db.PersonalEvent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/** The shared dual-calendar date picker (TASK-45, design 1b) from its call sites. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class DatePickerScreenshotTest(variant: Variant) : ScreenshotTest(variant) {

    /** A new event from the calendar opens on the selected day, TODAY (7 Kislev 5787), in Hebrew mode. */
    @Test
    fun hebrewLedYearListAndGregorianLed() {
        launchCalendar()
        compose.onNodeWithContentDescription(string(R.string.design_add_event)).performClick()
        awaitText(string(R.string.design_add_event))
        openPicker()
        capture("picker_hebrew_led")

        withClickLabel(string(R.string.date_picker_choose_year)).performClick()
        await(hasText("5787"))
        capture("picker_year_list")

        withClickLabel(string(R.string.date_picker_choose_day)).performClick()
        clickable(string(R.string.calendar_mode_gregorian)).performClick()
        await(hasContentDescription(string(R.string.date_picker_next_month)))
        capture("picker_gregorian_led")
    }

    /** Purim 5787, in Adar II of a leap year, with its holiday chip. */
    @Test
    fun leapYearAdarIIWithAHolidayChip() {
        editEventOn(14, JewishDate.ADAR_II, 5787)
        capture("picker_purim_5787")
    }

    /** A stored date before the supported years (TASK-24) opens unchanged. */
    @Test
    fun outOfRangeStoredDate() {
        editEventOn(12, JewishDate.NISSAN, 5500)
        capture("picker_out_of_range")
    }

    /** Go to date follows the calendar's own mode, Gregorian here. */
    @Test
    fun goToDate() {
        launchCalendar()
        compose.onNodeWithContentDescription(string(R.string.calendar_go_to_date)).performClick()
        await(hasText(string(R.string.date_picker_title)))
        capture("picker_go_to_date")
    }

    private fun withClickLabel(label: String): SemanticsNodeInteraction {
        val matcher = SemanticsMatcher("has click label $label") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label == label
        }
        await(matcher)
        return compose.onAllNodes(matcher)[0]
    }

    private fun openPicker() {
        clickable(string(R.string.event_hebrew_date_label)).performClick()
        await(hasText(string(R.string.date_picker_title)))
    }

    private fun editEventOn(day: Int, month: Int, year: Int) {
        runBlocking {
            db.personalEventDao().insert(
                PersonalEvent(title = "Stored", hebrewDay = day, hebrewMonth = month, hebrewYear = year)
            )
        }
        launchCalendar()
        openDrawer()
        clickable(string(R.string.design_my_events)).performClick()
        awaitText(string(R.string.events_tab_events))
        clickable(string(R.string.events_tab_events)).performClick()
        awaitText("Stored")
        clickable("Stored").performClick()
        awaitText(string(R.string.design_edit_event))
        openPicker()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = variants()
    }
}
