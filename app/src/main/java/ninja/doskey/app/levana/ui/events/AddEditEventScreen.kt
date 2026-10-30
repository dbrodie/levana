package ninja.doskey.app.levana.ui.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.ui.datepicker.HebrewDateField
import ninja.doskey.app.levana.ui.datepicker.HebrewDatePickerDialog
import ninja.doskey.app.levana.ui.datepicker.LeadingCalendar
import org.koin.androidx.compose.koinViewModel
import ninja.doskey.app.levana.ui.contentWidth

@Composable
fun AddEditEventScreen(
    eventId: Long,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    prefillDay: Int = 0,
    prefillMonth: Int = 0,
    prefillYear: Int = 0,
    viewModel: AddEditEventViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(eventId) {
        if (eventId > 0) {
            viewModel.onIntent(AddEditEventIntent.LoadEvent(eventId))
        }
    }

    LaunchedEffect(prefillDay, prefillMonth, prefillYear) {
        if (prefillDay > 0 && prefillMonth > 0 && prefillYear > 0 &&
            eventId == 0L
        ) {
            viewModel.onIntent(
                AddEditEventIntent.PreFillDate(
                    prefillDay,
                    prefillMonth,
                    prefillYear
                )
            )
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .contentWidth()
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.event_date_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(16.dp))

        // Title field
        OutlinedTextField(
            value = state.title,
            onValueChange = {
                viewModel.onIntent(AddEditEventIntent.SetTitle(it))
            },
            label = { Text(stringResource(R.string.event_title_label)) },
            placeholder = {
                Text(stringResource(R.string.event_title_example))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        HebrewDateField(
            label = stringResource(R.string.event_hebrew_date_label),
            hebrewDate = formatHebrewDate(state.hebrewDay, state.hebrewMonth, state.hebrewYear),
            gregorianDate = state.gregorianDate,
            onClick = { viewModel.onIntent(AddEditEventIntent.OpenDatePicker) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Yahrzeit rules toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.useYahrzeitRules,
                        role = Role.Switch,
                        onValueChange = {
                            viewModel.onIntent(
                                AddEditEventIntent.SetUseYahrzeitRules(it)
                            )
                        }
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Explanation is always shown so toggling never shifts the fields below.
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.event_yahrzeit_rules),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.event_yahrzeit_rules_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = state.useYahrzeitRules,
                    onCheckedChange = null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes field
        OutlinedTextField(
            value = state.notes,
            onValueChange = {
                viewModel.onIntent(AddEditEventIntent.SetNotes(it))
            },
            label = { Text(stringResource(R.string.event_notes_optional)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.onIntent(AddEditEventIntent.Save) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving && state.title.isNotBlank()
        ) {
            Text(stringResource(if (state.isEditing) R.string.event_update else R.string.event_save))
        }
    }

    if (state.showDatePicker) {
        HebrewDatePickerDialog(
            initialDate = state.gregorianDate,
            leading = LeadingCalendar.HEBREW,
            onConfirm = { viewModel.onIntent(AddEditEventIntent.SetHebrewDate(it)) },
            onDismiss = { viewModel.onIntent(AddEditEventIntent.DismissDatePicker) }
        )
    }
}
