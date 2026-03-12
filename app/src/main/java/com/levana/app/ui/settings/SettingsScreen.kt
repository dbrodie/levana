package com.levana.app.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.NoFood
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onChangeLocation: () -> Unit,
    onSystemCalendars: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsContent(
        state = state,
        onIntent = viewModel::onIntent,
        onChangeLocation = onChangeLocation,
        onSystemCalendars = onSystemCalendars,
        modifier = modifier
    )
}

@Composable
fun SettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onChangeLocation: () -> Unit,
    onSystemCalendars: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMinhagDialog by remember { mutableStateOf(false) }
    var showAppLanguageDialog by remember { mutableStateOf(false) }
    var showCandleLightingOffsetDialog by remember { mutableStateOf(false) }
    var showCandleLightingModeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // --- Location & System Calendars (no header) ---
        ListItem(
            headlineContent = { Text("Location") },
            supportingContent = {
                if (state.locationName.isNotEmpty()) Text(state.locationName)
            },
            leadingContent = {
                Icon(Icons.Outlined.LocationOn, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onChangeLocation)
        )
        ListItem(
            headlineContent = { Text("System Calendars") },
            leadingContent = {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onSystemCalendars)
        )

        HorizontalDivider()
        SettingsSectionHeader("Calendar Preferences")

        ListItem(
            headlineContent = { Text("Israel / Diaspora") },
            supportingContent = { Text("Affects second day Yom Tov and parsha readings") },
            leadingContent = {
                Icon(Icons.Outlined.Public, contentDescription = null)
            },
            trailingContent = {
                Switch(
                    checked = state.isInIsrael,
                    onCheckedChange = { onIntent(SettingsIntent.SetIsInIsrael(it)) }
                )
            }
        )
        ListItem(
            headlineContent = { Text("Modern Israeli Holidays") },
            supportingContent = { Text("Yom HaShoah, Yom HaZikaron, Yom HaAtzmaut, Yom Yerushalayim") },
            leadingContent = {
                Icon(Icons.Outlined.Flag, contentDescription = null)
            },
            trailingContent = {
                Switch(
                    checked = state.showModernIsraeliHolidays,
                    onCheckedChange = { onIntent(SettingsIntent.SetShowModernIsraeli(it)) }
                )
            }
        )
        ListItem(
            headlineContent = { Text("Candle Lighting Offset") },
            supportingContent = { Text("${state.candleLightingOffset.toInt()} min before sunset") },
            leadingContent = {
                Icon(Icons.Outlined.WbTwilight, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable { showCandleLightingOffsetDialog = true }
        )

        HorizontalDivider()
        SettingsSectionHeader("Minhag")

        val minhagLabel = state.minhag.name.lowercase().replaceFirstChar { it.uppercase() }
        ListItem(
            headlineContent = { Text("Minhag") },
            supportingContent = { Text(minhagLabel) },
            leadingContent = {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable { showMinhagDialog = true }
        )

        HorizontalDivider()
        SettingsSectionHeader("Appearance")

        val appLanguageLabel = when (state.appLanguage) {
            AppLanguage.SYSTEM -> "System default"
            AppLanguage.HEBREW -> "Hebrew (עברית)"
        }
        ListItem(
            headlineContent = { Text("App Language") },
            supportingContent = { Text(appLanguageLabel) },
            leadingContent = {
                Icon(Icons.Outlined.Language, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable { showAppLanguageDialog = true }
        )
        ListItem(
            headlineContent = { Text("Holiday Theming") },
            supportingContent = { Text("Change app colors during holiday periods") },
            leadingContent = {
                Icon(Icons.Outlined.Palette, contentDescription = null)
            },
            trailingContent = {
                Switch(
                    checked = state.dynamicHolidayTheme,
                    onCheckedChange = { onIntent(SettingsIntent.SetDynamicHolidayTheme(it)) }
                )
            }
        )

        HorizontalDivider()
        SettingsSectionHeader("Halachic Times")

        Text(
            text = "Shown in the day panel (up to 5)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
        )

        HalachicTimesSection(
            selectedZmanim = state.selectedZmanim,
            onToggle = { name, enabled -> onIntent(SettingsIntent.ToggleZman(name, enabled)) }
        )

        HorizontalDivider()
        SettingsSectionHeader("Notifications")

        NotificationsSection(
            state = state,
            onIntent = onIntent,
            onShowModeDialog = { showCandleLightingModeDialog = true }
        )

        if (state.showDeveloperSettings) {
            HorizontalDivider()
            SettingsSectionHeader("Developer Settings")
            DeveloperSettingsSection(
                devDateOverride = state.devDateOverride,
                devForceHolidayTheme = state.devForceHolidayTheme,
                onDateOverrideChange = { onIntent(SettingsIntent.SetDevDateOverride(it)) },
                onForceHolidayThemeChange = { onIntent(SettingsIntent.SetDevForceHolidayTheme(it)) }
            )
        }
    }

    if (showMinhagDialog) {
        MinhagDialog(
            selected = state.minhag,
            onSelect = {
                onIntent(SettingsIntent.SetMinhag(it))
                showMinhagDialog = false
            },
            onDismiss = { showMinhagDialog = false }
        )
    }

    if (showAppLanguageDialog) {
        AppLanguageDialog(
            selected = state.appLanguage,
            onSelect = {
                onIntent(SettingsIntent.SetAppLanguage(it))
                showAppLanguageDialog = false
            },
            onDismiss = { showAppLanguageDialog = false }
        )
    }

    if (showCandleLightingOffsetDialog) {
        CandleLightingOffsetDialog(
            offset = state.candleLightingOffset,
            onOffsetChange = { onIntent(SettingsIntent.SetCandleLightingOffset(it)) },
            onDismiss = { showCandleLightingOffsetDialog = false }
        )
    }

    if (showCandleLightingModeDialog) {
        CandleLightingModeDialog(
            mode = state.candleLightingNotifyMode,
            onSelect = {
                onIntent(SettingsIntent.SetCandleLightingNotifyMode(it))
                showCandleLightingModeDialog = false
            },
            onDismiss = { showCandleLightingModeDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun MinhagDialog(
    selected: Minhag,
    onSelect: (Minhag) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Minhag") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                Minhag.entries.forEach { minhag ->
                    val label = minhag.name.lowercase().replaceFirstChar { it.uppercase() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = minhag == selected,
                                onClick = { onSelect(minhag) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = minhag == selected, onClick = null)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AppLanguageDialog(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Language") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    val label = when (language) {
                        AppLanguage.SYSTEM -> "System default"
                        AppLanguage.HEBREW -> "Hebrew (עברית)"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = language == selected,
                                onClick = { onSelect(language) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = language == selected, onClick = null)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CandleLightingOffsetDialog(
    offset: Double,
    onOffsetChange: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember(offset) { mutableFloatStateOf(offset.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Candle Lighting Offset") },
        text = {
            Column {
                Text(
                    text = "${sliderValue.roundToInt()} min before sunset",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 10f..60f,
                    steps = 49
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onOffsetChange(sliderValue.roundToInt().toDouble())
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CandleLightingModeDialog(
    mode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf("morning" to "Morning of erev", "hours_before" to "Hours before")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Mode") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = mode == value,
                                onClick = { onSelect(value) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == value, onClick = null)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private val ALL_ZMANIM = listOf(
    "Alot HaShachar", "Misheyakir", "Sunrise",
    "Sof Zman Shema (GRA)", "Sof Zman Shema (MGA)", "Sof Zman Tefillah",
    "Chatzot", "Mincha Gedolah", "Mincha Ketanah", "Plag HaMincha",
    "Candle Lighting", "Sunset", "Nightfall", "Havdalah", "Chatzot HaLaylah"
)

@Composable
private fun HalachicTimesSection(
    selectedZmanim: Set<String>,
    onToggle: (name: String, enabled: Boolean) -> Unit
) {
    val atMax = selectedZmanim.size >= 5

    ALL_ZMANIM.forEach { name ->
        val checked = name in selectedZmanim
        val enabled = checked || !atMax
        ListItem(
            headlineContent = {
                Text(
                    text = name,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            },
            trailingContent = {
                Checkbox(
                    checked = checked,
                    onCheckedChange = if (enabled) {
                        { onToggle(name, it) }
                    } else null,
                    enabled = enabled
                )
            },
            modifier = Modifier.clickable(enabled = enabled) {
                onToggle(name, !checked)
            }
        )
    }
}

@Composable
private fun NotificationsSection(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onShowModeDialog: () -> Unit
) {
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

    // Candle Lighting
    ListItem(
        headlineContent = { Text("Candle Lighting") },
        leadingContent = {
            Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
        },
        trailingContent = {
            Switch(
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
        }
    )

    if (state.notifyCandleLighting) {
        val modeLabel = when (state.candleLightingNotifyMode) {
            "morning" -> "Morning of erev"
            "hours_before" -> "Hours before"
            else -> state.candleLightingNotifyMode
        }
        ListItem(
            headlineContent = { Text("Notify mode") },
            supportingContent = { Text(modeLabel) },
            leadingContent = { Spacer(modifier = Modifier.padding(start = 16.dp)) },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onShowModeDialog)
        )

        if (state.candleLightingNotifyMode == "morning") {
            val hours = state.candleLightingMorningTime / 60
            val minutes = state.candleLightingMorningTime % 60
            val timeLabel = "At %d:%02d AM".format(
                if (hours == 0) 12 else if (hours > 12) hours - 12 else hours,
                minutes
            )
            ListItem(
                headlineContent = { Text("Notification time") },
                supportingContent = { Text(timeLabel) },
                leadingContent = { Spacer(modifier = Modifier.padding(start = 16.dp)) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            Icon(Icons.Filled.Remove, contentDescription = "Earlier")
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
                            Icon(Icons.Filled.Add, contentDescription = "Later")
                        }
                    }
                }
            )
        }

        if (state.candleLightingNotifyMode == "hours_before") {
            ListItem(
                headlineContent = { Text("Hours before") },
                supportingContent = { Text("${state.candleLightingHoursBefore} hours before sunset") },
                leadingContent = { Spacer(modifier = Modifier.padding(start = 16.dp)) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease")
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
                            Icon(Icons.Filled.Add, contentDescription = "Increase")
                        }
                    }
                }
            )
        }
    }

    // Holidays
    ListItem(
        headlineContent = { Text("Holidays") },
        leadingContent = {
            Icon(Icons.Outlined.Event, contentDescription = null)
        },
        trailingContent = {
            Switch(
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
        }
    )

    if (state.notifyHolidays) {
        ListItem(
            headlineContent = { Text("Days before") },
            supportingContent = { Text("${state.holidayNotifyDaysBefore} days before") },
            leadingContent = { Spacer(modifier = Modifier.padding(start = 16.dp)) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
        )
    }

    // Fasts
    ListItem(
        headlineContent = { Text("Fasts") },
        leadingContent = {
            Icon(Icons.Outlined.NoFood, contentDescription = null)
        },
        trailingContent = {
            Switch(
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
        }
    )

    // Personal Events
    ListItem(
        headlineContent = { Text("Personal Events") },
        leadingContent = {
            Icon(Icons.Outlined.Person, contentDescription = null)
        },
        trailingContent = {
            Switch(
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
        }
    )

    // Omer
    ListItem(
        headlineContent = { Text("Omer (at sunset)") },
        leadingContent = {
            Icon(Icons.Outlined.Stars, contentDescription = null)
        },
        trailingContent = {
            Switch(
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
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeveloperSettingsSection(
    devDateOverride: LocalDate?,
    devForceHolidayTheme: String?,
    onDateOverrideChange: (LocalDate?) -> Unit,
    onForceHolidayThemeChange: (String?) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
        Spacer(modifier = Modifier.height(16.dp))
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
