package ninja.doskey.app.levana.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import ninja.doskey.app.levana.R

/**
 * The app's top bar for every route but the calendar: the route's title, the menu button on
 * drawer routes or back elsewhere, and My Events' registered [actions].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LevanaTopBar(
    backStackEntry: NavBackStackEntry?,
    showDrawer: Boolean,
    canGoBack: Boolean,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    actions: (@Composable () -> Unit)?,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        title = {
            val destination = backStackEntry?.destination
            val route = destination?.let { dest -> TitledRoutes.firstOrNull { dest.hasRoute(it) } }
            val eventId = if (route == AddEditEventRoute::class) {
                backStackEntry?.toRoute<AddEditEventRoute>()?.eventId ?: 0L
            } else {
                0L
            }
            Text(stringResource(topBarTitleFor(route, eventId)), style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = {
            if (showDrawer) {
                IconButton(
                    onClick = onOpenDrawer
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.design_open_menu))
                }
            } else if (canGoBack) {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
        actions = {
            if (backStackEntry?.destination?.hasRoute(PersonalEventsRoute::class) == true) {
                actions?.invoke()
            }
        }
    )
}
