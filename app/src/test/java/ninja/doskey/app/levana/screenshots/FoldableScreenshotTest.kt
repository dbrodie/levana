package ninja.doskey.app.levana.screenshots

import android.content.Intent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.MainActivity
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.notifications.NotificationPoster
import ninja.doskey.app.levana.ui.walkthrough.WalkthroughGate
import ninja.doskey.app.levana.ui.walkthrough.WalkthroughRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.Module
import org.koin.dsl.module
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Key screens at each foldable window size (TASK-52.1, TASK-52.2), in English light: the
 * month calendar, the date picker, the event editor, Zmanim, the Agenda and Settings. Above
 * phone width, single-column content is capped and centred (ui/LargeScreenLayout.kt).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class FoldableScreenshotTest(private val size: FoldableSize) : ScreenshotTest(Variant.EN_LIGHT, size.qualifiers) {

    @Test
    fun calendar() {
        launchCalendar()
        capture("foldable_calendar_${size.tag}")
    }

    @Test
    fun datePicker() {
        launchCalendar()
        compose.onNodeWithContentDescription(string(R.string.calendar_go_to_date)).performClick()
        await(hasText(string(R.string.date_picker_title)))
        capture("foldable_picker_${size.tag}")
    }

    @Test
    fun addEvent() {
        launchCalendar()
        compose.onNodeWithContentDescription(string(R.string.design_add_event)).performClick()
        awaitText(string(R.string.design_add_event))
        capture("foldable_add_event_${size.tag}")
    }

    @Test
    fun zmanim() {
        launch(
            hasText(string(R.string.nav_zmanim)),
            Intent(app, MainActivity::class.java)
                .putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, TODAY.toEpochDay())
                .putExtra(MainActivity.EXTRA_OPEN_ZMANIM, true)
        )
        capture("foldable_zmanim_${size.tag}")
    }

    @Test
    fun agenda() {
        launchCalendar()
        openDrawer()
        clickable(string(R.string.nav_agenda)).performClick()
        // Without calendar access the agenda offers it in a banner above the list
        awaitText(string(R.string.agenda_calendar_choose))
        capture("foldable_agenda_${size.tag}")
    }

    @Test
    fun settings() {
        openSettings()
        capture("foldable_settings_${size.tag}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = FoldableSize.entries.map { arrayOf<Any>(it) }
    }
}

/**
 * The walkthrough's appearance screen at each foldable size: its illustration grows with the
 * width, and on the landscape inner display it pushed the title below the fold (TASK-52.2).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class FoldableWalkthroughScreenshotTest(private val size: FoldableSize) : ScreenshotTest(Variant.EN_LIGHT, size.qualifiers) {

    override fun testModules(): List<Module> = listOf(module { single { WalkthroughGate(showOnLaunch = true) } })

    @Test
    fun appearance() {
        runBlocking { preferences.markWalkthroughSeen(WalkthroughRegistry.screens.map { it.id } - "appearance") }
        launch(hasText(string(R.string.walkthrough_appearance_title)))
        capture("foldable_walkthrough_appearance_${size.tag}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = FoldableSize.entries.map { arrayOf<Any>(it) }
    }
}

/**
 * 200% system font on the cover displays (TASK-52.2): the month grid and the date picker,
 * whose fixed-size cells cap their text scale and whose picker title moves above the toggle.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class FoldableLargeTextScreenshotTest(private val size: FoldableSize) :
    ScreenshotTest(Variant.EN_LIGHT, size.qualifiers, fontScale = 2f) {

    @Test
    fun calendarAndDatePicker() {
        launchCalendar()
        capture("foldable_font200_calendar_${size.tag}")
        compose.onNodeWithContentDescription(string(R.string.calendar_go_to_date)).performClick()
        await(hasText(string(R.string.date_picker_title)))
        capture("foldable_font200_picker_${size.tag}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = listOf(FoldableSize.FOLD8_COVER, FoldableSize.FOLD8_ULTRA_COVER).map { arrayOf<Any>(it) }
    }
}
