package ninja.doskey.app.levana.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.displayCity

private data class DrawerNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

/** The top-level routes listed in the drawer; they show the menu button instead of back. */
private val DrawerRoutes = listOf(CalendarRoute, PersonalEventsRoute)

internal fun isDrawerRoute(destination: NavDestination?): Boolean =
    destination?.let { dest -> DrawerRoutes.any { route -> dest.hasRoute(route::class) } } == true

/**
 * The drawer: the selected-city picker, Manage cities, and the top-level destinations.
 * [onSelectCity] stores the choice; the picker then collapses and the drawer closes.
 */
@Composable
internal fun LevanaDrawerContent(
    prefs: UserPreferences,
    drawerState: DrawerState,
    scope: CoroutineScope,
    currentDestination: NavDestination?,
    onSelectCity: suspend (id: String?) -> Unit,
    onManageCities: () -> Unit,
    onNavigateTopLevel: (route: Any) -> Unit,
) {
    // The drawer's selected-city picker; only the Zmanim screen and day panel use the city
    val locationName = prefs.displayCity.name
    val drawerNavItems = listOf(
        DrawerNavItem(stringResource(R.string.nav_calendar), Icons.Filled.CalendarMonth, CalendarRoute),
        DrawerNavItem(stringResource(R.string.design_my_events), Icons.Filled.Event, PersonalEventsRoute)
    )

    var citiesExpanded by remember { mutableStateOf(false) }
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Surface(
                onClick = { citiesExpanded = !citiesExpanded },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp),
                ) {
                    Box(
                        modifier = Modifier.size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.design_active_city),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(locationName, style = MaterialTheme.typography.titleMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Icon(if (citiesExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.design_switch_city))
                }
            }
            AnimatedVisibility(visible = citiesExpanded) {
                Column {
                    fun select(id: String?) {
                        scope.launch {
                            onSelectCity(id)
                            citiesExpanded = false
                            drawerState.close()
                        }
                    }
                    NavigationDrawerItem(
                        label = { Text(prefs.currentLocation.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        icon = { Icon(Icons.Filled.GpsFixed, contentDescription = null) },
                        selected = prefs.savedLocations.none { it.id == prefs.activeLocationId },
                        onClick = { select(null) },
                    )
                    prefs.savedLocations.forEach { saved ->
                        NavigationDrawerItem(
                            label = { Text(saved.location.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            icon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                            selected = saved.id == prefs.activeLocationId,
                            onClick = { select(saved.id) },
                        )
                    }
                }
            }
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.design_manage_cities)) },
                icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    onManageCities()
                },
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant)
            drawerNavItems.forEach { item ->
                val selected = currentDestination?.hasRoute(item.route::class) == true
                NavigationDrawerItem(
                    label = { Text(item.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                    icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                    selected = selected,
                    modifier = Modifier.padding(vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateTopLevel(item.route)
                    },
                )
            }
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_settings)) },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(22.dp)) },
                selected = currentDestination?.hasRoute(SettingsRoute::class) == true,
                modifier = Modifier.padding(vertical = 2.dp),
                onClick = {
                    scope.launch { drawerState.close() }
                    onNavigateTopLevel(SettingsRoute)
                },
            )
        }
    }
}
