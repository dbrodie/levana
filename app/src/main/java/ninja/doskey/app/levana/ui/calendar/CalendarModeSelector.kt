package ninja.doskey.app.levana.ui.calendar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import ninja.doskey.app.levana.R

/**
 * Single-choice Hebrew/Gregorian calendar mode selector.
 *
 * Each option reports its own mode through [onSelectMode]; activating the selected
 * option reports the current mode again rather than toggling it.
 */
@Composable
internal fun CalendarModeSelector(
    hebrewSelected: Boolean,
    onSelectMode: (hebrew: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = listOf(R.string.calendar_mode_hebrew to true, R.string.calendar_mode_gregorian to false)
    SingleChoiceSegmentedButtonRow(modifier) {
        labels.forEachIndexed { index, (label, hebrew) ->
            SegmentedButton(
                selected = hebrewSelected == hebrew,
                onClick = { onSelectMode(hebrew) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
            ) {
                Text(stringResource(label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}
