package ninja.doskey.app.levana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ninja.doskey.app.levana.data.LocationService
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.icon.DynamicIconManager
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.displayCity
import ninja.doskey.app.levana.notifications.NotificationPoster
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayScreen
import ninja.doskey.app.levana.ui.calendar.CalendarScreen
import ninja.doskey.app.levana.ui.calendarselection.CalendarSelectionScreen
import ninja.doskey.app.levana.ui.events.AddEditEventScreen
import ninja.doskey.app.levana.ui.events.EventsScreen
import ninja.doskey.app.levana.ui.location.CityPickerScreen
import ninja.doskey.app.levana.ui.navigation.AddEditEventRoute
import ninja.doskey.app.levana.ui.navigation.CalendarRoute
import ninja.doskey.app.levana.ui.navigation.CalendarSelectionRoute
import ninja.doskey.app.levana.ui.navigation.CityPickerRoute
import ninja.doskey.app.levana.ui.navigation.ContactBirthdayRoute
import ninja.doskey.app.levana.ui.navigation.LocationsSettingsRoute
import ninja.doskey.app.levana.ui.navigation.PersonalEventsRoute
import ninja.doskey.app.levana.ui.navigation.AboutRoute
import ninja.doskey.app.levana.ui.navigation.DeveloperSettingsRoute
import ninja.doskey.app.levana.ui.navigation.HalachicTimesSettingsRoute
import ninja.doskey.app.levana.ui.navigation.SettingsRoute
import ninja.doskey.app.levana.ui.navigation.ZmanimRoute
import ninja.doskey.app.levana.ui.settings.AboutScreen
import ninja.doskey.app.levana.ui.settings.DeveloperSettingsScreen
import ninja.doskey.app.levana.ui.settings.HalachicTimesSettingsScreen
import ninja.doskey.app.levana.ui.settings.LocationsSettingsScreen
import ninja.doskey.app.levana.ui.settings.SettingsScreen
import ninja.doskey.app.levana.ui.theme.activeHolidayTheme
import ninja.doskey.app.levana.ui.theme.LevanaTheme
import ninja.doskey.app.levana.ui.zmanim.ZmanimScreen
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    companion object {
        /** With [NotificationPoster.EXTRA_DATE_EPOCH_DAY], opens that day's Zmanim screen. */
        const val EXTRA_OPEN_ZMANIM = "EXTRA_OPEN_ZMANIM"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkEpochDay = intent?.getLongExtra(
            NotificationPoster.EXTRA_DATE_EPOCH_DAY,
            0L
        ) ?: 0L
        val openZmanim = intent?.getBooleanExtra(EXTRA_OPEN_ZMANIM, false) ?: false
        setContent {
            val preferencesRepository: PreferencesRepository = koinInject()
            val deviceContext: DeviceContext = koinInject()
            val prefs by preferencesRepository.preferences.collectAsState(
                initial = null
            )
            val holidayTheme = prefs?.let {
                activeHolidayTheme(it, deviceContext.now(it).today)
            }
            val configuration = LocalConfiguration.current
            val layoutDirection = if (configuration.layoutDirection == android.util.LayoutDirection.RTL) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LevanaTheme(holidayTheme = holidayTheme) {
                    LevanaApp(deepLinkEpochDay = deepLinkEpochDay, openZmanim = openZmanim)
                }
            }
        }
    }
}

private data class DrawerNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevanaApp(deepLinkEpochDay: Long = 0L, openZmanim: Boolean = false) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val canGoBack = backStackEntry?.destination?.route != null &&
        navController.previousBackStackEntry != null

    val preferencesRepository: PreferencesRepository = koinInject()
    val locationService: LocationService = koinInject()
    val deviceContext: DeviceContext = koinInject()
    val prefs by preferencesRepository.preferences.collectAsState(
        initial = null
    )

    val currentPrefs = prefs ?: return
    // The drawer's selected-city picker; only the Zmanim screen and day panel use the city
    val locationName = currentPrefs.displayCity.name

    // Ask for location once per launch; the dialog pauses the activity, so a grant is
    // picked up by the ON_RESUME refresh below.
    var askedForLocation by rememberSaveable { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    LaunchedEffect(Unit) {
        if (!askedForLocation && !locationService.hasPermission()) {
            askedForLocation = true
            locationPermissionLauncher.launch(LocationService.PERMISSIONS)
        }
    }

    // Current-location refresh on resume
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    locationService.getCurrentLocationOrNull()
                        ?.let { preferencesRepository.updateCurrentLocation(it) }
                }
                scope.launch(Dispatchers.IO) {
                    DynamicIconManager.update(context, deviceContext.today())
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle notification deep-link — open calendar (day detail is inline)
    LaunchedEffect(deepLinkEpochDay) {
        if (deepLinkEpochDay != 0L) {
            navController.navigate(CalendarRoute) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            // The home-screen widget's zmanim rows open that day's Zmanim screen over it
            if (openZmanim) navController.navigate(ZmanimRoute(deepLinkEpochDay))
        }
    }

    val drawerNavItems = listOf(
        DrawerNavItem(stringResource(R.string.nav_calendar), Icons.Filled.CalendarMonth, CalendarRoute),
        DrawerNavItem(stringResource(R.string.design_my_events), Icons.Filled.Event, PersonalEventsRoute)
    )

    val showDrawer = backStackEntry?.destination?.let { dest ->
        drawerNavItems.any { item -> dest.hasRoute(item.route::class) }
    } == true

    val isZmanimRoute =
        backStackEntry?.destination?.hasRoute(ZmanimRoute::class) == true

    val isHalachicTimesSettingsRoute =
        backStackEntry?.destination?.hasRoute(HalachicTimesSettingsRoute::class) == true

    val isDeveloperSettingsRoute =
        backStackEntry?.destination?.hasRoute(DeveloperSettingsRoute::class) == true

    val isAboutRoute =
        backStackEntry?.destination?.hasRoute(AboutRoute::class) == true

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Slot for per-screen actions rendered in the global TopAppBar
    var topBarActionsContent by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer,
        drawerContent = {
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
                                    preferencesRepository.setActiveLocation(id)
                                    citiesExpanded = false
                                    drawerState.close()
                                }
                            }
                            NavigationDrawerItem(
                                label = { Text(currentPrefs.currentLocation.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                icon = { Icon(Icons.Filled.GpsFixed, contentDescription = null) },
                                selected = currentPrefs.savedLocations.none { it.id == currentPrefs.activeLocationId },
                                onClick = { select(null) },
                            )
                            currentPrefs.savedLocations.forEach { saved ->
                                NavigationDrawerItem(
                                    label = { Text(saved.location.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                    icon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                                    selected = saved.id == currentPrefs.activeLocationId,
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
                            navController.navigate(LocationsSettingsRoute) { launchSingleTop = true }
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant)
                    drawerNavItems.forEach { item ->
                        val selected = backStackEntry?.destination?.hasRoute(item.route::class) == true
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
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_settings)) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(22.dp)) },
                        selected = backStackEntry?.destination?.hasRoute(SettingsRoute::class) == true,
                        modifier = Modifier.padding(vertical = 2.dp),
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(SettingsRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
        }
    ) {
        val isCalendarRoute =
            backStackEntry?.destination?.hasRoute(CalendarRoute::class) == true

        Scaffold(
            topBar = {
                if (!isCalendarRoute) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        title = {
                            val destination = backStackEntry?.destination
                            val title = when {
                                isZmanimRoute -> stringResource(R.string.nav_zmanim)
                                isHalachicTimesSettingsRoute -> stringResource(R.string.design_halachic_times)
                                isDeveloperSettingsRoute -> stringResource(R.string.design_developer_settings)
                                isAboutRoute -> stringResource(R.string.design_about)
                                destination?.hasRoute(SettingsRoute::class) == true -> stringResource(R.string.nav_settings)
                                destination?.hasRoute(PersonalEventsRoute::class) == true -> stringResource(R.string.design_my_events)
                                destination?.hasRoute(CityPickerRoute::class) == true -> stringResource(R.string.design_cities)
                                destination?.hasRoute(LocationsSettingsRoute::class) == true -> stringResource(R.string.design_cities)
                                destination?.hasRoute(CalendarSelectionRoute::class) == true -> stringResource(R.string.design_system_calendars)
                                destination?.hasRoute(AddEditEventRoute::class) == true -> {
                                    if ((backStackEntry?.toRoute<AddEditEventRoute>()?.eventId ?: 0L) != 0L)
                                        stringResource(R.string.design_edit_event) else stringResource(R.string.design_add_event)
                                }
                                destination?.hasRoute(ContactBirthdayRoute::class) == true -> stringResource(R.string.design_birthday)
                                else -> stringResource(R.string.app_name)
                            }
                            Text(title, style = MaterialTheme.typography.titleLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        navigationIcon = {
                            if (showDrawer) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } }
                                ) {
                                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.design_open_menu))
                                }
                            } else if (canGoBack) {
                                IconButton(
                                    onClick = { navController.popBackStack() }
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
                                topBarActionsContent?.invoke()
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = CalendarRoute,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(150)) },
            ) {
                composable<CityPickerRoute> {
                    CityPickerScreen(onLocationSaved = { navController.popBackStack() })
                }

                composable<CalendarRoute> {
                    CalendarScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onShowZmanim = { date ->
                            navController.navigate(ZmanimRoute(date.toEpochDay()))
                        },
                        onAddEvent = { day, month, year ->
                            navController.navigate(
                                AddEditEventRoute(
                                    prefillDay = day,
                                    prefillMonth = month,
                                    prefillYear = year
                                )
                            )
                        }
                    )
                }

                composable<ZmanimRoute> { backStack ->
                    val route = backStack.toRoute<ZmanimRoute>()
                    val initialDate = if (route.dateEpochDay != 0L) {
                        LocalDate.ofEpochDay(route.dateEpochDay)
                    } else {
                        null
                    }
                    ZmanimScreen(initialDate = initialDate)
                }

                composable<SettingsRoute> {
                    SettingsScreen(
                        onChangeLocation = {
                            navController.navigate(LocationsSettingsRoute)
                        },
                        onSystemCalendars = {
                            navController.navigate(CalendarSelectionRoute)
                        },
                        onHalachicTimesSettings = {
                            navController.navigate(HalachicTimesSettingsRoute)
                        },
                        onDeveloperSettings = {
                            navController.navigate(DeveloperSettingsRoute)
                        },
                        onAbout = {
                            navController.navigate(AboutRoute)
                        }
                    )
                }

                composable<HalachicTimesSettingsRoute> {
                    HalachicTimesSettingsScreen()
                }

                composable<DeveloperSettingsRoute> {
                    DeveloperSettingsScreen()
                }

                composable<AboutRoute> {
                    AboutScreen()
                }

                composable<LocationsSettingsRoute> {
                    LocationsSettingsScreen(
                        onAddLocation = {
                            navController.navigate(CityPickerRoute)
                        }
                    )
                }

                composable<PersonalEventsRoute> {
                    EventsScreen(
                        onAddEvent = {
                            navController.navigate(AddEditEventRoute())
                        },
                        onEditEvent = { eventId ->
                            navController.navigate(
                                AddEditEventRoute(eventId = eventId)
                            )
                        },
                        onAddBirthday = {
                            navController.navigate(ContactBirthdayRoute())
                        },
                        onEditBirthday = { lookupKey ->
                            navController.navigate(
                                ContactBirthdayRoute(
                                    contactLookupKey = lookupKey
                                )
                            )
                        },
                        onRegisterTopBarActions = { actions ->
                            topBarActionsContent = actions
                        },
                        onUnregisterTopBarActions = {
                            topBarActionsContent = null
                        }
                    )
                }

                composable<AddEditEventRoute> { backStack ->
                    val route = backStack.toRoute<AddEditEventRoute>()
                    AddEditEventScreen(
                        eventId = route.eventId,
                        prefillDay = route.prefillDay,
                        prefillMonth = route.prefillMonth,
                        prefillYear = route.prefillYear,
                        onSaved = {
                            navController.popBackStack()
                        }
                    )
                }

                composable<CalendarSelectionRoute> {
                    CalendarSelectionScreen()
                }

                composable<ContactBirthdayRoute> { backStack ->
                    val route =
                        backStack.toRoute<ContactBirthdayRoute>()
                    ContactBirthdayScreen(
                        contactLookupKey = route.contactLookupKey,
                        onSaved = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
