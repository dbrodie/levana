package com.levana.app.ui.settings

import android.Manifest
import android.app.AlarmManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levana.app.domain.model.Minhag
import com.levana.app.ui.theme.HolidayTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onChangeLocation: () -> Unit,
    onPersonalEvents: () -> Unit,
    onSystemCalendars: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsContent(
        state = state,
        onIntent = viewModel::onIntent,
        onChangeLocation = onChangeLocation,
        onPersonalEvents = onPersonalEvents,
        onSystemCalendars = onSystemCalendars,
        modifier = modifier
    )
}

@Composable
fun SettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onChangeLocation: () -> Unit,
    onPersonalEvents: () -> Unit,
    onSystemCalendars: () -> Unit,
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

        PersonalEventsSection(onPersonalEvents = onPersonalEvents)

        SystemCalendarsSection(onSystemCalendars = onSystemCalendars)

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

        ToggleSection(
            title = "Holiday Theming",
            description = "Change app colors during holiday periods",
            label = "Enable holiday theming",
            checked = state.dynamicHolidayTheme,
            onCheckedChange = {
                onIntent(SettingsIntent.SetDynamicHolidayTheme(it))
            }
        )

        CandleLightingSection(
            offset = state.candleLightingOffset,
            onOffsetChange = {
                onIntent(SettingsIntent.SetCandleLightingOffset(it))
            }
        )

        NotificationsSection(state = state, onIntent = onIntent)

        if (state.showDeveloperSettings) {
            DeveloperSettingsSection(
                devDateOverride = state.devDateOverride,
                devForceHolidayTheme = state.devForceHolidayTheme,
                onDateOverrideChange = {
                    onIntent(SettingsIntent.SetDevDateOverride(it))
                },
                onForceHolidayThemeChange = {
                    onIntent(SettingsIntent.SetDevForceHolidayTheme(it))
                }
            )
        }
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

@Composable
private fun PersonalEventsSection(onPersonalEvents: () -> Unit) {
    SectionCard(
        title = "Personal Events",
        description = "Birthdays, yahrzeits, and custom events"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPersonalEvents),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Manage Events",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = "Open",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SystemCalendarsSection(onSystemCalendars: () -> Unit) {
    SectionCard(
        title = "System Calendars",
        description = "Show events from your device calendars"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSystemCalendars),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Select Calendars",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = "Open",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeveloperSettingsSection(
    devDateOverride: LocalDate?,
    devForceHolidayTheme: String?,
    onDateOverrideChange: (LocalDate?) -> Unit,
    onForceHolidayThemeChange: (String?) -> Unit
) {
    SectionCard(
        title = "Developer Settings",
        description = "For testing and development"
    ) {
        // Date Override
        Text(
            text = "Date Override",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))

        DateOverridePicker(
            currentDate = devDateOverride,
            onDateChange = onDateOverrideChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Force Holiday Theme
        Text(
            text = "Force Holiday Theme",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))

        HolidayThemePicker(
            selected = devForceHolidayTheme,
            onSelect = onForceHolidayThemeChange
        )
    }
}

@Composable
private fun DateOverridePicker(currentDate: LocalDate?, onDateChange: (LocalDate?) -> Unit) {
    var yearText by remember(currentDate) {
        mutableStateOf(currentDate?.year?.toString() ?: "")
    }
    var monthText by remember(currentDate) {
        mutableStateOf(currentDate?.monthValue?.toString() ?: "")
    }
    var dayText by remember(currentDate) {
        mutableStateOf(currentDate?.dayOfMonth?.toString() ?: "")
    }

    if (currentDate != null) {
        Text(
            text = "Currently: ${currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = yearText,
            onValueChange = { yearText = it },
            label = { Text("Year") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = monthText,
            onValueChange = { monthText = it },
            label = { Text("Month") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = dayText,
            onValueChange = { dayText = it },
            label = { Text("Day") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = {
            yearText = ""
            monthText = ""
            dayText = ""
            onDateChange(null)
        }) {
            Text("Clear")
        }
        TextButton(onClick = {
            try {
                val y = yearText.toInt()
                val m = monthText.toInt()
                val d = dayText.toInt()
                onDateChange(LocalDate.of(y, m, d))
            } catch (_: Exception) {
                // Invalid input, ignore
            }
        }) {
            Text("Apply")
        }
    }
}

@Composable
private fun NotificationsSection(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    fun ensurePermissionThenEnable(onGranted: () -> Unit) {
        if (hasNotificationPermission) {
            onGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun needsExactAlarm(): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return !alarmManager.canScheduleExactAlarms()
    }

    SectionCard(
        title = "Notifications",
        description = "Configure reminders for events and times"
    ) {
        // Candle Lighting
        NotificationToggleRow(
            label = "Candle Lighting",
            checked = state.notifyCandleLighting,
            onCheckedChange = { enabled ->
                if (enabled) {
                    ensurePermissionThenEnable {
                        onIntent(SettingsIntent.SetNotifyCandleLighting(true))
                    }
                } else {
                    onIntent(SettingsIntent.SetNotifyCandleLighting(false))
                }
            }
        )

        if (state.notifyCandleLighting) {
            Spacer(modifier = Modifier.height(4.dp))

            // Mode selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .selectableGroup()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.candleLightingNotifyMode == "morning",
                            onClick = {
                                onIntent(
                                    SettingsIntent.SetCandleLightingNotifyMode("morning")
                                )
                            },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.candleLightingNotifyMode == "morning",
                        onClick = null
                    )
                    Text(
                        text = "Morning of erev",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (state.candleLightingNotifyMode == "morning") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hours = state.candleLightingMorningTime / 60
                        val minutes = state.candleLightingMorningTime % 60
                        Text(
                            text = "At %d:%02d AM".format(
                                if (hours == 0) 12 else if (hours > 12) hours - 12 else hours,
                                minutes
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                if (state.candleLightingMorningTime > 360) {
                                    onIntent(
                                        SettingsIntent.SetCandleLightingMorningTime(
                                            state.candleLightingMorningTime - 30
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "Earlier"
                            )
                        }
                        IconButton(
                            onClick = {
                                if (state.candleLightingMorningTime < 720) {
                                    onIntent(
                                        SettingsIntent.SetCandleLightingMorningTime(
                                            state.candleLightingMorningTime + 30
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Later"
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.candleLightingNotifyMode == "hours_before",
                            onClick = {
                                onIntent(
                                    SettingsIntent.SetCandleLightingNotifyMode("hours_before")
                                )
                            },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.candleLightingNotifyMode == "hours_before",
                        onClick = null
                    )
                    Text(
                        text = "Hours before",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (state.candleLightingNotifyMode == "hours_before") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.candleLightingHoursBefore} hours before",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                if (state.candleLightingHoursBefore > 1) {
                                    onIntent(
                                        SettingsIntent.SetCandleLightingHoursBefore(
                                            state.candleLightingHoursBefore - 1
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "Decrease"
                            )
                        }
                        IconButton(
                            onClick = {
                                if (state.candleLightingHoursBefore < 6) {
                                    onIntent(
                                        SettingsIntent.SetCandleLightingHoursBefore(
                                            state.candleLightingHoursBefore + 1
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Increase"
                            )
                        }
                    }

                    if (needsExactAlarm()) {
                        Text(
                            text = "Exact alarm permission required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Holidays
        NotificationToggleRow(
            label = "Holidays",
            checked = state.notifyHolidays,
            onCheckedChange = { enabled ->
                if (enabled) {
                    ensurePermissionThenEnable {
                        onIntent(SettingsIntent.SetNotifyHolidays(true))
                    }
                } else {
                    onIntent(SettingsIntent.SetNotifyHolidays(false))
                }
            }
        )

        if (state.notifyHolidays) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.holidayNotifyDaysBefore} days before",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        if (state.holidayNotifyDaysBefore > 0) {
                            onIntent(
                                SettingsIntent.SetHolidayNotifyDaysBefore(
                                    state.holidayNotifyDaysBefore - 1
                                )
                            )
                        }
                    }
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                }
                IconButton(
                    onClick = {
                        if (state.holidayNotifyDaysBefore < 14) {
                            onIntent(
                                SettingsIntent.SetHolidayNotifyDaysBefore(
                                    state.holidayNotifyDaysBefore + 1
                                )
                            )
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Fasts
        NotificationToggleRow(
            label = "Fasts",
            checked = state.notifyFasts,
            onCheckedChange = { enabled ->
                if (enabled) {
                    ensurePermissionThenEnable {
                        onIntent(SettingsIntent.SetNotifyFasts(true))
                    }
                } else {
                    onIntent(SettingsIntent.SetNotifyFasts(false))
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Personal Events
        NotificationToggleRow(
            label = "Personal Events",
            checked = state.notifyPersonalEvents,
            onCheckedChange = { enabled ->
                if (enabled) {
                    ensurePermissionThenEnable {
                        onIntent(SettingsIntent.SetNotifyPersonalEvents(true))
                    }
                } else {
                    onIntent(SettingsIntent.SetNotifyPersonalEvents(false))
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Omer
        NotificationToggleRow(
            label = "Omer (at sunset)",
            checked = state.notifyOmer,
            onCheckedChange = { enabled ->
                if (enabled) {
                    ensurePermissionThenEnable {
                        onIntent(SettingsIntent.SetNotifyOmer(true))
                    }
                } else {
                    onIntent(SettingsIntent.SetNotifyOmer(false))
                }
            }
        )

        if (state.notifyOmer && needsExactAlarm()) {
            Text(
                text = "Exact alarm permission required for sunset notifications",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun NotificationToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HolidayThemePicker(selected: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("None") + HolidayTheme.entries.map { it.name }
    val displayValue = selected ?: "None"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(if (option == "None") null else option)
                        expanded = false
                    }
                )
            }
        }
    }
}
