package ninja.doskey.app.levana.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import ninja.doskey.app.levana.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsStepperRowTest {

    @get:Rule
    val compose = createComposeRule()

    private val values = mutableListOf<Int>()
    private val intents = mutableListOf<SettingsIntent>()
    private lateinit var inputModeManager: InputModeManager

    private fun str(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Application>().getString(id, *args)

    private fun setRow(
        value: Int,
        range: IntRange = 360..720,
        step: Int = 30,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) {
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme {
                    SettingsStepperRow(
                        title = TITLE,
                        valueText = "Value $value",
                        value = value,
                        range = range,
                        step = step,
                        decreaseAction = "Earlier",
                        increaseAction = "Later",
                        onValueChange = { values += it },
                        modifier = Modifier.width(360.dp)
                    )
                }
            }
        }
    }

    private val decrease get() = compose.onNodeWithContentDescription("Earlier, $TITLE")
    private val increase get() = compose.onNodeWithContentDescription("Later, $TITLE")

    @Test
    fun buttonsAreSeparateContextualControlsWithCurrentValue() {
        setRow(value = 480)

        // Exactly two actionable nodes: the decrease and increase buttons.
        compose.onAllNodes(hasClickAction()).assertCountEquals(2)
        decrease.assert(hasStateDescription("Value 480")).assertIsEnabled()
        increase.assert(hasStateDescription("Value 480")).assertIsEnabled()
        // The title and value are read together as one non-actionable node.
        compose.onNodeWithText(TITLE).assert(hasTexts(TITLE, "Value 480"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun lowerBoundDisablesOnlyDecrease() {
        setRow(value = 360)
        decrease.assertIsNotEnabled()
        increase.assertIsEnabled()
        decrease.performClick()
        assertTrue(values.isEmpty())
        increase.performClick()
        assertEquals(listOf(390), values)
    }

    @Test
    fun upperBoundDisablesOnlyIncrease() {
        setRow(value = 720)
        increase.assertIsNotEnabled()
        decrease.assertIsEnabled()
        increase.performClick()
        assertTrue(values.isEmpty())
        decrease.performClick()
        assertEquals(listOf(690), values)
    }

    @Test
    fun eachActivationStepsOnce() {
        setRow(value = 3, range = 0..14, step = 1)
        decrease.performClick()
        increase.performClick()
        assertEquals(listOf(2, 4), values)
    }

    @Test
    fun nonAlignedValueIsClampedToRange() {
        setRow(value = 370)
        decrease.performClick()
        assertEquals(listOf(360), values)
    }

    @Test
    fun accessibilityActionStepsOnce() {
        setRow(value = 480)
        increase.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(510), values)
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun keyboardEnterOnFocusedButtonStepsOnce() {
        setRow(value = 480)
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        increase.requestFocus()
        increase.assertIsFocused()
        increase.performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(510), values)
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun keyboardFocusStaysInRowWhenReachingBound() {
        // Hoisted value, as in SettingsScreen, so the button really becomes disabled.
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            MaterialTheme {
                var value by remember { mutableStateOf(690) }
                Column {
                    // Another focusable control before the row, like the toolbar's Back button.
                    TextButton(onClick = {}) { Text("Back") }
                    SettingsStepperRow(
                        title = TITLE,
                        valueText = "Value $value",
                        value = value,
                        range = 360..720,
                        step = 30,
                        decreaseAction = "Earlier",
                        increaseAction = "Later",
                        onValueChange = { values += it; value = it }
                    )
                }
            }
        }
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        increase.requestFocus()
        increase.performKeyInput { pressKey(Key.Enter) }

        assertEquals(listOf(720), values)
        increase.assertIsNotEnabled()
        decrease.assertIsFocused()
        decrease.performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(720, 690), values)
    }

    @Test
    fun touchReachingBoundDoesNotMoveFocus() {
        setRow(value = 690)
        increase.performClick()
        assertEquals(listOf(720), values)
        decrease.assertIsNotFocused()
    }

    @Test
    fun rtlMirrorsButtonOrder() {
        setRow(value = 480, layoutDirection = LayoutDirection.Rtl)
        // Decrease stays at the start edge, which is the right in RTL.
        assertTrue(decrease.getBoundsInRoot().left > increase.getBoundsInRoot().left)
        decrease.performClick()
        assertEquals(listOf(450), values)
    }

    @Test
    @Config(qualifiers = "iw")
    fun hebrewLabelNamesActionAndSetting() {
        assertEquals("מוקדם יותר, שעת ההתראה", stepperLabel(R.string.settings_earlier, R.string.settings_notification_time))
        assertEquals("הגדלה, ימים לפני", stepperLabel(R.string.settings_increase, R.string.settings_days_before))
    }

    // --- NotificationsSection wiring: exact bounds, steps, labels and intents. ---

    private fun setSection(state: SettingsState) {
        compose.setContent {
            MaterialTheme {
                // The section emits rows into the caller's Column, as in SettingsScreen.
                Column {
                    NotificationsSection(state = state, onIntent = { intents += it }, onShowModeDialog = {})
                }
            }
        }
    }

    private fun stepperLabel(action: Int, title: Int) =
        str(R.string.settings_stepper_action, str(action), str(title))

    private fun button(action: Int, title: Int): SemanticsNodeInteraction =
        compose.onNodeWithContentDescription(stepperLabel(action, title))

    private fun assertBounds(
        lower: SettingsState,
        upper: SettingsState,
        title: Int,
        decreaseAction: Int,
        increaseAction: Int,
        afterLowerIncrease: SettingsIntent,
        afterUpperDecrease: SettingsIntent
    ) {
        var state by mutableStateOf(lower)
        compose.setContent {
            MaterialTheme {
                // The section emits rows into the caller's Column, as in SettingsScreen.
                Column {
                    NotificationsSection(state = state, onIntent = { intents += it }, onShowModeDialog = {})
                }
            }
        }
        button(decreaseAction, title).assertIsNotEnabled().performClick()
        button(increaseAction, title).assertIsEnabled().performClick()
        assertEquals(listOf(afterLowerIncrease), intents)

        intents.clear()
        state = upper
        compose.waitForIdle()
        button(increaseAction, title).assertIsNotEnabled().performClick()
        button(decreaseAction, title).assertIsEnabled().performClick()
        assertEquals(listOf(afterUpperDecrease), intents)
    }

    @Test
    fun candleLightingMorningTimeBounds() {
        val base = SettingsState(notifyCandleLighting = true, candleLightingNotifyMode = "morning")
        assertBounds(
            lower = base.copy(candleLightingMorningTime = 360),
            upper = base.copy(candleLightingMorningTime = 720),
            title = R.string.settings_notification_time,
            decreaseAction = R.string.settings_earlier,
            increaseAction = R.string.settings_later,
            afterLowerIncrease = SettingsIntent.SetCandleLightingMorningTime(390),
            afterUpperDecrease = SettingsIntent.SetCandleLightingMorningTime(690)
        )
    }

    @Test
    fun candleLightingHoursBeforeBounds() {
        val base = SettingsState(notifyCandleLighting = true, candleLightingNotifyMode = "hours_before")
        assertBounds(
            lower = base.copy(candleLightingHoursBefore = 1),
            upper = base.copy(candleLightingHoursBefore = 6),
            title = R.string.settings_hours_before,
            decreaseAction = R.string.settings_decrease,
            increaseAction = R.string.settings_increase,
            afterLowerIncrease = SettingsIntent.SetCandleLightingHoursBefore(2),
            afterUpperDecrease = SettingsIntent.SetCandleLightingHoursBefore(5)
        )
    }

    @Test
    fun holidayDaysBeforeBounds() {
        val base = SettingsState(notifyHolidays = true)
        assertBounds(
            lower = base.copy(holidayNotifyDaysBefore = 0),
            upper = base.copy(holidayNotifyDaysBefore = 14),
            title = R.string.settings_days_before,
            decreaseAction = R.string.settings_decrease,
            increaseAction = R.string.settings_increase,
            afterLowerIncrease = SettingsIntent.SetHolidayNotifyDaysBefore(1),
            afterUpperDecrease = SettingsIntent.SetHolidayNotifyDaysBefore(13)
        )
    }

    @Test
    fun omerMorningTimeBounds() {
        val base = SettingsState(notifyOmer = true, notifyOmerMorning = true)
        assertBounds(
            lower = base.copy(notifyOmerMorningTime = 360),
            upper = base.copy(notifyOmerMorningTime = 720),
            title = R.string.settings_reminder_time,
            decreaseAction = R.string.settings_earlier,
            increaseAction = R.string.settings_later,
            afterLowerIncrease = SettingsIntent.SetNotifyOmerMorningTime(390),
            afterUpperDecrease = SettingsIntent.SetNotifyOmerMorningTime(690)
        )
    }

    @Test
    fun sectionExposesFormattedValueAsState() {
        setSection(SettingsState(notifyHolidays = true, holidayNotifyDaysBefore = 3))
        val valueText = str(R.string.settings_days_before_value, 3)
        button(R.string.settings_increase, R.string.settings_days_before).assert(hasStateDescription(valueText))
        compose.onNodeWithText(str(R.string.settings_days_before))
            .assert(hasTexts(str(R.string.settings_days_before), valueText))
    }

    private fun hasStateDescription(text: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    private fun hasTexts(vararg texts: String) = SemanticsMatcher("Text == ${texts.toList()}") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.map { it.text } == texts.toList()
    }

    private companion object {
        const val TITLE = "Notification time"
    }
}
