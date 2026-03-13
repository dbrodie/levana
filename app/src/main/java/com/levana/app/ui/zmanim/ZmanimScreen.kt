package com.levana.app.ui.zmanim

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levana.app.domain.model.ZmanCategory
import com.levana.app.domain.model.ZmanTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.koin.androidx.compose.koinViewModel

@Composable
fun ZmanimScreen(
    initialDate: LocalDate? = null,
    modifier: Modifier = Modifier,
    viewModel: ZmanimViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(initialDate) {
        viewModel.onIntent(ZmanimIntent.LoadDate(initialDate ?: LocalDate.now()))
    }

    ZmanimContent(state = state, modifier = modifier)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZmanimContent(
    state: ZmanimState,
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

    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val formatter = if (is24Hour) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofPattern("h:mm a")
    }

    val grouped = state.zmanim.groupBy { it.category }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        ZmanCategory.entries.forEach { category ->
            val zmanim = grouped[category]
            if (zmanim.isNullOrEmpty()) return@forEach

            val label = when (category) {
                ZmanCategory.MORNING -> "Morning"
                ZmanCategory.AFTERNOON -> "Afternoon"
                ZmanCategory.EVENING -> "Evening"
                ZmanCategory.NIGHT -> "Night"
            }

            stickyHeader(key = category.name) {
                // MaterialTheme is accessible here — stickyHeader is @Composable
                val containerColor = when (category) {
                    ZmanCategory.MORNING -> MaterialTheme.colorScheme.primaryContainer
                    ZmanCategory.AFTERNOON -> MaterialTheme.colorScheme.secondaryContainer
                    ZmanCategory.EVENING -> MaterialTheme.colorScheme.tertiaryContainer
                    ZmanCategory.NIGHT -> MaterialTheme.colorScheme.surfaceVariant
                }
                val labelColor = when (category) {
                    ZmanCategory.MORNING -> MaterialTheme.colorScheme.onPrimaryContainer
                    ZmanCategory.AFTERNOON -> MaterialTheme.colorScheme.onSecondaryContainer
                    ZmanCategory.EVENING -> MaterialTheme.colorScheme.onTertiaryContainer
                    ZmanCategory.NIGHT -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(containerColor)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = labelColor
                    )
                }
            }

            items(zmanim, key = { z -> "${category.name}:${z.name}" }) { zman ->
                ListItem(
                    headlineContent = { Text(zman.name) },
                    supportingContent = {
                        Text(
                            text = zman.hebrewName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Text(
                            text = formatTime(zman.time, formatter),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

private fun formatTime(time: LocalTime?, formatter: DateTimeFormatter): String =
    time?.format(formatter) ?: "—"
