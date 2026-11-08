package ninja.doskey.app.levana.screenshots

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import ninja.doskey.app.levana.ui.panes.PANE_DIVIDER_TAG
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fold/unfold continuity (TASK-52.3): each test sets up a screen on the inner display, then
 * folds to the cover, unfolds, rotates or enters split screen. Every step recreates
 * MainActivity with the new size, keeping its view models, as the system does. The user must
 * still be on the same screen, with the same back stack and in-progress state. A few tests
 * also kill the process (saved state only, new view models).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = Application::class)
class FoldContinuityTest(private val device: Device) : ScreenshotTest(Variant.EN_LIGHT, device.inner.qualifiers) {

    /** A foldable: the inner display it starts on, its cover, and the other sizes it can take. */
    enum class Device(val inner: FoldableSize, val cover: FoldableSize, val rotated: FoldableSize, val split: FoldableSize) {
        FOLD8(
            FoldableSize.FOLD8_INNER_LANDSCAPE, FoldableSize.FOLD8_COVER,
            FoldableSize.FOLD8_INNER_PORTRAIT, FoldableSize.FOLD8_SPLIT_HALF,
        ),
        FOLD8_ULTRA(
            FoldableSize.FOLD8_ULTRA_INNER_PORTRAIT, FoldableSize.FOLD8_ULTRA_COVER,
            FoldableSize.FOLD8_ULTRA_INNER_LANDSCAPE, FoldableSize.FOLD8_ULTRA_SPLIT_HALF,
        ),
    }

    /** The window size now; the test starts on the inner display. */
    private var size = device.inner

    private fun resize(to: FoldableSize) {
        size = to
        recreate("+${to.qualifiers}")
    }
    private fun fold() = resize(device.cover)
    private fun unfold() = resize(device.inner)
    private fun rotate() = resize(device.rotated)
    private fun enterSplitScreen() = resize(device.split)

    /** Month and Agenda show the day panel beside the list from 840dp (TASK-52.6). */
    private val twoPanes get() = size.widthDp >= 840

    /** Every size change the device goes through, checking [assertKept] after each. */
    private fun throughEverySize(assertKept: () -> Unit) {
        for (change in listOf(::fold, ::unfold, ::rotate, ::unfold, ::enterSplitScreen, ::unfold)) {
            change()
            assertKept()
        }
    }

    private fun day(description: String) = hasContentDescription(description, substring = true)

    private fun assertSelected(description: String) = await(day(description) and isSelected())

    /** The texts on screen, in order: equal before and after means the same content. */
    private fun shownTexts(): List<String> = compose.onAllNodes(onScreen(), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .flatMap { node -> node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } }

    private fun awaitShown(text: String) = awaitUntil("$text on screen") { text in shownTexts() }

    private fun back() = onActivity { it.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun theMonthKeepsItsMonthSelectedDayAndDivider() {
        launchCalendar()
        // To December (a swipe in the grid pane, which leaves the divider alone), then the 3rd
        val dividerBefore = dividerState()
        compose.onAllNodes(day("November 20, 2026"))[0].performTouchInput { swipeLeft(startX = right, endX = left - 4 * width) }
        await(day("December 3, 2026"))
        assertEquals(dividerBefore, dividerState())
        compose.onAllNodes(day("December 3, 2026"))[0].performClick()
        assertSelected("December 3, 2026")
        // The divider to its next stop (TalkBack's double tap): 50% -> 65%
        divider().performSemanticsAction(SemanticsActions.OnClick)
        val divider65 = dividerState()
        assertTrue(divider65 != dividerBefore)

        throughEverySize {
            assertSelected("December 3, 2026")
            if (twoPanes) {
                // The panel beside the grid, no sheet; the divider where it was left
                awaitText(string(R.string.sheet_more_times))
                assertEquals(0, compose.onAllNodes(hasContentDescription(string(R.string.sheet_expand))).fetchSemanticsNodes().size)
                if (size == device.inner) assertEquals(divider65, dividerState())
            } else {
                // The phone layout: the same day in the sheet, no divider
                await(hasContentDescription(string(R.string.sheet_expand)))
                assertEquals(0, compose.onAllNodes(hasTestTag(PANE_DIVIDER_TAG)).fetchSemanticsNodes().size)
            }
        }
    }

    @Test
    fun theSheetStaysOpenBetweenPhoneSizes() {
        launchCalendar()
        fold()
        compose.onAllNodes(day("November 20, 2026"))[0].performClick()
        compose.onNode(hasContentDescription(string(R.string.sheet_expand))).performClick()
        await(hasContentDescription(string(R.string.sheet_collapse)))
        for (change in listOf(::enterSplitScreen, ::fold)) {
            change()
            assertSelected("November 20, 2026")
            await(hasContentDescription(string(R.string.sheet_collapse)))
        }
    }

    @Test
    fun theDividerAndSelectedDaySurviveProcessDeath() {
        launchCalendar()
        compose.onAllNodes(day("November 20, 2026"))[0].performClick()
        assertSelected("November 20, 2026")
        divider().performCustomAction(string(R.string.pane_divider_hide))
        await(hasTestTag(PANE_DIVIDER_TAG) and hasStateDescription(string(R.string.pane_divider_hidden)))

        restoreAfterProcessDeath()
        assertSelected("November 20, 2026")
        await(hasTestTag(PANE_DIVIDER_TAG) and hasStateDescription(string(R.string.pane_divider_hidden)))
        // Shown again at the stop it had
        divider().performCustomAction(string(R.string.pane_divider_show))
        awaitText(string(R.string.sheet_more_times))
    }

    @Test
    fun theAgendaKeepsItsSelectedDay() {
        launchCalendar()
        openDrawer()
        clickable(string(R.string.nav_agenda)).performClick()
        await(hasScrollToIndexAction())
        // The date rail shows its day in the panel
        await(day("November 21, 2026") and hasClickAction())
        compose.onAllNodes(day("November 21, 2026") and hasClickAction())[0].performClick()
        assertSelected("November 21, 2026")
        awaitShown("Vayetzei")

        throughEverySize {
            if (twoPanes) {
                assertSelected("November 21, 2026")
                awaitShown("Vayetzei")
            } else {
                // No panel and no selection on phone-sized windows
                assertEquals(0, compose.onAllNodes(day("November 21, 2026") and isSelected()).fetchSemanticsNodes().size)
            }
        }

        restoreAfterProcessDeath()
        assertSelected("November 21, 2026")
    }

    @Test
    fun settingsKeepsItsOpenDialogAndBackStack() {
        openSettings()
        clickable(string(R.string.settings_theme_label)).performScrollTo().performClick()
        await(hasText(string(R.string.settings_theme_dark)))

        throughEverySize { await(hasText(string(R.string.settings_theme_dark))) }

        // Closing the dialog leaves Settings, and back returns to the Month under it
        clickable(string(R.string.common_cancel)).performClick()
        awaitGone(hasText(string(R.string.settings_theme_dark)))
        await(hasText(string(R.string.settings_theme_label)))
        back()
        await(hasContentDescription(string(R.string.calendar_go_to_date)))
    }

    @Test
    fun theDrawerStaysOpen() {
        launchCalendar()
        openDrawer()
        throughEverySize { awaitText(string(R.string.design_manage_cities)) }
    }

    @Test
    fun theAgendaKeepsItsPlaceAndSearch() {
        launchCalendar()
        openDrawer()
        clickable(string(R.string.nav_agenda)).performClick()
        await(hasScrollToIndexAction())
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(40)
        compose.waitForIdle()
        val header = shownTexts().first()

        throughEverySize { assertEquals(header, shownTexts().first()) }

        compose.onNode(hasContentDescription(string(R.string.agenda_search))).performClick()
        await(hasSetTextAction())
        compose.onNode(hasSetTextAction()).performTextReplacement("Chanukah")
        awaitShown("Chanukah")
        // The first results, which every size shows
        val results = shownTexts().take(AGENDA_ROWS)

        throughEverySize { assertEquals(results, shownTexts().take(AGENDA_ROWS)) }
    }

    @Test
    fun zmanimKeepsItsDate() {
        launchCalendar()
        compose.onAllNodes(day("November 20, 2026"))[0].performClick()
        assertSelected("November 20, 2026")
        // On a phone-sized window the times are in the sheet; beside the grid they show already
        if (!twoPanes) compose.onNode(hasContentDescription(string(R.string.sheet_expand))).performClick()
        clickable(string(R.string.sheet_more_times)).performScrollTo().performClick()
        await(hasText(string(R.string.zmanim_category_morning)))
        // The first rows, which every size shows; their times are that day's
        val zmanim = shownTexts().take(ZMANIM_ROWS)

        throughEverySize { assertEquals(zmanim, shownTexts().take(ZMANIM_ROWS)) }

        back()
        assertSelected("November 20, 2026")
    }

    @Test
    fun theDatePickerKeepsItsYearListAndSelection() {
        launchCalendar()
        compose.onNode(hasContentDescription(string(R.string.calendar_go_to_date))).performClick()
        await(hasText(string(R.string.date_picker_title)))
        clickable("21").performClick()
        compose.onNode(clickLabel(string(R.string.date_picker_choose_year))).performClick()
        await(hasText("2030"))
        val picker = pickerTexts()

        throughEverySize { assertEquals(picker, pickerTexts()) }
    }

    @Test
    fun theEditorKeepsUnsavedInput() {
        runBlocking {
            db.personalEventDao().insert(PersonalEvent(title = "Stored", hebrewDay = 10, hebrewMonth = JewishDate.TEVES, hebrewYear = 5787))
        }
        launchCalendar()
        openDrawer()
        clickable(string(R.string.design_my_events)).performClick()
        clickable(string(R.string.events_tab_events)).performClick()
        clickable("Stored").performClick()
        awaitText(string(R.string.design_edit_event))
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Changed, not saved")
        await(hasText("Changed, not saved"))

        throughEverySize { await(hasText("Changed, not saved")) }

        restoreAfterProcessDeath()
        await(hasText("Changed, not saved"))
        awaitText(string(R.string.design_edit_event))
    }

    @Test
    fun processDeathKeepsTheBackStackAndSelectedDay() {
        launchCalendar()
        compose.onAllNodes(day("November 20, 2026"))[0].performClick()
        assertSelected("November 20, 2026")
        openDrawer()
        clickable(string(R.string.nav_settings)).performClick()
        await(hasText(string(R.string.settings_developer_settings)))

        restoreAfterProcessDeath()
        await(hasText(string(R.string.settings_developer_settings)))
        back()
        assertSelected("November 20, 2026")
    }

    /** The date picker's texts, without the calendar behind it, whose layout depends on the width. */
    private fun pickerTexts() = shownTexts().dropWhile { it != string(R.string.date_picker_title) }

    private fun divider() = compose.onNode(hasTestTag(PANE_DIVIDER_TAG))

    private fun dividerState(): String? = divider().fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private fun hasStateDescription(text: String) = SemanticsMatcher("state $text") {
        it.config.getOrNull(SemanticsProperties.StateDescription) == text
    }

    private fun SemanticsNodeInteraction.performCustomAction(label: String) {
        val action = fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == label }
        compose.runOnIdle { action.action() }
        compose.waitForIdle()
    }

    private fun clickLabel(label: String) = SemanticsMatcher("has click label $label") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }

    companion object {
        private const val ZMANIM_ROWS = 12
        private const val AGENDA_ROWS = 12

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = Device.entries.map { arrayOf<Any>(it) }
    }
}

/** The TASK-46 time-zone notice across fold/unfold: still shown once, never duplicated or repeated. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FoldContinuityZoneNoticeTest : ScreenshotTest(Variant.EN_LIGHT, FoldableSize.FOLD8_INNER_LANDSCAPE.qualifiers) {

    override suspend fun seedPreferences(preferences: PreferencesRepository) {
        super.seedPreferences(preferences)
        // A fix in Jerusalem while the device's zone is New York
        preferences.updateCurrentLocation(Location(31.7683, 35.2137, 754.0, "Asia/Jerusalem", "Jerusalem"))
    }

    private val title get() = string(R.string.zone_mismatch_title)
    private fun shown() = compose.onAllNodes(hasText(title)).fetchSemanticsNodes().size

    @Test
    fun showsOnceAcrossFoldsAndStaysDismissed() {
        launch(hasText(title))
        for (size in listOf(FoldableSize.FOLD8_COVER, FoldableSize.FOLD8_INNER_LANDSCAPE, FoldableSize.FOLD8_SPLIT_HALF)) {
            recreate("+${size.qualifiers}")
            await(hasText(title))
            assertEquals(1, shown())
        }

        clickable(string(R.string.zone_mismatch_dismiss)).performClick()
        awaitGone(hasText(title))
        for (size in listOf(FoldableSize.FOLD8_COVER, FoldableSize.FOLD8_INNER_LANDSCAPE)) {
            recreate("+${size.qualifiers}")
            await(hasContentDescription(string(R.string.calendar_go_to_date)))
            assertEquals(0, shown())
        }
    }
}
