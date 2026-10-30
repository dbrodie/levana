package ninja.doskey.app.levana.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.ui.walkthrough.WalkthroughIntent
import ninja.doskey.app.levana.ui.walkthrough.WalkthroughScreen
import ninja.doskey.app.levana.ui.walkthrough.activityWalkthroughViewModel
import org.koin.compose.koinInject

/**
 * The app shell: drawer, top bar and navigation, shown once the preferences have loaded.
 * [deepLinkEpochDay] (non-zero) opens the calendar on that day, and [openZmanim] that day's
 * Zmanim screen over it.
 */
@Composable
fun LevanaApp(deepLinkEpochDay: Long = 0L, openZmanim: Boolean = false) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val canGoBack = backStackEntry?.destination?.route != null &&
        navController.previousBackStackEntry != null

    val preferencesRepository: PreferencesRepository = koinInject()
    val prefs by preferencesRepository.preferences.collectAsState(
        initial = null
    )

    val currentPrefs = prefs ?: return

    // The feature walkthrough (TASK-43) comes before the shell, never over a deep link
    val walkthrough = activityWalkthroughViewModel()
    val walkthroughState by walkthrough.state.collectAsStateWithLifecycle()
    LaunchedEffect(walkthrough) {
        walkthrough.onIntent(WalkthroughIntent.Start(deepLink = deepLinkEpochDay != 0L))
    }
    if (!walkthroughState.decided) return
    if (walkthroughState.showing) {
        WalkthroughScreen(walkthroughState, walkthrough::onIntent)
        return
    }

    // The walkthrough just offered location; don't ask again at once
    LevanaStartupEffects(navController, deepLinkEpochDay, openZmanim, askForLocation = !walkthroughState.shownOnLaunch)
    ZoneMismatchNotice()

    val scope = rememberCoroutineScope()
    val showDrawer = isDrawerRoute(backStackEntry?.destination)
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Slot for per-screen actions rendered in the global TopAppBar
    var topBarActionsContent by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer,
        drawerContent = {
            LevanaDrawerContent(
                prefs = currentPrefs,
                drawerState = drawerState,
                scope = scope,
                currentDestination = backStackEntry?.destination,
                onSelectCity = { id -> preferencesRepository.setActiveLocation(id) },
                onManageCities = {
                    navController.navigate(LocationsSettingsRoute) { launchSingleTop = true }
                },
                onNavigateTopLevel = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
    ) {
        val isCalendarRoute =
            backStackEntry?.destination?.hasRoute(CalendarRoute::class) == true

        Scaffold(
            topBar = {
                if (!isCalendarRoute) {
                    LevanaTopBar(
                        backStackEntry = backStackEntry,
                        showDrawer = showDrawer,
                        canGoBack = canGoBack,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onBack = { navController.popBackStack() },
                        actions = topBarActionsContent,
                    )
                }
            }
        ) { innerPadding ->
            LevanaNavHost(
                navController = navController,
                deepLinkEpochDay = deepLinkEpochDay,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onRegisterTopBarActions = { actions -> topBarActionsContent = actions },
                onUnregisterTopBarActions = { topBarActionsContent = null },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
