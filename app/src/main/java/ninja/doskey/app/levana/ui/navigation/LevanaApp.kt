package ninja.doskey.app.levana.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.launch
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.ui.walkthrough.WalkthroughIntent
import ninja.doskey.app.levana.ui.walkthrough.WalkthroughScreen
import ninja.doskey.app.levana.ui.walkthrough.activityWalkthroughViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * The app shell: drawer, top bar and navigation, shown once the preferences have loaded.
 * [deepLinkEpochDay] (non-zero) opens the calendar on that day, and [openZmanim] that day's
 * Zmanim screen over it.
 */
@Composable
fun LevanaApp(deepLinkEpochDay: Long = 0L, openZmanim: Boolean = false) {
    // Before any early return, each under its own saved-state key (TASK-50)
    val navController = rememberLevanaNavController()
    val drawerState = rememberLevanaDrawerState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val canGoBack = backStackEntry?.destination?.route != null &&
        navController.previousBackStackEntry != null

    val preferencesRepository: PreferencesRepository = koinInject()
    // Kept across recreations: a recreated shell shows at once, without an empty frame
    val shell: ShellViewModel = koinViewModel()
    val prefs by shell.preferences.collectAsStateWithLifecycle()

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
        // Month and Agenda draw their own header
        val ownsHeader = backStackEntry?.destination?.let {
            it.hasRoute(CalendarRoute::class) || it.hasRoute(AgendaRoute::class)
        } == true

        Scaffold(
            topBar = {
                if (!ownsHeader) {
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
