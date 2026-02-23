package com.levana.app.ui.events

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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
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
        Text(
            text = if (state.isEditing) "Edit Event" else "Add Event",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title field
        OutlinedTextField(
            value = state.title,
            onValueChange = {
                viewModel.onIntent(AddEditEventIntent.SetTitle(it))
            },
            label = { Text("Title") },
            placeholder = {
                Text("e.g., Dad's Yahrzeit, Anniversary")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hebrew date picker
        Text(
            text = "Hebrew Date",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
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

        Spacer(modifier = Modifier.height(16.dp))

        // Yahrzeit rules toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use yahrzeit Adar rules",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Switch(
                        checked = state.useYahrzeitRules,
                        onCheckedChange = {
                            viewModel.onIntent(
                                AddEditEventIntent.SetUseYahrzeitRules(it)
                            )
                        }
                    )
                }
                if (state.useYahrzeitRules) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adar of a non-leap year maps to Adar II " +
                            "in leap years. Adar I/II of a leap year " +
                            "remain in their respective Adar.",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes field
        OutlinedTextField(
            value = state.notes,
            onValueChange = {
                viewModel.onIntent(AddEditEventIntent.SetNotes(it))
            },
            label = { Text("Notes (optional)") },
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
            Text(if (state.isEditing) "Update" else "Save")
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
                text = "Day",
                style = MaterialTheme.typography.labelMedium
            )
            NumberPicker(
                value = day.coerceIn(1, daysInMonth),
                range = 1..daysInMonth,
                onValueChange = onDayChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Month picker
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = "Month",
                style = MaterialTheme.typography.labelMedium
            )
            MonthPicker(
                selectedMonth = month,
                months = months,
                onMonthChange = onMonthChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Year picker
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = "Year",
                style = MaterialTheme.typography.labelMedium
            )
            NumberPicker(
                value = year,
                range = 5700..5900,
                onValueChange = onYearChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class MonthOption(
    val jewishDateMonth: Int,
    val displayName: String
)

private fun buildMonthList(isLeapYear: Boolean): List<MonthOption> = buildList {
    add(MonthOption(JewishDate.TISHREI, "Tishrei"))
    add(MonthOption(JewishDate.CHESHVAN, "Cheshvan"))
    add(MonthOption(JewishDate.KISLEV, "Kislev"))
    add(MonthOption(JewishDate.TEVES, "Teves"))
    add(MonthOption(JewishDate.SHEVAT, "Shevat"))
    if (isLeapYear) {
        add(MonthOption(JewishDate.ADAR, "Adar I"))
        add(MonthOption(JewishDate.ADAR_II, "Adar II"))
    } else {
        add(MonthOption(JewishDate.ADAR, "Adar"))
    }
    add(MonthOption(JewishDate.NISSAN, "Nissan"))
    add(MonthOption(JewishDate.IYAR, "Iyar"))
    add(MonthOption(JewishDate.SIVAN, "Sivan"))
    add(MonthOption(JewishDate.TAMMUZ, "Tammuz"))
    add(MonthOption(JewishDate.AV, "Av"))
    add(MonthOption(JewishDate.ELUL, "Elul"))
}

@Composable
private fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.IconButton(
            onClick = {
                if (value > range.first) onValueChange(value - 1)
            },
            enabled = value > range.first
        ) {
            Text(
                "\u2212",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.width(4.dp))
        androidx.compose.material3.IconButton(
            onClick = {
                if (value < range.last) onValueChange(value + 1)
            },
            enabled = value < range.last
        ) {
            Text(
                "+",
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
    modifier: Modifier = Modifier
) {
    val currentIndex = months.indexOfFirst {
        it.jewishDateMonth == selectedMonth
    }.coerceAtLeast(0)
    val currentMonth = months[currentIndex]

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.IconButton(
            onClick = {
                if (currentIndex > 0) {
                    onMonthChange(months[currentIndex - 1].jewishDateMonth)
                }
            },
            enabled = currentIndex > 0
        ) {
            Text(
                "\u2212",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = currentMonth.displayName,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material3.IconButton(
            onClick = {
                if (currentIndex < months.size - 1) {
                    onMonthChange(
                        months[currentIndex + 1].jewishDateMonth
                    )
                }
            },
            enabled = currentIndex < months.size - 1
        ) {
            Text(
                "+",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
