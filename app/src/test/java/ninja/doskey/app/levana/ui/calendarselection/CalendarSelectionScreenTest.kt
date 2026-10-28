package ninja.doskey.app.levana.ui.calendarselection

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
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.domain.model.DeviceCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CalendarSelectionScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val intents = mutableListOf<CalendarSelectionIntent>()
    private lateinit var inputModeManager: InputModeManager

    /** Hosts the content with selection state that follows ToggleCalendar, like the ViewModel. */
    private fun setContent(
        selected: Set<Long> = setOf(WORK.id),
        hasPermission: Boolean = true,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) {
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            var selectedIds by remember { mutableStateOf(selected) }
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme {
                    CalendarSelectionContent(
                        state = CalendarSelectionState(
                            calendars = listOf(PERSONAL, WORK),
                            selectedIds = selectedIds,
                            hasPermission = hasPermission,
                            isLoading = false
                        ),
                        onIntent = { intent ->
                            intents += intent
                            if (intent is CalendarSelectionIntent.ToggleCalendar) {
                                val id = intent.calendarId
                                selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                            }
                        }
                    )
                }
            }
        }
    }

    private fun rowFor(calendar: DeviceCalendar) =
        compose.onNode(isToggleable() and hasText(calendar.name))

    private fun toggled(calendar: DeviceCalendar) =
        listOf(CalendarSelectionIntent.ToggleCalendar(calendar.id))

    @Test
    fun eachCalendarIsOneLabeledCheckbox() {
        setContent()

        compose.onAllNodes(isToggleable()).assertCountEquals(2)
        rowFor(PERSONAL)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(hasTexts(PERSONAL.name, PERSONAL.accountName))
            .assertIsOff()
        rowFor(WORK)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(hasTexts(WORK.name, WORK.accountName))
            .assertIsOn()
        // Only the rows are actionable; the checkboxes have no handler of their own.
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(2)
        compose.onAllNodes(isToggleable(), useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun tapOnNameTogglesOnce() {
        setContent()
        compose.onNodeWithText(PERSONAL.name, useUnmergedTree = true).performClick()
        assertEquals(toggled(PERSONAL), intents)
        rowFor(PERSONAL).assertIsOn()
    }

    @Test
    fun tapOnAccountTogglesOnce() {
        setContent()
        compose.onNodeWithText(WORK.accountName, useUnmergedTree = true).performClick()
        assertEquals(toggled(WORK), intents)
        rowFor(WORK).assertIsOff()
    }

    @Test
    fun tapOnCheckboxAreaTogglesOnce() {
        setContent()
        // The checkbox sits at the trailing end (right edge in LTR).
        rowFor(PERSONAL).performTouchInput { click(centerRight.copy(x = right - 36f)) }
        assertEquals(toggled(PERSONAL), intents)
    }

    @Test
    fun tapOnColorDotAreaTogglesOnce() {
        setContent()
        // The decorative color dot sits at the leading end.
        rowFor(PERSONAL).performTouchInput { click(centerLeft.copy(x = left + 24f)) }
        assertEquals(toggled(PERSONAL), intents)
    }

    @Test
    fun repeatedTapsToggleBackAndForth() {
        setContent()
        rowFor(PERSONAL).performClick()
        rowFor(PERSONAL).assertIsOn()
        rowFor(PERSONAL).performClick()
        rowFor(PERSONAL).assertIsOff()
        assertEquals(toggled(PERSONAL) + toggled(PERSONAL), intents)
    }

    @Test
    fun accessibilityActionTogglesOnce() {
        setContent()
        rowFor(WORK).performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(toggled(WORK), intents)
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun keyboardEnterTogglesOnce() {
        setContent()
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        rowFor(PERSONAL).requestFocus()
        rowFor(PERSONAL).assertIsFocused()
        rowFor(PERSONAL).performKeyInput { pressKey(Key.Enter) }
        assertEquals(toggled(PERSONAL), intents)
    }

    @Test
    fun rtlKeepsSemanticsAndMirrorsLayout() {
        setContent(layoutDirection = LayoutDirection.Rtl)
        rowFor(WORK).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).assertIsOn()

        val label = compose.onNodeWithText(WORK.name, useUnmergedTree = true).getBoundsInRoot()
        val rowBounds = rowFor(WORK).getBoundsInRoot()
        // In RTL the color dot and label start at the right, leaving the checkbox on the left.
        assertTrue(label.left > (rowBounds.left + rowBounds.right) / 2 - 1.dp)

        rowFor(WORK).performTouchInput { click(centerLeft.copy(x = left + 36f)) }
        assertEquals(toggled(WORK), intents)
    }

    @Test
    fun withoutPermissionShowsGrantButtonAndNoRows() {
        setContent(hasPermission = false)
        compose.onNodeWithText("Grant Permission").assertExists()
        compose.onAllNodes(isToggleable()).assertCountEquals(0)
        assertTrue(intents.isEmpty())
    }

    private fun hasTexts(vararg texts: String) = SemanticsMatcher("Text == ${texts.toList()}") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.map { it.text } == texts.toList()
    }

    private companion object {
        val PERSONAL = DeviceCalendar(id = 1L, name = "Personal", accountName = "me@example.com", color = 0xFF3F51B5.toInt())
        val WORK = DeviceCalendar(id = 2L, name = "Work", accountName = "work@example.com", color = 0xFFE91E63.toInt())
    }
}
