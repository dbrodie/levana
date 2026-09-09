package ninja.doskey.app.levana.ui.daydetail

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.domain.model.CalendarEvent
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.DayInfo
import ninja.doskey.app.levana.domain.model.SystemCalendarEvent
import ninja.doskey.app.levana.domain.model.ZmanTime
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
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DaySheetBadges(dayInfo)
                    QuickZmanimCards(state.allZmanim, dayInfo.shabbatInfo?.takeIf { it.showCandleLighting }?.candleLightingTime)
                    Spacer(modifier = Modifier.height(24.dp))
                    HalachicTimesSection(
                        times = state.halachicTimes,
                        onShowAll = { onShowAllZmanim(dayInfo.gregorianDate) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Torah reading
                    if (dayInfo.parsha != null) {
                        ParshaSection(
                            dayInfo.parsha,
                            dayInfo.parshaHebrew,
                            dayInfo.specialShabbat
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Molad
                    if (dayInfo.molad != null) {
                        MoladSection(dayInfo.molad)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Omer
                    if (dayInfo.omerFormatted != null) {
                        OmerSection(dayInfo.omerFormatted!!)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Shabbat / Yom Tov times
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

                    // Personal and device events follow the day’s Jewish-calendar details.
                    if (state.calendarEvents.isNotEmpty()) {
                        PersonalEventsSection(state.calendarEvents)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (state.systemEvents.isNotEmpty()) {
                        SystemEventsSection(state.systemEvents)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DaySheetBadges(dayInfo: DayInfo) {
    val locale = LocalConfiguration.current.locales[0]
    val isHebrew = locale.language in setOf("he", "iw")
    val labels = dayInfo.holidays.map { if (isHebrew) it.hebrewName else it.name } +
        if (dayInfo.shabbatInfo?.isShabbat == true) listOf(stringResource(R.string.sheet_shabbat)) else emptyList()
    if (labels.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            labels.forEach { label ->
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickZmanimCards(times: List<ZmanTime>, candleLighting: LocalTime?) {
    val context = LocalContext.current
    val formatter = if (DateFormat.is24HourFormat(context)) TIME_24H else TIME_12H
    val sunrise = times.firstOrNull {
        it.name.contains("sunrise", ignoreCase = true) || it.name.contains("netz", ignoreCase = true)
    }?.time
    val sunset = times.firstOrNull {
        it.name.contains("sunset", ignoreCase = true) || it.name.contains("shkia", ignoreCase = true)
    }?.time
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickZmanCard(stringResource(R.string.sheet_netz), sunrise?.format(formatter) ?: "--:--", false, Modifier.weight(1f))
        QuickZmanCard(stringResource(R.string.sheet_shkia), sunset?.format(formatter) ?: "--:--", false, Modifier.weight(1f))
        QuickZmanCard(
            stringResource(if (candleLighting != null) R.string.sheet_candles else R.string.sheet_tzeit),
            candleLighting?.format(formatter) ?: times.firstOrNull {
                it.name.contains("tzeit", true) || it.name.contains("nightfall", true)
            }?.time?.format(formatter) ?: "--:--",
            true,
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickZmanCard(label: String, value: String, featured: Boolean, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (featured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (featured) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum", textDirection = TextDirection.Ltr),
                fontWeight = FontWeight.SemiBold,
                color = if (featured) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.sheet_zmanim),
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
                                text = if (isHebrewUi()) zman.hebrewName else zman.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (isHebrewUi()) zman.name else zman.hebrewName,
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
                Text(stringResource(R.string.sheet_more_times))
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

@Composable
private fun ParshaSection(parsha: String, parshaHebrew: String?, specialShabbat: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.parsha),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isHebrewUi()) parshaHebrew ?: parsha else parsha,
                style = MaterialTheme.typography.bodyLarge
            )
            if (parshaHebrew != null) {
                Text(
                    text = if (isHebrewUi()) parsha else parshaHebrew,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.sheet_shabbat_mevarchim),
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isHebrewUi()) hebrewTitle else title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isHebrewUi()) title else hebrewTitle,
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.sefirat_haomer),
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.design_my_events),
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
                                    text = stringResource(R.string.design_birthday),
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
                                        text = stringResource(R.string.sheet_yahrzeit),
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.sheet_calendar_events),
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
                                stringResource(R.string.sheet_all_day)
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

@Composable
private fun isHebrewUi(): Boolean = LocalConfiguration.current.locales[0].language in setOf("he", "iw")
