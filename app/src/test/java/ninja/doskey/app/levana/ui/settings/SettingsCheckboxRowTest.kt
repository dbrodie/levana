package ninja.doskey.app.levana.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.data.ZmanimRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsCheckboxRowTest {

    @get:Rule
    val compose = createComposeRule()

    private val changes = mutableListOf<Boolean>()
    private lateinit var inputModeManager: InputModeManager

    private fun setRow(
        checked: Boolean = false,
        enabled: Boolean = true,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) {
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme {
                    SettingsCheckboxRow(
                        title = TITLE,
                        checked = checked,
                        enabled = enabled,
                        onCheckedChange = { changes += it },
                        modifier = Modifier.width(360.dp)
                    )
                }
            }
        }
    }

    private val row get() = compose.onNode(isToggleable())

    @Test
    fun exposesOneLabeledCheckbox() {
        setRow(checked = true)

        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assert(hasTextExactly(TITLE))
            .assertIsOn()
            .assertIsEnabled()
        // Only the row is actionable; the checkbox child has no handler of its own.
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodes(isToggleable(), useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun uncheckedStateIsExposed() {
        setRow(checked = false)
        row.assertIsOff()
    }

    @Test
    fun tapOnLabelTogglesOnce() {
        setRow(checked = false)
        compose.onNodeWithText(TITLE, useUnmergedTree = true).performClick()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun tapOnCheckboxAreaTogglesOnce() {
        setRow(checked = true)
        // The checkbox sits at the trailing end (right edge in LTR).
        row.performTouchInput { click(centerRight.copy(x = right - 36f)) }
        assertEquals(listOf(false), changes)
    }

    @Test
    fun tapOnRowTogglesOnce() {
        setRow(checked = false)
        row.performClick()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun accessibilityActionToggles() {
        setRow(checked = false)
        row.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(true), changes)
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun keyboardEnterToggles() {
        setRow(checked = false)
        // Rows are focusable in keyboard mode, as with a hardware keyboard or D-pad.
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        row.requestFocus()
        row.assertIsFocused()
        row.performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(true), changes)
    }

    @Test
    fun disabledRowDoesNotToggle() {
        setRow(checked = false, enabled = false)
        row.assertIsNotEnabled()
        row.performClick()
        compose.onNodeWithText(TITLE, useUnmergedTree = true).performClick()
        row.performTouchInput { click(centerRight.copy(x = right - 36f)) }
        assertTrue(changes.isEmpty())
    }

    @Test
    fun rtlKeepsSemanticsAndMirrorsCheckbox() {
        setRow(checked = true, layoutDirection = LayoutDirection.Rtl)
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).assertIsOn()

        val label = compose.onNodeWithText(TITLE, useUnmergedTree = true).getBoundsInRoot()
        val rowBounds = row.getBoundsInRoot()
        // In RTL the label starts at the right, leaving the checkbox on the left.
        assertTrue(label.left > (rowBounds.left + rowBounds.right) / 2 - 1.dp)

        // A tap on the left (checkbox) end toggles once.
        row.performTouchInput { click(centerLeft.copy(x = left + 36f)) }
        assertEquals(listOf(false), changes)
    }

    @Test
    fun leadingContentStaysInsideTheSingleTarget() {
        compose.setContent {
            MaterialTheme {
                SettingsCheckboxRow(
                    title = TITLE,
                    description = DESCRIPTION,
                    checked = false,
                    onCheckedChange = { changes += it },
                    leadingContent = { Text(LEADING) },
                    modifier = Modifier.width(360.dp)
                )
            }
        }

        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        row.assert(hasTextExactly(LEADING, TITLE, DESCRIPTION))
        compose.onNodeWithText(LEADING, useUnmergedTree = true).performClick()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun halachicTimesSectionExposesOneCheckboxPerZman() {
        val toggles = mutableListOf<Pair<String, Boolean>>()
        val first = ZmanimRepository.ZMAN_NAMES.first().english
        compose.setContent {
            MaterialTheme {
                var selected by remember { mutableStateOf(setOf(first)) }
                HalachicTimesSection(selectedZmanim = selected) { name, enabled ->
                    toggles += name to enabled
                    selected = if (enabled) selected + name else selected - name
                }
            }
        }

        compose.onAllNodes(isToggleable()).assertCountEquals(ZmanimRepository.ZMAN_NAMES.size)
        val firstRow = compose.onAllNodes(isToggleable())[0]
        firstRow.assertIsOn().performClick()
        assertEquals(listOf(first to false), toggles)
        firstRow.assertIsOff()
    }

    private fun hasTextExactly(vararg texts: String) = SemanticsMatcher("Text == ${texts.toList()}") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.map { it.text } == texts.toList()
    }

    private companion object {
        const val TITLE = "Sunrise"
        const val DESCRIPTION = "Netz"
        const val LEADING = "Dot"
    }
}
