package ninja.doskey.app.levana.ui.settings

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
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.ui.targetedLabel
import ninja.doskey.app.levana.domain.model.SavedLocation
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

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
