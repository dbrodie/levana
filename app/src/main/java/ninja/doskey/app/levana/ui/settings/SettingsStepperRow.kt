package ninja.doskey.app.levana.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.R

/**
 * A settings row showing a numeric value with decrease/increase buttons. Each button is
 * disabled at its bound, is labeled with its action and the setting name, and exposes the
 * current formatted value as its state, so TalkBack reads the new value after activation.
 * The title and value text form one separate node; the buttons keep their own semantics.
 * Reaching a bound with the keyboard moves focus to the other button instead of losing it.
 */
@Composable
internal fun SettingsStepperRow(
    title: String,
    valueText: String,
    value: Int,
    range: IntRange,
    step: Int,
    decreaseAction: String,
    increaseAction: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val inputModeManager = LocalInputModeManager.current
    val decreaseFocus = remember { FocusRequester() }
    val increaseFocus = remember { FocusRequester() }

    // A button disabled at its bound drops keyboard focus, which would jump out of the row,
    // so move it to the opposite button first. Touch and TalkBack focus are left alone.
    fun applyStep(newValue: Int, atBound: Boolean, other: FocusRequester) {
        if (atBound && inputModeManager.inputMode == InputMode.Keyboard) other.requestFocus()
        onValueChange(newValue)
    }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(valueText) },
        leadingContent = { Spacer(modifier = Modifier.padding(start = 16.dp)) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperButton(
                    icon = Icons.Filled.Remove,
                    label = stringResource(R.string.settings_stepper_action, decreaseAction, title),
                    valueText = valueText,
                    enabled = value > range.first,
                    focusRequester = decreaseFocus,
                    onClick = {
                        val newValue = (value - step).coerceIn(range)
                        applyStep(newValue, atBound = newValue == range.first, other = increaseFocus)
                    }
                )
                StepperButton(
                    icon = Icons.Filled.Add,
                    label = stringResource(R.string.settings_stepper_action, increaseAction, title),
                    valueText = valueText,
                    enabled = value < range.last,
                    focusRequester = increaseFocus,
                    onClick = {
                        val newValue = (value + step).coerceIn(range)
                        applyStep(newValue, atBound = newValue == range.last, other = decreaseFocus)
                    }
                )
            }
        },
        modifier = modifier.semantics(mergeDescendants = true) {}
    )
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    label: String,
    valueText: String,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .focusRequester(focusRequester)
            .semantics { stateDescription = valueText }
    ) {
        Icon(icon, contentDescription = label)
    }
}
