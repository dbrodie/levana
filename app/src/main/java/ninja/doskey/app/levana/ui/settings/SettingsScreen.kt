package ninja.doskey.app.levana.ui.settings

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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NoFood
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.displayName
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.data.LocationService
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.ui.calendar.GoToDateDialog
import ninja.doskey.app.levana.ui.targetedLabel
import ninja.doskey.app.levana.domain.model.YahrzeitFirstYear
import ninja.doskey.app.levana.domain.model.YahrzeitLeapYearAdar
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import ninja.doskey.app.levana.domain.model.IsraelMode
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.ui.theme.HolidayTheme
import ninja.doskey.app.levana.update.UpdateChecker
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.LocalDate
import java.util.GregorianCalendar
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onChangeLocation: () -> Unit,
    onSystemCalendars: () -> Unit,
    onHalachicTimesSettings: () -> Unit,
    onDeveloperSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsContent(
        state = state,
        onIntent = viewModel::onIntent,
        onChangeLocation = onChangeLocation,
        onSystemCalendars = onSystemCalendars,
        onHalachicTimesSettings = onHalachicTimesSettings,
        onDeveloperSettings = onDeveloperSettings,
        onAbout = onAbout,
        modifier = modifier
    )
}

@Composable
fun HalachicTimesSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        HalachicTimesSection(
            selectedZmanim = state.selectedZmanim,
            onToggle = { name, enabled ->
                viewModel.onIntent(SettingsIntent.ToggleZman(name, enabled))
            }
        )
    }
}

@Composable
fun DeveloperSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDateDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        ListItem(
            headlineContent = { Text("Date Override") },
            supportingContent = {
                Text(
                    state.overriddenDeviceDate?.let { date ->
                        val jd = JewishDate(
                            GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
                        )
                        "${jd.jewishDayOfMonth}/${jd.jewishMonth}/${jd.jewishYear} (Hebrew)"
                    } ?: "Not set"
                )
            },
            leadingContent = {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
            },
            modifier = Modifier.clickable { showDateDialog = true }
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Force Holiday Theme",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            HolidayThemePicker(
                selected = state.devForceHolidayTheme,
                onSelect = { viewModel.onIntent(SettingsIntent.SetDevForceHolidayTheme(it)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        ListItem(
            headlineContent = { Text("Test crash") },
            supportingContent = { Text("Crash the app to check the crash report dialog") },
            leadingContent = {
                Icon(Icons.Outlined.BugReport, contentDescription = null)
            },
            modifier = Modifier.clickable {
                throw RuntimeException("Test crash from Developer Settings")
            }
        )
    }

    if (showDateDialog) {
        GoToDateDialog(
            initialDate = state.overriddenDeviceDate ?: state.deviceToday ?: return,
            calendarHebrewMode = true,
            onConfirm = { date ->
                viewModel.onIntent(SettingsIntent.SetDevDateOverride(date))
                showDateDialog = false
            },
            onDismiss = { showDateDialog = false },
            onClear = {
                viewModel.onIntent(SettingsIntent.SetDevDateOverride(null))
                showDateDialog = false
            }
        )
    }
}

@Composable
fun LocationsSettingsScreen(
    onAddLocation: () -> Unit,
    modifier: Modifier = Modifier,
    preferencesRepository: PreferencesRepository = koinInject(),
    locationService: LocationService = koinInject(),
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var hasLocationPermission by remember { mutableStateOf(locationService.hasPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPermission = results.values.any { it }
        if (hasLocationPermission) {
            scope.launch {
                locationService.getCurrentLocationOrNull()
                    ?.let { preferencesRepository.updateCurrentLocation(it) }
            }
        }
    }

    LocationsSettingsContent(
        currentLocationName = state.currentLocationName,
        hasLocationPermission = hasLocationPermission,
        savedLocations = state.savedLocations,
        onRequestPermission = { permissionLauncher.launch(LocationService.PERMISSIONS) },
        onDelete = { id -> scope.launch { preferencesRepository.removeSavedLocation(id) } },
        onAddLocation = onAddLocation,
        modifier = modifier
    )
}

@Composable
internal fun LocationsSettingsContent(
    currentLocationName: String,
    hasLocationPermission: Boolean,
    savedLocations: List<SavedLocation>,
    onRequestPermission: () -> Unit,
    onDelete: (String) -> Unit,
    onAddLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsGroup(stringResource(R.string.settings_group_current_location)) {
        ListItem(
            headlineContent = { Text(currentLocationName) },
            supportingContent = if (hasLocationPermission) null else {
                { Text(stringResource(R.string.location_permission_needed)) }
            },
            leadingContent = {
                Icon(Icons.Filled.GpsFixed, contentDescription = null)
            },
            modifier = if (hasLocationPermission) Modifier
                else Modifier.clickable(onClick = onRequestPermission)
        )
        }

        if (savedLocations.isNotEmpty()) {
            SettingsGroup(stringResource(R.string.settings_group_saved_locations)) {
            savedLocations.forEach { saved ->
                ListItem(
                    headlineContent = { Text(saved.location.name) },
                    supportingContent = if (saved.location.country.isNotEmpty()) {
                        { Text(saved.location.country) }
                    } else null,
                    leadingContent = {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null)
                    },
                    trailingContent = {
                        IconButton(onClick = { onDelete(saved.id) }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = targetedLabel(
                                    R.string.a11y_delete_saved_location,
                                    saved.location.name,
                                    R.string.location_delete
                                )
                            )
                        }
                    }
                )
            }
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_locations)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.location_add)) },
            leadingContent = {
                Icon(Icons.Filled.Add, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onAddLocation)
        )
        }
    }
}

@Composable
fun SettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onChangeLocation: () -> Unit,
    onSystemCalendars: () -> Unit,
    onHalachicTimesSettings: () -> Unit,
    onDeveloperSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showYahrzeitLeapDialog by remember { mutableStateOf(false) }
    var showYahrzeitFirstYearDialog by remember { mutableStateOf(false) }
    var showYahrzeitMissingDayDialog by remember { mutableStateOf(false) }
    var showAppLanguageDialog by remember { mutableStateOf(false) }
    var showIsraelModeDialog by remember { mutableStateOf(false) }
    var showCandleLightingOffsetDialog by remember { mutableStateOf(false) }
    var showCandleLightingModeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsGroup(stringResource(R.string.settings_group_calendar)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_location_label)) },
            leadingContent = {
                Icon(Icons.Outlined.LocationOn, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onChangeLocation)
        )
        SettingsRowDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_system_calendars_label)) },
            leadingContent = {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onSystemCalendars)
        )
        }

        SettingsGroup(stringResource(R.string.settings_group_preferences)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_israel_diaspora_label)) },
            supportingContent = {
                Column {
                    Text(israelModeSummary(state.israelMode, state.isInIsrael))
                    Text(stringResource(R.string.settings_israel_diaspora_description))
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Public, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable { showIsraelModeDialog = true }
        )
        SettingsRowDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_modern_holidays_label),
            description = stringResource(R.string.settings_modern_holidays_description),
            leadingContent = {
                Icon(Icons.Outlined.Flag, contentDescription = null)
            },
            checked = state.showModernIsraeliHolidays,
            onCheckedChange = { onIntent(SettingsIntent.SetShowModernIsraeli(it)) }
        )
        SettingsRowDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_candle_offset_label)) },
            supportingContent = { Text(stringResource(R.string.settings_candle_offset_description, state.candleLightingOffset.toInt())) },
            leadingContent = {
                Icon(Icons.Outlined.WbTwilight, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable { showCandleLightingOffsetDialog = true }
        )
        }

        SettingsGroup(stringResource(R.string.settings_group_yahrzeit)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_yahrzeit_leap_label)) },
            supportingContent = { Text(yahrzeitLeapLabel(state.yahrzeitRules.leapYearAdar)) },
            leadingContent = {
                Icon(Icons.Outlined.WbTwilight, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable { showYahrzeitLeapDialog = true }
        )
        SettingsRowDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_yahrzeit_first_year_label)) },
            supportingContent = { Text(yahrzeitFirstYearLabel(state.yahrzeitRules.firstYear)) },
            leadingContent = {
                Icon(Icons.Outlined.WbTwilight, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable { showYahrzeitFirstYearDialog = true }
        )
        SettingsRowDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_yahrzeit_nisan_label),
            description = stringResource(R.string.settings_yahrzeit_nisan_description),
            leadingContent = {
                Icon(Icons.Outlined.WbTwilight, contentDescription = null)
            },
            checked = state.yahrzeitRules.adarI30AlsoNisan1,
            onCheckedChange = { onIntent(SettingsIntent.SetYahrzeitAdarI30AlsoNisan1(it)) }
        )
        SettingsRowDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_yahrzeit_missing_day_label)) },
            supportingContent = { Text(yahrzeitMissingDayShortLabel(state.yahrzeitRules.missingDay)) },
            leadingContent = {
                Icon(Icons.Outlined.WbTwilight, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable { showYahrzeitMissingDayDialog = true }
        )
        }

        val appLanguageLabel = when (state.appLanguage) {
            AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
            AppLanguage.HEBREW -> stringResource(R.string.settings_language_hebrew)
        }
        SettingsGroup(stringResource(R.string.settings_group_appearance)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_app_language_label)) },
            supportingContent = { Text(appLanguageLabel) },
            leadingContent = {
                Icon(Icons.Outlined.Language, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable { showAppLanguageDialog = true }
        )
        SettingsRowDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_holiday_theming_label),
            description = stringResource(R.string.settings_holiday_theming_description),
            leadingContent = {
                Icon(Icons.Outlined.Palette, contentDescription = null)
            },
            checked = state.dynamicHolidayTheme,
            onCheckedChange = { onIntent(SettingsIntent.SetDynamicHolidayTheme(it)) }
        )
        }

        val selectedCount = state.selectedZmanim.size
        SettingsGroup(stringResource(R.string.settings_group_halachic_times)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_halachic_times_title)) },
            supportingContent = { Text(stringResource(R.string.settings_halachic_times_count, selectedCount)) },
            leadingContent = {
                Icon(Icons.Outlined.WbTwilight, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onHalachicTimesSettings)
        )
        }

        SettingsGroup(stringResource(R.string.settings_group_notifications)) {
        NotificationsSection(
            state = state,
            onIntent = onIntent,
            onShowModeDialog = { showCandleLightingModeDialog = true }
        )
        }

        if (state.showDeveloperSettings) {
            SettingsGroup(stringResource(R.string.settings_group_developer)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_developer_settings)) },
                leadingContent = {
                    Icon(Icons.Outlined.Person, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onDeveloperSettings)
            )
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_about)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_about)) },
            leadingContent = {
                Icon(Icons.Outlined.Info, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onAbout)
        )
        }
    }

    if (showYahrzeitLeapDialog) {
        OptionDialog(
            title = stringResource(R.string.settings_yahrzeit_leap_label),
            description = stringResource(R.string.settings_yahrzeit_leap_description),
            options = YahrzeitLeapYearAdar.entries,
            selected = state.yahrzeitRules.leapYearAdar,
            label = { yahrzeitLeapLabel(it) },
            onSelect = {
                onIntent(SettingsIntent.SetYahrzeitLeapYearAdar(it))
                showYahrzeitLeapDialog = false
            },
            onDismiss = { showYahrzeitLeapDialog = false }
        )
    }

    if (showYahrzeitFirstYearDialog) {
        OptionDialog(
            title = stringResource(R.string.settings_yahrzeit_first_year_label),
            description = stringResource(R.string.settings_yahrzeit_first_year_description),
            options = YahrzeitFirstYear.entries,
            selected = state.yahrzeitRules.firstYear,
            label = { yahrzeitFirstYearLabel(it) },
            onSelect = {
                onIntent(SettingsIntent.SetYahrzeitFirstYear(it))
                showYahrzeitFirstYearDialog = false
            },
            onDismiss = { showYahrzeitFirstYearDialog = false }
        )
    }

    if (showYahrzeitMissingDayDialog) {
        OptionDialog(
            title = stringResource(R.string.settings_yahrzeit_missing_day_label),
            description = stringResource(R.string.settings_yahrzeit_missing_day_description),
            options = YahrzeitMissingDay.entries,
            selected = state.yahrzeitRules.missingDay,
            label = { yahrzeitMissingDayLabel(it) },
            onSelect = {
                onIntent(SettingsIntent.SetYahrzeitMissingDay(it))
                showYahrzeitMissingDayDialog = false
            },
            onDismiss = { showYahrzeitMissingDayDialog = false }
        )
    }

    if (showIsraelModeDialog) {
        IsraelModeDialog(
            selected = state.israelMode,
            onSelect = {
                onIntent(SettingsIntent.SetIsraelMode(it))
                showIsraelModeDialog = false
            },
            onDismiss = { showIsraelModeDialog = false }
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
private fun SettingsGroup(label: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            content()
        }
    }
}

/**
 * A settings row with a trailing switch. The whole row is one toggleable element
 * (Role.Switch), so a tap anywhere toggles it once and TalkBack reads one switch
 * with its label. The Switch itself only displays the state.
 */
@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingContent: @Composable () -> Unit,
    description: String? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = description?.let { { Text(it) } },
        leadingContent = leadingContent,
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null)
        },
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange
        )
    )
}

@Composable
private fun SettingsRowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 16.dp)
    )
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 20.dp, bottom = 6.dp)
    )
}

/** Single-choice dialog with an explanation above the options. */
@Composable
private fun <T> OptionDialog(
    title: String,
    description: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                onClick = { onSelect(option) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun yahrzeitLeapLabel(value: YahrzeitLeapYearAdar): String = stringResource(
    when (value) {
        YahrzeitLeapYearAdar.ADAR_I -> R.string.settings_yahrzeit_leap_adar_i
        YahrzeitLeapYearAdar.ADAR_II -> R.string.settings_yahrzeit_leap_adar_ii
        YahrzeitLeapYearAdar.BOTH -> R.string.settings_yahrzeit_leap_both
    }
)

@Composable
private fun yahrzeitFirstYearLabel(value: YahrzeitFirstYear): String = stringResource(
    when (value) {
        YahrzeitFirstYear.FOLLOW_ANNUAL -> R.string.settings_yahrzeit_first_year_follow
        YahrzeitFirstYear.ADAR_I -> R.string.settings_yahrzeit_first_year_adar_i
        YahrzeitFirstYear.BOTH -> R.string.settings_yahrzeit_first_year_both
    }
)

@Composable
private fun yahrzeitMissingDayLabel(value: YahrzeitMissingDay): String = stringResource(
    when (value) {
        YahrzeitMissingDay.FIRST_ANNIVERSARY -> R.string.settings_yahrzeit_missing_day_first_anniversary
        YahrzeitMissingDay.ROSH_CHODESH -> R.string.settings_yahrzeit_missing_day_rosh_chodesh
        YahrzeitMissingDay.BOTH -> R.string.settings_yahrzeit_missing_day_both
    }
)

@Composable
private fun yahrzeitMissingDayShortLabel(value: YahrzeitMissingDay): String = stringResource(
    when (value) {
        YahrzeitMissingDay.FIRST_ANNIVERSARY -> R.string.settings_yahrzeit_missing_day_first_anniversary_short
        YahrzeitMissingDay.ROSH_CHODESH -> R.string.settings_yahrzeit_missing_day_rosh_chodesh_short
        YahrzeitMissingDay.BOTH -> R.string.settings_yahrzeit_missing_day_both_short
    }
)

@Composable
private fun israelModeLabel(mode: IsraelMode): String = stringResource(
    when (mode) {
        IsraelMode.AUTO -> R.string.settings_israel_mode_auto
        IsraelMode.ISRAEL -> R.string.settings_israel_mode_israel
        IsraelMode.DIASPORA -> R.string.settings_israel_mode_diaspora
    }
)

/** The chosen option; Auto-detect also names what it detected on this device. */
@Composable
private fun israelModeSummary(mode: IsraelMode, isInIsrael: Boolean): String =
    if (mode == IsraelMode.AUTO) {
        stringResource(
            R.string.settings_israel_mode_auto_detected,
            israelModeLabel(if (isInIsrael) IsraelMode.ISRAEL else IsraelMode.DIASPORA)
        )
    } else {
        israelModeLabel(mode)
    }

@Composable
private fun IsraelModeDialog(
    selected: IsraelMode,
    onSelect: (IsraelMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_israel_diaspora_label)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                IsraelMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = mode == selected,
                                onClick = { onSelect(mode) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == selected, onClick = null)
                        Text(
                            text = israelModeLabel(mode),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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
        title = { Text(stringResource(R.string.settings_app_language_dialog_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    val label = when (language) {
                        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                        AppLanguage.HEBREW -> stringResource(R.string.settings_language_hebrew)
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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
        title = { Text(stringResource(R.string.settings_candle_offset_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_candle_offset_description, sliderValue.roundToInt()),
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
            }) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun CandleLightingModeDialog(
    mode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "morning" to stringResource(R.string.settings_notify_mode_morning),
        "hours_before" to stringResource(R.string.settings_notify_mode_hours_before)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_notification_mode_title)) },
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
internal fun HalachicTimesSection(
    selectedZmanim: Set<String>,
    onToggle: (name: String, enabled: Boolean) -> Unit
) {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.zmanim_selection_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val hebrewUi = LocalConfiguration.current.locales[0].language in setOf("he", "iw")
        // Single line: the name in the app language. Keys are the English names
        ZmanimRepository.ZMAN_NAMES.forEach { zmanName ->
            val name = zmanName.english
            val checked = name in selectedZmanim
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                SettingsCheckboxRow(
                    title = zmanName.displayName(hebrewUi).primary,
                    checked = checked,
                    onCheckedChange = { onToggle(name, it) }
                )
            }
        }
    }
}

@Composable
internal fun NotificationsSection(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onShowModeDialog: () -> Unit
) {
    val context = LocalContext.current

    // The switch the user tried to turn on while the permission dialog is showing.
    // Saved by name so it survives configuration changes (e.g. rotation).
    var pendingSwitchName by rememberSaveable { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionResultIntent(pendingSwitchName, granted)?.let(onIntent)
        pendingSwitchName = null
    }

    fun setNotificationSwitch(switch: NotificationSwitch, enabled: Boolean) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!enabled || granted) {
            onIntent(switch.intent(enabled))
        } else {
            pendingSwitchName = switch.name
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Candle Lighting
    SettingsSwitchRow(
        title = stringResource(R.string.settings_notify_candle_lighting),
        leadingContent = {
            Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
        },
        checked = state.notifyCandleLighting,
        onCheckedChange = { setNotificationSwitch(NotificationSwitch.CandleLighting, it) }
    )

    if (state.notifyCandleLighting) {
        val modeLabel = when (state.candleLightingNotifyMode) {
            "morning" -> stringResource(R.string.settings_notify_mode_morning)
            "hours_before" -> stringResource(R.string.settings_notify_mode_hours_before)
            else -> state.candleLightingNotifyMode
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_notify_mode)) },
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
            val timeLabel = stringResource(
                R.string.settings_at_morning_time,
                if (hours == 0) 12 else if (hours > 12) hours - 12 else hours,
                minutes,
                hours
            )
            SettingsStepperRow(
                title = stringResource(R.string.settings_notification_time),
                valueText = timeLabel,
                value = state.candleLightingMorningTime,
                range = 360..720,
                step = 30,
                decreaseAction = stringResource(R.string.settings_earlier),
                increaseAction = stringResource(R.string.settings_later),
                onValueChange = { onIntent(SettingsIntent.SetCandleLightingMorningTime(it)) }
            )
        }

        if (state.candleLightingNotifyMode == "hours_before") {
            SettingsStepperRow(
                title = stringResource(R.string.settings_hours_before),
                valueText = stringResource(R.string.settings_hours_before_sunset, state.candleLightingHoursBefore),
                value = state.candleLightingHoursBefore,
                range = 1..6,
                step = 1,
                decreaseAction = stringResource(R.string.settings_decrease),
                increaseAction = stringResource(R.string.settings_increase),
                onValueChange = { onIntent(SettingsIntent.SetCandleLightingHoursBefore(it)) }
            )
        }
    }

    // Holidays
    SettingsSwitchRow(
        title = stringResource(R.string.settings_notify_holidays),
        leadingContent = {
            Icon(Icons.Outlined.Event, contentDescription = null)
        },
        checked = state.notifyHolidays,
        onCheckedChange = { setNotificationSwitch(NotificationSwitch.Holidays, it) }
    )

    if (state.notifyHolidays) {
        SettingsStepperRow(
            title = stringResource(R.string.settings_days_before),
            valueText = stringResource(R.string.settings_days_before_value, state.holidayNotifyDaysBefore),
            value = state.holidayNotifyDaysBefore,
            range = 0..14,
            step = 1,
            decreaseAction = stringResource(R.string.settings_decrease),
            increaseAction = stringResource(R.string.settings_increase),
            onValueChange = { onIntent(SettingsIntent.SetHolidayNotifyDaysBefore(it)) }
        )
    }

    // Fasts
    SettingsSwitchRow(
        title = stringResource(R.string.settings_notify_fasts),
        leadingContent = {
            Icon(Icons.Outlined.NoFood, contentDescription = null)
        },
        checked = state.notifyFasts,
        onCheckedChange = { setNotificationSwitch(NotificationSwitch.Fasts, it) }
    )

    // Personal Events
    SettingsSwitchRow(
        title = stringResource(R.string.settings_notify_personal_events),
        leadingContent = {
            Icon(Icons.Outlined.Person, contentDescription = null)
        },
        checked = state.notifyPersonalEvents,
        onCheckedChange = { setNotificationSwitch(NotificationSwitch.PersonalEvents, it) }
    )

    // Omer
    SettingsSwitchRow(
        title = stringResource(R.string.settings_notify_omer),
        leadingContent = {
            Icon(Icons.Outlined.Stars, contentDescription = null)
        },
        checked = state.notifyOmer,
        onCheckedChange = { setNotificationSwitch(NotificationSwitch.Omer, it) }
    )

    if (state.notifyOmer) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_omer_at_tzait),
            leadingContent = { Spacer(modifier = Modifier.padding(start = 16.dp)) },
            checked = state.notifyOmerTzait,
            onCheckedChange = { setNotificationSwitch(NotificationSwitch.OmerTzait, it) }
        )

        SettingsSwitchRow(
            title = stringResource(R.string.settings_omer_morning_reminder),
            leadingContent = { Spacer(modifier = Modifier.padding(start = 16.dp)) },
            checked = state.notifyOmerMorning,
            onCheckedChange = { setNotificationSwitch(NotificationSwitch.OmerMorning, it) }
        )

        if (state.notifyOmerMorning) {
            val hours = state.notifyOmerMorningTime / 60
            val minutes = state.notifyOmerMorningTime % 60
            val timeLabel = stringResource(
                R.string.settings_at_morning_time,
                if (hours == 0) 12 else if (hours > 12) hours - 12 else hours,
                minutes,
                hours
            )
            SettingsStepperRow(
                title = stringResource(R.string.settings_reminder_time),
                valueText = timeLabel,
                value = state.notifyOmerMorningTime,
                range = 360..720,
                step = 30,
                decreaseAction = stringResource(R.string.settings_earlier),
                increaseAction = stringResource(R.string.settings_later),
                onValueChange = { onIntent(SettingsIntent.SetNotifyOmerMorningTime(it)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeveloperSettingsSection(
    overriddenDate: LocalDate?,
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
            currentDate = overriddenDate,
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
    // Convert stored Gregorian LocalDate back to Hebrew for display and initial field values
    val currentHebrew = currentDate?.let { date ->
        JewishDate(GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth))
    }

    var yearText by remember(currentDate) {
        mutableStateOf(currentHebrew?.jewishYear?.toString() ?: "")
    }
    var monthText by remember(currentDate) {
        mutableStateOf(currentHebrew?.jewishMonth?.toString() ?: "")
    }
    var dayText by remember(currentDate) {
        mutableStateOf(currentHebrew?.jewishDayOfMonth?.toString() ?: "")
    }

    if (currentHebrew != null) {
        Text(
            text = "Currently: ${currentHebrew.jewishDayOfMonth}/${currentHebrew.jewishMonth}/${currentHebrew.jewishYear} (Hebrew)",
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
            label = { Text("Hebrew Year") },
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
            // Invalid input is ignored
            parseHebrewDateOverride(yearText, monthText, dayText)?.let(onDateChange)
        }) {
            Text("Apply")
        }
    }
}

/**
 * The Gregorian date for a Hebrew date typed into the Date Override fields,
 * or null when it is not a real date. The year must be one the date pickers
 * accept: an override far outside that range makes the app's "today" a date
 * the calendar cannot render, and the override persists across restarts.
 */
internal fun parseHebrewDateOverride(yearText: String, monthText: String, dayText: String): LocalDate? =
    try {
        val y = yearText.trim().toInt()
        val m = monthText.trim().toInt()
        val d = dayText.trim().toInt()
        require(y in HebrewDateMatcher.EDITABLE_YEARS)
        val gc = JewishDate(y, m, d).gregorianCalendar
        LocalDate.of(
            gc.get(GregorianCalendar.YEAR),
            gc.get(GregorianCalendar.MONTH) + 1,
            gc.get(GregorianCalendar.DAY_OF_MONTH)
        )
    } catch (_: Exception) {
        null
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HolidayThemePicker(selected: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options: List<HolidayTheme?> = listOf(null) + HolidayTheme.entries
    val selectedTheme = selected?.let { name -> HolidayTheme.entries.firstOrNull { it.name == name } }
    val displayValue = holidayThemeLabel(selectedTheme)

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
                    text = { Text(holidayThemeLabel(option)) },
                    onClick = {
                        onSelect(option?.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun holidayThemeLabel(theme: HolidayTheme?): String =
    stringResource(theme?.labelRes ?: R.string.holiday_theme_none)

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    updateChecker: UpdateChecker = koinInject()
) {
    val versionName = updateChecker.currentVersionName()
    val installSource = updateChecker.installerPackageName()
        ?: stringResource(R.string.about_install_source_none)
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_version)) },
            supportingContent = { Text(versionName) }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_install_source)) },
            supportingContent = { Text(installSource) }
        )
    }
}
