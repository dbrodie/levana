package ninja.doskey.app.levana.ui.calendarselection

import ninja.doskey.app.levana.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ninja.doskey.app.levana.domain.model.DeviceCalendar
import ninja.doskey.app.levana.ui.settings.SettingsCheckboxRow
import org.koin.androidx.compose.koinViewModel
import ninja.doskey.app.levana.ui.ContentWidthBox
import ninja.doskey.app.levana.ui.PermissionBlockedNote
import ninja.doskey.app.levana.ui.PermissionButton
import ninja.doskey.app.levana.ui.rememberPermissionRequester

@Composable
fun CalendarSelectionScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarSelectionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // A grant made in system settings shows on return
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!viewModel.state.value.hasPermission) viewModel.onIntent(CalendarSelectionIntent.Load)
    }

    CalendarSelectionContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
fun CalendarSelectionContent(
    state: CalendarSelectionState,
    onIntent: (CalendarSelectionIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        !state.hasPermission -> {
            val permission = rememberPermissionRequester(Manifest.permission.READ_CALENDAR) { granted ->
                if (granted) onIntent(CalendarSelectionIntent.PermissionGranted)
            }
            PermissionRequest(
                blocked = permission.blocked,
                onRequest = permission::request,
                modifier = modifier
            )
        }
        state.calendars.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.calendar_selection_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            CalendarList(
                calendars = state.calendars,
                selectedIds = state.selectedIds,
                onToggle = { id ->
                    onIntent(CalendarSelectionIntent.ToggleCalendar(id))
                },
                modifier = modifier
            )
        }
    }
}

/** Asks for calendar access; once the system stops asking ([blocked]), points to system settings. */
@Composable
internal fun PermissionRequest(blocked: Boolean, onRequest: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.calendar_selection_permission_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.calendar_selection_permission_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PermissionBlockedNote(blocked)
        Spacer(modifier = Modifier.height(16.dp))
        PermissionButton(blocked = blocked, onClick = onRequest)
    }
}

@Composable
private fun CalendarList(
    calendars: List<DeviceCalendar>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    ContentWidthBox(modifier.fillMaxSize()) { gutter ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentPadding = PaddingValues(horizontal = gutter),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.calendar_selection_prompt),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(calendars, key = { it.id }) { calendar ->
                CalendarRow(
                    calendar = calendar,
                    isSelected = calendar.id in selectedIds,
                    onToggle = { onToggle(calendar.id) }
                )
            }
        }
    }
}

@Composable
private fun CalendarRow(calendar: DeviceCalendar, isSelected: Boolean, onToggle: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        // One labeled checkbox: the row owns the toggle; the color dot is decorative.
        SettingsCheckboxRow(
            title = calendar.name,
            description = calendar.accountName.takeIf { it.isNotBlank() },
            checked = isSelected,
            onCheckedChange = { onToggle() },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(calendar.color))
                )
            }
        )
    }
}
