package ninja.doskey.app.levana.ui.walkthrough

import android.app.Application
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.MainActivity
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.notifications.NotificationPoster
import ninja.doskey.app.levana.screenshots.ScreenshotTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The walkthrough in the real [MainActivity] (TASK-43): when it opens, deep links, Skip,
 * a single Got it screen, replay from About and its TalkBack labels. No images are captured.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = ScreenshotTest.DEVICE)
class WalkthroughLaunchTest : ScreenshotTest(Variant.EN_LIGHT) {

    private val allIds = WalkthroughRegistry.screens.map { it.id }

    // So the zmanim screen asks for location
    override val locationPermission = false
    private val menu get() = hasContentDescription(string(R.string.design_open_menu))
    private val welcome get() = string(R.string.walkthrough_welcome_title)

    /** What the app does outside tests; set before [launch]. */
    private fun enableWalkthrough() {
        koin.loadModules(listOf(module { single { WalkthroughGate(showOnLaunch = true) } }), allowOverride = true)
    }

    private fun seen() = runBlocking { preferences.walkthroughSeenIds.first() }

    private fun awaitSeen(expected: Set<String>) {
        val deadline = System.currentTimeMillis() + 10_000
        while (seen() != expected) {
            check(System.currentTimeMillis() < deadline) { "seen ${seen()}, expected $expected" }
            Thread.sleep(10)
        }
    }

    @Test
    fun testsNeverSeeItUnlessTheyAsk() {
        launchCalendar()
        assertTrue(compose.onAllNodes(hasText(welcome)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun aNormalLaunchOpensItAndSkipLeadsToTheCalendar() {
        enableWalkthrough()
        launch(hasText(welcome))
        clickable(string(R.string.walkthrough_skip)).performClick()
        await(menu)
        awaitSeen(allIds.toSet())
    }

    @Test
    fun aDeepLinkOpensTheCalendarAndLeavesTheWalkthroughForLater() {
        enableWalkthrough()
        val intent = Intent(app, MainActivity::class.java)
            .putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, TODAY.plusDays(3).toEpochDay())
        launch(menu, intent)
        assertTrue(compose.onAllNodes(hasText(welcome)).fetchSemanticsNodes().isEmpty())
        assertEquals(emptySet<String>(), seen())
    }

    @Test
    fun oneNewScreenShowsOnlyGotIt() {
        runBlocking { preferences.markWalkthroughSeen(allIds - "appearance") }
        enableWalkthrough()
        launch(hasText(string(R.string.walkthrough_appearance_title)))
        for (absent in listOf(R.string.walkthrough_skip, R.string.walkthrough_next, R.string.walkthrough_back)) {
            assertTrue(compose.onAllNodes(hasText(string(absent))).fetchSemanticsNodes().isEmpty())
        }
        clickable(string(R.string.walkthrough_got_it)).performClick()
        await(menu)
        awaitSeen(allIds.toSet())
    }

    @Test
    fun pagesAnnounceTheirPositionAndControlsAreLabelled() {
        enableWalkthrough()
        launch(hasText(welcome))
        await(hasText(welcome) and hasStateDescription(string(R.string.walkthrough_page_of, 1, allIds.size)) and isHeading())
        clickable(string(R.string.walkthrough_next)).performClick()
        val second = string(R.string.walkthrough_location_title)
        await(hasText(second) and hasStateDescription(string(R.string.walkthrough_page_of, 2, allIds.size)) and onScreen())
        clickable(string(R.string.walkthrough_back))
        clickable(string(R.string.walkthrough_skip))
        // The zmanim screen asks for location with a reason, and never blocks
        await(hasText(string(R.string.walkthrough_permission_location)))
        clickable(string(R.string.walkthrough_not_now)).performClick()
        await(hasText(string(R.string.walkthrough_days_title)) and onScreen())
    }

    @Test
    fun replayFromAboutReturnsToAboutAndKeepsTheSeenState() {
        runBlocking { preferences.markWalkthroughSeen(allIds.take(2)) }
        openSettings()
        clickable(string(R.string.settings_about)).performScrollTo().performClick()
        awaitText(VERSION_NAME)
        clickable(string(R.string.walkthrough_show_again)).performClick()
        await(hasText(welcome))
        clickable(string(R.string.walkthrough_next)).performClick()
        await(hasText(string(R.string.walkthrough_location_title)) and onScreen())
        clickable(string(R.string.walkthrough_skip)).performClick()
        await(hasText(string(R.string.walkthrough_show_again)))
        await(hasText(string(R.string.about_version)))
        assertEquals(allIds.take(2).toSet(), seen())
    }

    private fun hasStateDescription(value: String) = SemanticsMatcher("has state description $value") {
        it.config.getOrNull(SemanticsProperties.StateDescription) == value
    }

    private fun isHeading() = SemanticsMatcher("is a heading") {
        it.config.getOrNull(SemanticsProperties.Heading) != null
    }
}
