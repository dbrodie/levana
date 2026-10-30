package ninja.doskey.app.levana.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.NoFood
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.R
import kotlinx.coroutines.launch

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
