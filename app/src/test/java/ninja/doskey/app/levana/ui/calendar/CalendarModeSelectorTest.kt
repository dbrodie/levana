package ninja.doskey.app.levana.ui.calendar

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CalendarModeSelectorTest {

    @get:Rule
    val compose = createComposeRule()

    private val selections = mutableListOf<Boolean>()
    private lateinit var inputModeManager: InputModeManager

    /** Hosts the selector; when [stateful], selections update the shown mode like the app does. */
    private fun setSelector(
        hebrewSelected: Boolean,
        stateful: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) {
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme {
                    var hebrew by remember { mutableStateOf(hebrewSelected) }
                    CalendarModeSelector(
                        hebrewSelected = hebrew,
                        onSelectMode = {
                            selections += it
                            if (stateful) hebrew = it
                        }
                    )
                }
            }
        }
    }

    private val hebrew get() = compose.onNode(isSelectable() and hasText(HEBREW))
    private val gregorian get() = compose.onNode(isSelectable() and hasText(GREGORIAN))

    @Test
    fun exposesTwoEnabledRadioOptionsInAGroup() {
        setSelector(hebrewSelected = true)

        val group = SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)
        compose.onAllNodes(isSelectable()).assertCountEquals(2)
        compose.onAllNodes(isSelectable() and hasAnyAncestor(group)).assertCountEquals(2)
        listOf(hebrew, gregorian).forEach {
            it.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)).assertIsEnabled()
        }
        hebrew.assertIsSelected()
        gregorian.assertIsNotSelected()
        // Each label belongs to its option; no separately actionable label nodes.
        compose.onAllNodesWithText(HEBREW).assertCountEquals(1)
        compose.onAllNodesWithText(GREGORIAN).assertCountEquals(1)
    }

    @Test
    fun gregorianSelectedIsExposed() {
        setSelector(hebrewSelected = false)
        gregorian.assertIsSelected().assertIsEnabled()
        hebrew.assertIsNotSelected().assertIsEnabled()
    }

    @Test
    fun activatingSelectedOptionReportsItsOwnMode() {
        setSelector(hebrewSelected = true)
        hebrew.performClick()
        hebrew.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(true, true), selections)
    }

    @Test
    fun choosingOtherOptionReportsItOnce() {
        setSelector(hebrewSelected = true)
        gregorian.performClick()
        assertEquals(listOf(false), selections)
    }

    @Test
    fun accessibilityActionChoosesOption() {
        setSelector(hebrewSelected = false)
        hebrew.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(true), selections)
    }

    @Test
    fun repeatedSelectionNeverInverts() {
        setSelector(hebrewSelected = false, stateful = true)
        gregorian.performClick()
        gregorian.performClick()
        gregorian.assertIsSelected()
        hebrew.assertIsNotSelected()

        hebrew.performClick()
        hebrew.assertIsSelected()
        gregorian.assertIsNotSelected()
        hebrew.performClick()
        hebrew.assertIsSelected()
        assertEquals(listOf(false, false, true, true), selections)
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun keyboardEnterSelectsFocusedOption() {
        setSelector(hebrewSelected = true, stateful = true)
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        gregorian.requestFocus()
        gregorian.assertIsFocused()
        gregorian.performKeyInput { pressKey(Key.Enter) }
        gregorian.assertIsSelected()
        assertEquals(listOf(false), selections)
    }

    @Test
    fun rtlMirrorsOrderAndKeepsSemantics() {
        setSelector(hebrewSelected = true, layoutDirection = LayoutDirection.Rtl)
        hebrew.assertIsSelected()
        gregorian.assertIsNotSelected()
        // Hebrew is the first option, so it starts on the right in RTL.
        assertTrue(hebrew.getBoundsInRoot().left >= gregorian.getBoundsInRoot().right - 1.dp)
        gregorian.performClick()
        assertEquals(listOf(false), selections)
    }

    @Test
    fun ltrStartsWithHebrew() {
        setSelector(hebrewSelected = true)
        assertTrue(hebrew.getBoundsInRoot().right <= gregorian.getBoundsInRoot().left + 1.dp)
    }

    @Test
    fun optionsKeepMinimumTouchTarget() {
        setSelector(hebrewSelected = true)
        listOf(hebrew, gregorian).forEach {
            // The visual button is 40dp; the standard minimum touch target extends it to 48dp.
            val node = it.fetchSemanticsNode()
            val minTouch = with(node.layoutInfo.density) { 48.dp.toPx() }
            assertTrue(node.touchBoundsInRoot.height >= minTouch)
        }
    }

    private companion object {
        const val HEBREW = "Hebrew"
        const val GREGORIAN = "Gregorian"
    }
}
