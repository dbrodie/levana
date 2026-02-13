package com.levana.app.ui.daydetail

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levana.app.domain.model.DayInfo
import com.levana.app.domain.model.Holiday
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

private val TIME_12H = DateTimeFormatter.ofPattern("h:mm a")
private val TIME_24H = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun DayDetailScreen(
    dateEpochDay: Long,
    onShowZmanim: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DayDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(dateEpochDay) {
        viewModel.onIntent(
            DayDetailIntent.LoadDay(LocalDate.ofEpochDay(dateEpochDay))
        )
    }

    DayDetailContent(
        state = state,
        onShowZmanim = onShowZmanim,
        modifier = modifier
    )
}

@Composable
fun DayDetailContent(
    state: DayDetailState,
    onShowZmanim: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            state.dayInfo?.let { dayInfo ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DateHeader(dayInfo)

                    if (dayInfo.holidays.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HolidaySection(dayInfo.holidays)
                    }

                    dayInfo.shabbatInfo?.let { info ->
                        if (info.showCandleLighting) {
                            Spacer(modifier = Modifier.height(16.dp))
                            ShabbatTimeCard(
                                "Candle Lighting",
                                "הדלקת נרות",
                                info.candleLightingTime
                            )
                        }
                        if (info.showHavdalah) {
                            Spacer(modifier = Modifier.height(16.dp))
                            ShabbatTimeCard(
                                "Havdalah",
                                "הבדלה",
                                info.havdalahTime
                            )
                        }
                    }

                    if (dayInfo.parsha != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ParshaSection(
                            dayInfo.parsha,
                            dayInfo.parshaHebrew,
                            dayInfo.specialShabbat
                        )
                    }

                    if (dayInfo.molad != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        MoladSection(dayInfo.molad)
                    }

                    if (dayInfo.omerFormatted != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OmerSection(dayInfo.omerFormatted!!)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            onShowZmanim(dayInfo.gregorianDate)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Zmanim")
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(dayInfo: DayInfo) {
    val dayOfWeekName = dayInfo.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val gregorianFormatted = dayInfo.gregorianDate.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    )

    Text(
        text = dayInfo.hebrewDay.hebrewFormatted,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = dayInfo.hebrewDay.transliterated,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = dayOfWeekName,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text(
        text = gregorianFormatted,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HolidaySection(holidays: List<Holiday>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Holidays",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            holidays.forEach { holiday ->
                Text(
                    text = holiday.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = holiday.hebrewName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = holiday.category.name.lowercase()
                                    .replaceFirstChar { it.uppercase() }
                                    .replace("_", " "),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ParshaSection(parsha: String, parshaHebrew: String?, specialShabbat: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Parsha",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = parsha,
                style = MaterialTheme.typography.bodyLarge
            )
            if (parshaHebrew != null) {
                Text(
                    text = parshaHebrew,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme
                        .onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            if (specialShabbat != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = specialShabbat,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun MoladSection(molad: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Shabbat Mevarchim",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = molad,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ShabbatTimeCard(title: String, hebrewTitle: String, time: LocalTime?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val formatter = if (is24Hour) TIME_24H else TIME_12H

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = hebrewTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time?.format(formatter) ?: "--:--",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OmerSection(omerFormatted: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sefirat HaOmer",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = omerFormatted,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
