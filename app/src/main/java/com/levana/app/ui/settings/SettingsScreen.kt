package com.levana.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levana.app.domain.model.Minhag
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onChangeLocation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsContent(
        state = state,
        onIntent = viewModel::onIntent,
        onChangeLocation = onChangeLocation,
        modifier = modifier
    )
}

@Composable
fun SettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onChangeLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LocationSection(
            locationName = state.locationName,
            onChangeLocation = onChangeLocation
        )

        MinhagSection(
            selected = state.minhag,
            onSelect = { onIntent(SettingsIntent.SetMinhag(it)) }
        )

        ToggleSection(
            title = "Israel / Diaspora",
            description = "Affects second day Yom Tov and parsha readings",
            label = "In Israel",
            checked = state.isInIsrael,
            onCheckedChange = { onIntent(SettingsIntent.SetIsInIsrael(it)) }
        )

        ToggleSection(
            title = "Modern Israeli Holidays",
            description = "Yom HaShoah, Yom HaZikaron, Yom HaAtzmaut, Yom Yerushalayim",
            label = "Show on calendar",
            checked = state.showModernIsraeliHolidays,
            onCheckedChange = {
                onIntent(SettingsIntent.SetShowModernIsraeli(it))
            }
        )

        ToggleSection(
            title = "Hebrew-Primary Mode",
            description = "Calendar organized by Hebrew months",
            label = "Enable Hebrew-primary",
            checked = state.hebrewPrimary,
            onCheckedChange = {
                onIntent(SettingsIntent.SetHebrewPrimary(it))
            }
        )

        CandleLightingSection(
            offset = state.candleLightingOffset,
            onOffsetChange = {
                onIntent(SettingsIntent.SetCandleLightingOffset(it))
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
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
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun LocationSection(locationName: String, onChangeLocation: () -> Unit) {
    SectionCard(title = "Location") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onChangeLocation),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = locationName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = "Change",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MinhagSection(selected: Minhag, onSelect: (Minhag) -> Unit) {
    SectionCard(
        title = "Minhag",
        description = "Affects prayer customs and some holiday observances"
    ) {
        Column(modifier = Modifier.selectableGroup()) {
            Minhag.entries.forEach { minhag ->
                val label = minhag.name.lowercase()
                    .replaceFirstChar { it.uppercase() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = minhag == selected,
                            onClick = { onSelect(minhag) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = minhag == selected,
                        onClick = null
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleSection(
    title: String,
    description: String,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SectionCard(title = title, description = description) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun CandleLightingSection(offset: Double, onOffsetChange: (Double) -> Unit) {
    SectionCard(
        title = "Candle Lighting",
        description = "Minutes before sunset"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (offset > 10) onOffsetChange(offset - 1)
                }
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
            }
            Text(
                text = "${offset.toInt()} min",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            IconButton(
                onClick = {
                    if (offset < 60) onOffsetChange(offset + 1)
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase")
            }
        }
    }
}
