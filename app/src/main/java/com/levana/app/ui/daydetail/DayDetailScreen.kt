package com.levana.app.ui.daydetail

import android.content.ContentUris
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.levana.app.domain.model.CalendarEvent
import com.levana.app.domain.model.DayInfo
import com.levana.app.domain.model.Holiday
import com.levana.app.domain.model.SystemCalendarEvent
import com.levana.app.domain.model.ZmanTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_12H = DateTimeFormatter.ofPattern("h:mm a")
private val TIME_24H = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun DayDetailContent(
    state: DayDetailState,
    onShowAllZmanim: (LocalDate) -> Unit = {},
    scrollState: ScrollState = rememberScrollState(),
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
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Personal Events
                    if (state.calendarEvents.isNotEmpty()) {
                        PersonalEventsSection(state.calendarEvents)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (state.systemEvents.isNotEmpty()) {
                        SystemEventsSection(state.systemEvents)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 2. Holidays
                    if (dayInfo.holidays.isNotEmpty()) {
                        HolidaySection(dayInfo.holidays)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 3. Shabbat / Yom Tov times
                    dayInfo.shabbatInfo?.let { info ->
                        if (info.showCandleLighting) {
                            ShabbatTimeCard(
                                "Candle Lighting",
                                "\u05d4\u05d3\u05dc\u05e7\u05ea \u05e0\u05e8\u05d5\u05ea",
                                info.candleLightingTime
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        if (info.showHavdalah) {
                            ShabbatTimeCard(
                                "Havdalah",
                                "\u05d4\u05d1\u05d3\u05dc\u05d4",
                                info.havdalahTime
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // 4. Parasha
                    if (dayInfo.parsha != null) {
                        ParshaSection(
                            dayInfo.parsha,
                            dayInfo.parshaHebrew,
                            dayInfo.specialShabbat
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 5. Molad
                    if (dayInfo.molad != null) {
                        MoladSection(dayInfo.molad)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 6. Omer
                    if (dayInfo.omerFormatted != null) {
                        OmerSection(dayInfo.omerFormatted!!)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 7. Halachic Times
                    HalachicTimesSection(
                        times = state.halachicTimes,
                        onShowAll = { onShowAllZmanim(dayInfo.gregorianDate) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HalachicTimesSection(
    times: List<ZmanTime>,
    onShowAll: () -> Unit
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val formatter = if (is24Hour) TIME_24H else TIME_12H

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            Text(
                text = "Halachic Times",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            if (times.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                times.forEach { zman ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = zman.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = zman.hebrewName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = zman.time?.format(formatter) ?: "--:--",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            }
            TextButton(
                onClick = onShowAll,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("More Halachic Times")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
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
                    color = MaterialTheme.colorScheme
                        .onSecondaryContainer.copy(alpha = 0.8f)
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

@Composable
private fun PersonalEventsSection(events: List<CalendarEvent>) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Personal Events",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            events.forEach { event ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (event) {
                        is CalendarEvent.Birthday -> {
                            val photoBitmap = remember(
                                event.birthday.contactPhotoUri
                            ) {
                                event.birthday.contactPhotoUri?.let {
                                        uriStr ->
                                    try {
                                        context.contentResolver
                                            .openInputStream(
                                                Uri.parse(uriStr)
                                            )?.use {
                                                BitmapFactory
                                                    .decodeStream(it)
                                            }
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                            }
                            if (photoBitmap != null) {
                                Image(
                                    bitmap = photoBitmap.asImageBitmap(),
                                    contentDescription = event.title,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Cake,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme
                                        .primary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography
                                        .bodyLarge
                                )
                                Text(
                                    text = "Birthday",
                                    style = MaterialTheme.typography
                                        .labelSmall,
                                    color = MaterialTheme.colorScheme
                                        .onTertiaryContainer
                                        .copy(alpha = 0.7f)
                                )
                            }
                        }
                        is CalendarEvent.CustomEvent -> {
                            Icon(
                                imageVector = if (
                                    event.event.useYahrzeitRules
                                ) {
                                    Icons.Outlined.WbTwilight
                                } else {
                                    Icons.Filled.Event
                                },
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography
                                        .bodyLarge
                                )
                                if (event.event.useYahrzeitRules) {
                                    Text(
                                        text = "Yahrzeit",
                                        style = MaterialTheme.typography
                                            .labelSmall,
                                        color = MaterialTheme.colorScheme
                                            .onTertiaryContainer
                                            .copy(alpha = 0.7f)
                                    )
                                }
                                if (event.event.notes.isNotBlank()) {
                                    Text(
                                        text = event.event.notes,
                                        style = MaterialTheme.typography
                                            .bodySmall,
                                        color = MaterialTheme.colorScheme
                                            .onTertiaryContainer
                                            .copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SystemEventsSection(events: List<SystemCalendarEvent>) {
    val context = LocalContext.current
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
                text = "Calendar Events",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            events.forEach { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val uri = ContentUris.withAppendedId(
                                CalendarContract.Events.CONTENT_URI,
                                event.eventId
                            )
                            val intent = Intent(Intent.ACTION_VIEW)
                                .setData(uri)
                            context.startActivity(intent)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(event.calendarColor))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (event.allDay) {
                                "All day"
                            } else {
                                "${event.startTime.toLocalTime().format(formatter)} \u2013 " +
                                    event.endTime.toLocalTime().format(
                                        formatter
                                    )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme
                                .onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
