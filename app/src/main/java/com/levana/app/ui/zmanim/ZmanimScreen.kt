package com.levana.app.ui.zmanim

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levana.app.domain.model.ZmanCategory
import com.levana.app.domain.model.ZmanTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.koin.androidx.compose.koinViewModel

@Composable
fun ZmanimScreen(
    initialDate: LocalDate? = null,
    modifier: Modifier = Modifier,
    viewModel: ZmanimViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(initialDate) {
        if (initialDate != null) {
            viewModel.onIntent(ZmanimIntent.LoadDate(initialDate))
        }
    }

    ZmanimContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
private fun ZmanimContent(
    state: ZmanimState,
    onIntent: (ZmanimIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        DateHeader(
            date = state.date,
            onPreviousDay = {
                onIntent(ZmanimIntent.LoadDate(state.date.minusDays(1)))
            },
            onNextDay = {
                onIntent(ZmanimIntent.LoadDate(state.date.plusDays(1)))
            },
            onDateSelected = { onIntent(ZmanimIntent.LoadDate(it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        val grouped = state.zmanim.groupBy { it.category }
        ZmanCategory.entries.forEach { category ->
            val zmanim = grouped[category] ?: return@forEach
            CategoryCard(category, zmanim)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateHeader(
    date: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous day"
            )
        }

        Text(
            text = date.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { showDatePicker = true }
        )

        IconButton(onClick = onNextDay) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next day"
            )
        }
    }

    if (showDatePicker) {
        val epochMs = date.atStartOfDay()
            .toInstant(ZoneOffset.UTC).toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = epochMs
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val selected = Instant.ofEpochMilli(ms)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(selected)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun CategoryCard(category: ZmanCategory, zmanim: List<ZmanTime>) {
    val title = when (category) {
        ZmanCategory.MORNING -> "Morning"
        ZmanCategory.AFTERNOON -> "Afternoon"
        ZmanCategory.EVENING -> "Evening"
        ZmanCategory.NIGHT -> "Night"
    }

    val containerColor = when (category) {
        ZmanCategory.MORNING -> MaterialTheme.colorScheme.primaryContainer
        ZmanCategory.AFTERNOON -> MaterialTheme.colorScheme.secondaryContainer
        ZmanCategory.EVENING -> MaterialTheme.colorScheme.tertiaryContainer
        ZmanCategory.NIGHT -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            zmanim.forEachIndexed { index, zman ->
                ZmanRow(zman)
                if (index < zmanim.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface
                            .copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZmanRow(zman: ZmanTime) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val formatter = if (is24Hour) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofPattern("h:mm a")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = zman.name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = zman.hebrewName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
                    .copy(alpha = 0.7f)
            )
        }
        Text(
            text = formatTime(zman.time, formatter),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTime(time: LocalTime?, formatter: DateTimeFormatter): String =
    time?.format(formatter) ?: "--:--"
