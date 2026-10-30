package ninja.doskey.app.levana.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.YahrzeitFirstYear
import ninja.doskey.app.levana.domain.model.YahrzeitLeapYearAdar
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import ninja.doskey.app.levana.domain.model.IsraelMode
import kotlin.math.roundToInt

/** Single-choice dialog with an explanation above the options. */
@Composable
internal fun <T> OptionDialog(
    title: String,
    description: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                onClick = { onSelect(option) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
internal fun yahrzeitLeapLabel(value: YahrzeitLeapYearAdar): String = stringResource(
    when (value) {
        YahrzeitLeapYearAdar.ADAR_I -> R.string.settings_yahrzeit_leap_adar_i
        YahrzeitLeapYearAdar.ADAR_II -> R.string.settings_yahrzeit_leap_adar_ii
        YahrzeitLeapYearAdar.BOTH -> R.string.settings_yahrzeit_leap_both
    }
)

@Composable
internal fun yahrzeitFirstYearLabel(value: YahrzeitFirstYear): String = stringResource(
    when (value) {
        YahrzeitFirstYear.FOLLOW_ANNUAL -> R.string.settings_yahrzeit_first_year_follow
        YahrzeitFirstYear.ADAR_I -> R.string.settings_yahrzeit_first_year_adar_i
        YahrzeitFirstYear.BOTH -> R.string.settings_yahrzeit_first_year_both
    }
)

@Composable
internal fun yahrzeitMissingDayLabel(value: YahrzeitMissingDay): String = stringResource(
    when (value) {
        YahrzeitMissingDay.FIRST_ANNIVERSARY -> R.string.settings_yahrzeit_missing_day_first_anniversary
        YahrzeitMissingDay.ROSH_CHODESH -> R.string.settings_yahrzeit_missing_day_rosh_chodesh
        YahrzeitMissingDay.BOTH -> R.string.settings_yahrzeit_missing_day_both
    }
)

@Composable
internal fun yahrzeitMissingDayShortLabel(value: YahrzeitMissingDay): String = stringResource(
    when (value) {
        YahrzeitMissingDay.FIRST_ANNIVERSARY -> R.string.settings_yahrzeit_missing_day_first_anniversary_short
        YahrzeitMissingDay.ROSH_CHODESH -> R.string.settings_yahrzeit_missing_day_rosh_chodesh_short
        YahrzeitMissingDay.BOTH -> R.string.settings_yahrzeit_missing_day_both_short
    }
)

@Composable
private fun israelModeLabel(mode: IsraelMode): String = stringResource(
    when (mode) {
        IsraelMode.AUTO -> R.string.settings_israel_mode_auto
        IsraelMode.ISRAEL -> R.string.settings_israel_mode_israel
        IsraelMode.DIASPORA -> R.string.settings_israel_mode_diaspora
    }
)

/** The chosen option; Auto-detect also names what it detected on this device. */
@Composable
internal fun israelModeSummary(mode: IsraelMode, isInIsrael: Boolean): String =
    if (mode == IsraelMode.AUTO) {
        stringResource(
            R.string.settings_israel_mode_auto_detected,
            israelModeLabel(if (isInIsrael) IsraelMode.ISRAEL else IsraelMode.DIASPORA)
        )
    } else {
        israelModeLabel(mode)
    }

@Composable
internal fun IsraelModeDialog(
    selected: IsraelMode,
    onSelect: (IsraelMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_israel_diaspora_label)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                IsraelMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = mode == selected,
                                onClick = { onSelect(mode) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == selected, onClick = null)
                        Text(
                            text = israelModeLabel(mode),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
internal fun AppLanguageDialog(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_app_language_dialog_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    val label = when (language) {
                        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                        AppLanguage.HEBREW -> stringResource(R.string.settings_language_hebrew)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = language == selected,
                                onClick = { onSelect(language) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = language == selected, onClick = null)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
internal fun CandleLightingOffsetDialog(
    offset: Double,
    onOffsetChange: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember(offset) { mutableFloatStateOf(offset.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_candle_offset_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_candle_offset_description, sliderValue.roundToInt()),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 10f..60f,
                    steps = 49
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onOffsetChange(sliderValue.roundToInt().toDouble())
                onDismiss()
            }) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
internal fun CandleLightingModeDialog(
    mode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "morning" to stringResource(R.string.settings_notify_mode_morning),
        "hours_before" to stringResource(R.string.settings_notify_mode_hours_before)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_notification_mode_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = mode == value,
                                onClick = { onSelect(value) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == value, onClick = null)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
