package ninja.doskey.app.levana.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.YahrzeitFirstYear
import ninja.doskey.app.levana.domain.model.YahrzeitLeapYearAdar
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import org.koin.androidx.compose.koinViewModel

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
