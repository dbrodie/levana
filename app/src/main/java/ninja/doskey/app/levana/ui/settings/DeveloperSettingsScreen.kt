package ninja.doskey.app.levana.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.ui.calendar.GoToDateDialog
import ninja.doskey.app.levana.ui.theme.HolidayTheme
import java.util.GregorianCalendar
import org.koin.androidx.compose.koinViewModel

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
