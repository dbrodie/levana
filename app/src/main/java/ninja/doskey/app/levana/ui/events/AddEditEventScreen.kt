package ninja.doskey.app.levana.ui.events

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.R
import org.koin.androidx.compose.koinViewModel

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

        // Hebrew date picker
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.event_hebrew_date_label), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(12.dp))
        HebrewDatePicker(
            day = state.hebrewDay,
            month = state.hebrewMonth,
            year = state.hebrewYear,
            onDayChange = {
                viewModel.onIntent(AddEditEventIntent.SetHebrewDay(it))
            },
            onMonthChange = {
                viewModel.onIntent(AddEditEventIntent.SetHebrewMonth(it))
            },
            onYearChange = {
                viewModel.onIntent(AddEditEventIntent.SetHebrewYear(it))
            }
        )

        // Show formatted date preview
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatHebrewDate(
                state.hebrewDay,
                state.hebrewMonth,
                state.hebrewYear
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
            }
        }

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
}

@Composable
internal fun HebrewDatePicker(
    day: Int,
    month: Int,
    year: Int,
    onDayChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit
) {
    val isLeapYear = try {
        JewishDate(year, JewishDate.TISHREI, 1).isJewishLeapYear
    } catch (_: Exception) {
        false
    }

    val months = buildMonthList(isLeapYear)
    val daysInMonth = try {
        JewishDate(year, month, 1).daysInJewishMonth
    } catch (_: Exception) {
        30
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Day picker
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.event_date_picker_day),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            NumberPicker(
                value = day.coerceIn(1, daysInMonth),
                range = 1..daysInMonth,
                onValueChange = onDayChange,
                controlName = stringResource(R.string.event_date_picker_day),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Month picker
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = stringResource(R.string.event_date_picker_month),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            MonthPicker(
                selectedMonth = month,
                months = months,
                onMonthChange = onMonthChange,
                controlName = stringResource(R.string.event_date_picker_month),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Year picker
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = stringResource(R.string.event_date_picker_year),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            NumberPicker(
                value = year,
                range = HebrewDateMatcher.EDITABLE_YEARS,
                onValueChange = onYearChange,
                controlName = stringResource(R.string.event_date_picker_year),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class MonthOption(
    val jewishDateMonth: Int,
    val displayName: String,
    val hebrewDisplayName: String
)

private fun buildMonthList(isLeapYear: Boolean): List<MonthOption> = buildList {
    // English names come from the shared HebrewMonth source
    fun add(month: Int, hebrewName: String) =
        add(MonthOption(month, HebrewMonth.englishName(month, isLeapYear), hebrewName))
    add(JewishDate.TISHREI, "תשרי")
    add(JewishDate.CHESHVAN, "חשוון")
    add(JewishDate.KISLEV, "כסלו")
    add(JewishDate.TEVES, "טבת")
    add(JewishDate.SHEVAT, "שבט")
    if (isLeapYear) {
        add(JewishDate.ADAR, "אדר א׳")
        add(JewishDate.ADAR_II, "אדר ב׳")
    } else {
        add(JewishDate.ADAR, "אדר")
    }
    add(JewishDate.NISSAN, "ניסן")
    add(JewishDate.IYAR, "אייר")
    add(JewishDate.SIVAN, "סיוון")
    add(JewishDate.TAMMUZ, "תמוז")
    add(JewishDate.AV, "אב")
    add(JewishDate.ELUL, "אלול")
}

@Composable
private fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    controlName: String,
    modifier: Modifier = Modifier
) {
    val increaseDescription = stringResource(R.string.event_increase_value, controlName)
    val decreaseDescription = stringResource(R.string.event_decrease_value, controlName)
    Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.IconButton(
            onClick = {
                if (value < range.last) onValueChange(value + 1)
            },
            enabled = value < range.last,
            modifier = Modifier.semantics { contentDescription = increaseDescription }
        ) {
            Text(
                "+",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        androidx.compose.material3.IconButton(
            onClick = {
                if (value > range.first) onValueChange(value - 1)
            },
            enabled = value > range.first,
            modifier = Modifier.semantics { contentDescription = decreaseDescription }
        ) {
            Text(
                "\u2212",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun MonthPicker(
    selectedMonth: Int,
    months: List<MonthOption>,
    onMonthChange: (Int) -> Unit,
    controlName: String,
    modifier: Modifier = Modifier
) {
    val currentIndex = months.indexOfFirst {
        it.jewishDateMonth == selectedMonth
    }.coerceAtLeast(0)
    val currentMonth = months[currentIndex]
    val increaseDescription = stringResource(R.string.event_increase_value, controlName)
    val decreaseDescription = stringResource(R.string.event_decrease_value, controlName)

    Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.IconButton(
            onClick = {
                if (currentIndex < months.size - 1) {
                    onMonthChange(months[currentIndex + 1].jewishDateMonth)
                }
            },
            enabled = currentIndex < months.size - 1,
            modifier = Modifier.semantics { contentDescription = increaseDescription }
        ) {
            Text(
                "+",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = if (Locale.current.language in setOf("he", "iw")) currentMonth.hebrewDisplayName else currentMonth.displayName,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.material3.IconButton(
            onClick = {
                if (currentIndex > 0) {
                    onMonthChange(months[currentIndex - 1].jewishDateMonth)
                }
            },
            enabled = currentIndex > 0,
            modifier = Modifier.semantics { contentDescription = decreaseDescription }
        ) {
            Text(
                "\u2212",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
