package ninja.doskey.app.levana.ui.walkthrough

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.screenshots.ScreenshotTest
import ninja.doskey.app.levana.ui.theme.LevanaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TASK-65: while the current page can still scroll down, Next, Get started and Got it scroll
 * it by about a viewport instead of moving on; at the bottom, or on a page that fits, they act
 * as before. Every tap makes progress, so TalkBack can never get stuck.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp-mdpi")
class WalkthroughNextScrollTest {

    @get:Rule
    val compose = createComposeRule()

    private val sent = mutableListOf<WalkthroughIntent>()
    private val forwardIntents get() = sent.filter { it == WalkthroughIntent.Next || it == WalkthroughIntent.Finish }

    private fun str(id: Int): String = ApplicationProvider.getApplicationContext<Application>().getString(id)

    private fun screen(id: String, illustrationHeight: Int) = FeatureScreen(
        id = id,
        title = R.string.walkthrough_welcome_title,
        body = R.string.walkthrough_welcome_body,
        illustration = { Box(Modifier.fillMaxWidth().height(illustrationHeight.dp)) },
    )

    private val tall = screen("tall", illustrationHeight = 2_000)
    private val short = screen("short", illustrationHeight = 10)

    /** The walkthrough with the view model's paging: Next, Back and a settled swipe move the page. */
    private fun show(screens: List<FeatureScreen>, page: Int = 0, reducedMotion: Boolean = false) {
        compose.setContent {
            var state by remember { mutableStateOf(WalkthroughState(decided = true, screens = screens, page = page)) }
            LevanaTheme(darkTheme = false) {
                WalkthroughContent(
                    state = state,
                    onIntent = { intent ->
                        sent += intent
                        state = when (intent) {
                            WalkthroughIntent.Next -> state.copy(page = (state.page + 1).coerceAtMost(screens.lastIndex))
                            WalkthroughIntent.Back -> state.copy(page = (state.page - 1).coerceAtLeast(0))
                            is WalkthroughIntent.PageShown -> state.copy(page = intent.index)
                            else -> state
                        }
                    },
                    onRequestPermission = {},
                    reducedMotion = reducedMotion,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun button(@androidx.annotation.StringRes text: Int): SemanticsNodeInteraction =
        compose.onNode(hasText(str(text)) and hasClickAction())

    /** The current page's scroll (value to max): the vertical scroll that starts on screen. */
    private fun scroll(): Pair<Float, Float> {
        val node = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .fetchSemanticsNodes()
            .single { it.boundsInRoot.left >= 0f && it.boundsInRoot.left < compose.onRoot().fetchSemanticsNode().size.width }
        val range = node.config[SemanticsProperties.VerticalScrollAxisRange]
        return range.value() to range.maxValue()
    }

    /** Taps [text] until it sends an intent; returns the taps that only scrolled. */
    private fun tapUntilSent(@androidx.annotation.StringRes text: Int): Int {
        repeat(MAX_TAPS) { taps ->
            val before = forwardIntents.size
            button(text).performClick()
            compose.waitForIdle()
            if (forwardIntents.size > before) return taps
        }
        error("${str(text)} never moved on in $MAX_TAPS taps")
    }

    @Test
    fun nextScrollsAPageWithMoreBelowThenAdvancesAtTheBottom() {
        show(listOf(tall, short))
        val (start, max) = scroll()
        assertEquals(0f, start)
        assertTrue("the tall page scrolls", max > 0f)

        button(R.string.walkthrough_next).performClick()
        compose.waitForIdle()
        assertEquals(emptyList<WalkthroughIntent>(), forwardIntents)
        val (afterOne, _) = scroll()
        assertTrue("scrolled down, not to the end", afterOne > 0f && afterOne < max)

        val scrollTaps = tapUntilSent(R.string.walkthrough_next)
        assertTrue("several viewports of content take several taps", scrollTaps >= 1)
        assertEquals(listOf(WalkthroughIntent.Next), forwardIntents)
    }

    @Test
    fun aPageThatFitsAdvancesOnTheFirstTap() {
        show(listOf(short, tall))
        assertEquals(0f, scroll().second)
        button(R.string.walkthrough_next).performClick()
        compose.waitForIdle()
        assertEquals(listOf(WalkthroughIntent.Next), forwardIntents)
    }

    @Test
    fun getStartedScrollsFirstThenFinishes() {
        show(listOf(short, tall), page = 1)
        assertTrue(tapUntilSent(R.string.walkthrough_get_started) > 0)
        assertEquals(listOf(WalkthroughIntent.Finish), forwardIntents)
        val (value, max) = scroll()
        assertEquals(max, value)
    }

    @Test
    fun gotItScrollsFirstThenFinishes() {
        show(listOf(tall))
        assertTrue(tapUntilSent(R.string.walkthrough_got_it) > 0)
        assertEquals(listOf(WalkthroughIntent.Finish), forwardIntents)
    }

    @Test
    fun backIsUnchangedOnAPageWithMoreBelow() {
        show(listOf(short, tall), page = 1)
        button(R.string.walkthrough_back).performClick()
        compose.waitForIdle()
        assertTrue(WalkthroughIntent.Back in sent)
    }

    @Test
    fun reducedMotionScrollsWithoutAnimating() {
        show(listOf(tall, short), reducedMotion = true)
        compose.mainClock.autoAdvance = false
        button(R.string.walkthrough_next).performClick()
        compose.waitForIdle()
        assertTrue("already scrolled, with no frames run", scroll().first > 0f)
    }

    @Test
    fun withMotionTheScrollAnimates() {
        show(listOf(tall, short))
        compose.mainClock.autoAdvance = false
        button(R.string.walkthrough_next).performClick()
        compose.waitForIdle()
        val (atTap, _) = scroll()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertNotEquals(atTap, scroll().first)
    }

    /** TalkBack hears "double tap to show more" while Next scrolls, and its action does the same. */
    @Test
    fun talkBackActionSaysShowMoreAndAlwaysProgresses() {
        show(listOf(tall, short))
        val next = button(R.string.walkthrough_next)
        assertEquals(str(R.string.walkthrough_show_more), next.fetchSemanticsNode().config[SemanticsActions.OnClick].label)

        repeat(MAX_TAPS) {
            if (forwardIntents.isNotEmpty()) return@repeat
            next.performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
        }
        assertEquals(listOf(WalkthroughIntent.Next), forwardIntents)
    }

    @Test
    fun talkBackActionHasNoLabelOnceAtTheBottom() {
        show(listOf(short, tall))
        assertEquals(null, button(R.string.walkthrough_next).fetchSemanticsNode().config[SemanticsActions.OnClick].label)
    }

    /** A real permission page at 200% on a small phone: as it opens, and after one Next. */
    @Test
    @Config(qualifiers = "+h640dp", fontScale = 2f)
    fun largeFontPermissionPageScrollsOnNext() {
        val reminders = WalkthroughRegistry.screens.first { it.id == "reminders" }
        show(listOf(reminders, short))
        assertTrue("the page scrolls at 200%", scroll().second > 0f)
        compose.onRoot().captureRoboImage("$DIR/walkthrough_next_scroll_large_font_top.png")
        button(R.string.walkthrough_next).performClick()
        compose.waitForIdle()
        assertEquals(emptyList<WalkthroughIntent>(), forwardIntents)
        compose.onRoot().captureRoboImage("$DIR/walkthrough_next_scroll_large_font_scrolled.png")
    }

    private companion object {
        const val MAX_TAPS = 20
        const val DIR = ScreenshotTest.OUTPUT_DIR
    }
}
