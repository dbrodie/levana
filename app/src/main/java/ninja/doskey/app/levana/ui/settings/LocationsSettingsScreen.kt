package ninja.doskey.app.levana.ui.settings

import ninja.doskey.app.levana.ui.placeName
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ninja.doskey.app.levana.data.LocationService
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.ui.targetedLabel
import ninja.doskey.app.levana.domain.model.SavedLocation
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import ninja.doskey.app.levana.ui.contentWidth

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

    // The selected city, as UserPreferences.displayCity: the active saved city, else the current location
    val selected = state.savedLocations.find { it.id == state.activeLocationId }
    LocationsSettingsContent(
        currentLocationName = state.currentLocationName,
        hasLocationPermission = hasLocationPermission,
        savedLocations = state.savedLocations,
        incalculableTimesCityId = when {
            selected == null -> CURRENT_LOCATION_ID.takeIf { ZmanimRepository.hasIncalculableTimes(state.currentLocationLatitude) }
            else -> selected.id.takeIf { ZmanimRepository.hasIncalculableTimes(selected.location.latitude) }
        },
        onRequestPermission = { permissionLauncher.launch(LocationService.PERMISSIONS) },
        onDelete = { id -> scope.launch { preferencesRepository.removeSavedLocation(id) } },
        onAddLocation = onAddLocation,
        modifier = modifier
    )
}

/** Stands for the current location in [LocationsSettingsContent]'s incalculableTimesCityId. */
internal const val CURRENT_LOCATION_ID = "current"

/**
 * The Cities settings. [incalculableTimesCityId] is the selected city's id, or
 * [CURRENT_LOCATION_ID], when some of its times can't be calculated on some days; that row
 * carries the note.
 */
@Composable
internal fun LocationsSettingsContent(
    currentLocationName: String,
    hasLocationPermission: Boolean,
    savedLocations: List<SavedLocation>,
    onRequestPermission: () -> Unit,
    onDelete: (String) -> Unit,
    onAddLocation: () -> Unit,
    modifier: Modifier = Modifier,
    incalculableTimesCityId: String? = null
) {
    val incalculableNote = stringResource(R.string.city_incalculable_times)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .contentWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsGroup(stringResource(R.string.settings_group_current_location)) {
        ListItem(
            headlineContent = { Text(placeName(currentLocationName)) },
            supportingContent = listOfNotNull(
                stringResource(R.string.location_permission_needed).takeUnless { hasLocationPermission },
                incalculableNote.takeIf { incalculableTimesCityId == CURRENT_LOCATION_ID }
            ).takeIf { it.isNotEmpty() }?.let { lines ->
                { Column { lines.forEach { Text(it) } } }
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
                    headlineContent = { Text(placeName(saved.location.name)) },
                    supportingContent = listOfNotNull(
                        placeName(saved.location.country).takeIf { it.isNotEmpty() },
                        incalculableNote.takeIf { incalculableTimesCityId == saved.id }
                    ).takeIf { it.isNotEmpty() }?.let { lines ->
                        { Column { lines.forEach { Text(it) } } }
                    },
                    leadingContent = {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null)
                    },
                    trailingContent = {
                        IconButton(onClick = { onDelete(saved.id) }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = targetedLabel(
                                    R.string.a11y_delete_saved_location,
                                    placeName(saved.location.name),
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
