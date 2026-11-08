package ninja.doskey.app.levana.screenshots

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.db.PersonalEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The two-pane Agenda in the app (Fold 8 inner display, landscape): only a day's date rail
 * changes the day panel (TASK-59). Today, Go to date and a search result scroll the list; cards
 * open what they open on a phone; back on the Agenda the panel still shows the tapped day.
 * [TODAY] is Tuesday 17 November 2026; the panel's Shabbat, the 21st, reads Vayetzei.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AgendaTwoPaneSelectionTest : ScreenshotTest(Variant.EN_LIGHT, FoldableSize.FOLD8_INNER_LANDSCAPE.qualifiers) {

    private fun day(description: String) = hasContentDescription(description, substring = true)

    private fun assertSelected(description: String) = await(day(description) and isSelected())

    private fun card(description: String) =
        compose.onAllNodes(hasContentDescription(description, substring = true) and hasClickAction())[0]

    private fun shownTexts(): List<String> = compose.onAllNodes(onScreen(), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .flatMap { node -> node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } }

    private fun awaitShown(text: String) = awaitUntil("$text on screen") { text in shownTexts() }

    private fun back() = onActivity { it.onBackPressedDispatcher.onBackPressed() }

    /** The Agenda with Shabbat the 21st chosen by its date rail. */
    private fun agendaOnShabbat() {
        runBlocking {
            db.personalEventDao().insert(PersonalEvent(title = "Anniversary", hebrewDay = 9, hebrewMonth = JewishDate.KISLEV, hebrewYear = 5770))
        }
        launchCalendar()
        openDrawer()
        clickable(string(R.string.nav_agenda)).performClick()
        await(hasScrollToIndexAction())
        await(day("November 21, 2026") and hasClickAction())
        compose.onAllNodes(day("November 21, 2026") and hasClickAction())[0].performClick()
        assertSelected("November 21, 2026")
        awaitShown("Vayetzei")
    }

    @Test
    fun todayAndGoToDateOnlyScroll() {
        agendaOnShabbat()

        compose.onNode(hasContentDescription(string(R.string.calendar_today))).performClick()
        compose.waitForIdle()
        assertSelected("November 21, 2026")
        assertEquals(0, compose.onAllNodes(day("November 17, 2026") and isSelected()).fetchSemanticsNodes().size)

        // The picker opens on the day at the top, today; OK goes there
        compose.onNode(hasContentDescription(string(R.string.calendar_go_to_date))).performClick()
        await(hasText(string(R.string.date_picker_title)))
        clickable(string(R.string.common_ok)).performClick()
        compose.waitForIdle()
        assertSelected("November 21, 2026")
        assertEquals(0, compose.onAllNodes(day("November 17, 2026") and isSelected()).fetchSemanticsNodes().size)
        awaitShown("Vayetzei")
    }

    @Test
    fun aSearchResultOnlyScrollsAndAHolidayCardOpensTheMonth() {
        agendaOnShabbat()

        compose.onNode(hasContentDescription(string(R.string.agenda_search))).performClick()
        await(hasSetTextAction())
        compose.onNode(hasSetTextAction()).performTextReplacement("Chanukah")
        // The first result: the first day of Chanukah 5786, a year before
        await(day("December 15, 2025"))
        card("Chanukah").performClick()
        awaitGone(hasSetTextAction())
        await(day("December 15, 2025") and hasClickAction())
        // The list is far from the 21st, whose Shabbat only the panel still shows; nothing in view is selected
        awaitShown("Vayetzei")
        assertEquals(0, compose.onAllNodes(isSelected() and day("2025")).fetchSemanticsNodes().size)

        // A holiday card opens the Month on its day, as on a phone
        card("Chanukah").performClick()
        await(hasContentDescription(string(R.string.calendar_go_to_date)))
        assertSelected("December 15, 2025")
    }

    @Test
    fun zmanimAndEditorCardsLeaveThePanelsDay() {
        agendaOnShabbat()

        // Friday's candle lighting: that day's Zmanim
        card(string(R.string.agenda_candle_lighting)).performClick()
        await(hasText(string(R.string.zmanim_category_morning)))
        back()
        assertSelected("November 21, 2026")
        assertEquals(0, compose.onAllNodes(day("November 20, 2026") and isSelected()).fetchSemanticsNodes().size)
        awaitShown("Vayetzei")

        // Thursday's event: its editor
        card("Anniversary").performClick()
        awaitText(string(R.string.design_edit_event))
        back()
        assertSelected("November 21, 2026")
        assertEquals(0, compose.onAllNodes(day("November 19, 2026") and isSelected()).fetchSemanticsNodes().size)
        awaitShown("Vayetzei")
    }
}
