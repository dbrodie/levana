package ninja.doskey.app.levana.ui.events

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.domain.model.ContactBirthday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DeleteActionLabelsTest {

    @get:Rule
    val compose = createComposeRule()

    private val edited = mutableListOf<Any>()
    private val deleted = mutableListOf<Any>()
    private lateinit var inputModeManager: InputModeManager

    private fun show(layoutDirection: LayoutDirection = LayoutDirection.Ltr, content: @Composable () -> Unit) {
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme { content() }
            }
        }
    }

    private fun showBirthdays(
        birthdays: List<ContactBirthday> = listOf(SARAH, DAVID),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) = show(layoutDirection) {
        BirthdaysTab(
            birthdays = birthdays,
            hasPermission = true,
            onEditBirthday = { edited += it },
            onDeleteBirthday = { deleted += it },
            onRequestPermission = {}
        )
    }

    private fun showEvents(events: List<PersonalEvent> = listOf(ANNIVERSARY, TRICKY)) = show {
        CustomEventsTab(
            events = events,
            isLoading = false,
            onEditEvent = { edited += it },
            onDeleteEvent = { deleted += it }
        )
    }

    private fun isButton() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    @Test
    fun birthdayDeleteButtonsNameTheirContact() {
        showBirthdays()

        compose.onNodeWithContentDescription("Remove Hebrew birthday for Sarah")
            .assert(isButton())
            .assertHasClickAction()
        compose.onNodeWithContentDescription("Remove Hebrew birthday for David").assert(isButton())
        compose.onAllNodesWithContentDescription("Delete").assertCountEquals(0)
        // The label is accessibility-only: never shown as text.
        compose.onAllNodes(hasText("Remove Hebrew birthday for Sarah")).assertCountEquals(0)
    }

    @Test
    fun birthdayRemoveConfirmsAndTargetsThatContact() {
        showBirthdays()

        compose.onNodeWithContentDescription("Remove Hebrew birthday for Sarah").performClick()
        // Existing dialog wording is unchanged.
        compose.onNodeWithText("Remove Birthday").assertExists()
        compose.onNodeWithText("Remove Hebrew birthday for Sarah?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(deleted.isEmpty())
        compose.onAllNodes(hasText("Remove Birthday")).assertCountEquals(0)

        compose.onNodeWithContentDescription("Remove Hebrew birthday for Sarah").performClick()
        compose.onNodeWithText("Remove").performClick()
        assertEquals(listOf<Any>("sarah-key"), deleted)
        assertTrue(edited.isEmpty())
    }

    @Test
    fun birthdayCardOpensEditorNotDelete() {
        showBirthdays()
        compose.onNodeWithText("David").performClick()
        assertEquals(listOf<Any>("david-key"), edited)
        assertTrue(deleted.isEmpty())
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun keyboardActivatesDeleteButton() {
        showBirthdays()
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        val button = compose.onNodeWithContentDescription("Remove Hebrew birthday for David")
        button.requestFocus()
        button.assertIsFocused()
        button.performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithText("Remove Hebrew birthday for David?").assertExists()
        compose.onNodeWithText("Remove").performClick()
        assertEquals(listOf<Any>("david-key"), deleted)
    }

    @Test
    fun eventDeleteButtonsNameTheirEvent() {
        showEvents()

        compose.onNodeWithContentDescription("Delete event Anniversary").assert(isButton())
        // User text is a format argument: % and quotes come through literally.
        compose.onNodeWithContentDescription("Delete event 100% \"Siyum\" %1\$s").assert(isButton())
        compose.onAllNodesWithContentDescription("Delete").assertCountEquals(0)
        compose.onAllNodes(hasText("Delete event Anniversary")).assertCountEquals(0)
    }

    @Test
    fun eventDeleteConfirmsAndTargetsThatEvent() {
        showEvents()

        compose.onNodeWithContentDescription("Delete event 100% \"Siyum\" %1\$s").performClick()
        compose.onNodeWithText("Delete Event").assertExists()
        compose.onNodeWithText("Delete \"100% \"Siyum\" %1\$s\"?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(deleted.isEmpty())

        compose.onNodeWithContentDescription("Delete event Anniversary").performClick()
        compose.onNodeWithText("Delete").performClick()
        assertEquals(listOf<Any>(ANNIVERSARY), deleted)

        compose.onNodeWithText("Anniversary").performClick()
        assertEquals(listOf<Any>(ANNIVERSARY.id), edited)
    }

    @Test
    fun blankNamesFallBackToGenericLabel() {
        showBirthdays(birthdays = listOf(SARAH.copy(contactName = " ")))
        compose.onNodeWithContentDescription("Delete").assert(isButton())
    }

    @Test
    fun blankEventTitleFallsBackToGenericLabel() {
        showEvents(events = listOf(ANNIVERSARY.copy(title = "")))
        compose.onNodeWithContentDescription("Delete").assert(isButton())
    }

    @Test
    @Config(qualifiers = "iw")
    fun hebrewLabelsInRtl() {
        showBirthdays(layoutDirection = LayoutDirection.Rtl)

        val button = compose.onNodeWithContentDescription("הסרת יום ההולדת העברי של Sarah")
        button.assert(isButton())
        // RTL mirrors the row: the trash button sits left of the name.
        val name = compose.onNodeWithText("Sarah", useUnmergedTree = true).getBoundsInRoot()
        assertTrue(button.getBoundsInRoot().right <= name.left)

        button.performClick()
        compose.onNodeWithText("להסיר את יום ההולדת העברי של Sarah?").assertExists()
        compose.onNodeWithText("הסרה").performClick()
        assertEquals(listOf<Any>("sarah-key"), deleted)
    }

    @Test
    @Config(qualifiers = "iw")
    fun hebrewEventLabel() {
        showEvents()
        compose.onAllNodes(hasContentDescription("מחיקת האירוע Anniversary") and isButton())
            .assertCountEquals(1)
    }

    private companion object {
        val SARAH = ContactBirthday("sarah-key", "Sarah", null, 10, 9, 5750)
        val DAVID = ContactBirthday("david-key", "David", null, 3, 1, 0)
        val ANNIVERSARY = PersonalEvent(id = 1, title = "Anniversary", hebrewDay = 5, hebrewMonth = 7, hebrewYear = 5780)
        val TRICKY = PersonalEvent(id = 2, title = "100% \"Siyum\" %1\$s", hebrewDay = 6, hebrewMonth = 8, hebrewYear = 5781)
    }
}
