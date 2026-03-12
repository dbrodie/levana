package com.levana.app

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.levana.app.data.LocationService
import com.levana.app.data.PreferencesRepository
import com.levana.app.notifications.NotificationPoster
import com.levana.app.ui.birthday.ContactBirthdayScreen
import com.levana.app.ui.calendar.CalendarScreen
import com.levana.app.ui.calendarselection.CalendarSelectionScreen
import com.levana.app.ui.events.AddEditEventScreen
import com.levana.app.ui.events.EventsScreen
import com.levana.app.ui.location.CityPickerScreen
import com.levana.app.ui.location.ManualLocationScreen
import com.levana.app.ui.navigation.AddEditEventRoute
import com.levana.app.ui.navigation.CalendarRoute
import com.levana.app.ui.navigation.CalendarSelectionRoute
import com.levana.app.ui.navigation.CityPickerRoute
import com.levana.app.ui.navigation.ContactBirthdayRoute
import com.levana.app.ui.navigation.ManualLocationRoute
import com.levana.app.ui.navigation.OnboardingRoute
import com.levana.app.ui.navigation.PersonalEventsRoute
import com.levana.app.ui.navigation.HalachicTimesSettingsRoute
import com.levana.app.ui.navigation.SettingsRoute
import com.levana.app.ui.navigation.ZmanimRoute
import com.levana.app.ui.onboarding.OnboardingScreen
import com.levana.app.ui.settings.HalachicTimesSettingsScreen
import com.levana.app.ui.settings.SettingsScreen
import com.levana.app.ui.theme.HolidayTheme
import com.levana.app.ui.theme.HolidayThemeResolver
import com.levana.app.ui.theme.LevanaTheme
import com.levana.app.ui.zmanim.ZmanimScreen
import java.time.LocalDate
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkEpochDay = intent?.getLongExtra(
            NotificationPoster.EXTRA_DATE_EPOCH_DAY,
            0L
        ) ?: 0L
        setContent {
            val preferencesRepository: PreferencesRepository = koinInject()
            val prefs by preferencesRepository.preferences.collectAsState(
                initial = null
            )
            val forceThemeName = prefs?.devForceHolidayTheme
            val forceTheme = forceThemeName?.let {
                try {
                    HolidayTheme.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            val holidayTheme = forceTheme ?: if (prefs?.dynamicHolidayTheme == true) {
                val themeDate = prefs?.devDateOverride ?: LocalDate.now()
                HolidayThemeResolver.resolve(themeDate)
            } else {
                null
            }
            val configuration = LocalConfiguration.current
            val layoutDirection = if (configuration.layoutDirection == android.util.LayoutDirection.RTL) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LevanaTheme(holidayTheme = holidayTheme) {
                    LevanaApp(deepLinkEpochDay = deepLinkEpochDay)
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
fun LevanaApp(deepLinkEpochDay: Long = 0L) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val canGoBack = backStackEntry?.destination?.route != null &&
        navController.previousBackStackEntry != null

    val preferencesRepository: PreferencesRepository = koinInject()
    val prefs by preferencesRepository.preferences.collectAsState(
        initial = null
    )

    val hasLocation = prefs?.location != null
    val locationName = prefs?.location?.name ?: ""

    if (prefs == null) return

    val startDest: Any = if (hasLocation) CalendarRoute else OnboardingRoute

    // Handle notification deep-link — open calendar (day detail is inline)
    LaunchedEffect(deepLinkEpochDay) {
        if (deepLinkEpochDay != 0L && hasLocation) {
            navController.navigate(CalendarRoute) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val drawerNavItems = listOf(
        DrawerNavItem("Calendar", Icons.Filled.CalendarMonth, CalendarRoute),
        DrawerNavItem("Events", Icons.Filled.Event, PersonalEventsRoute)
    )

    val showDrawer = hasLocation && backStackEntry?.destination?.let { dest ->
        drawerNavItems.any { item -> dest.hasRoute(item.route::class) }
    } == true

    val isZmanimRoute =
        backStackEntry?.destination?.hasRoute(ZmanimRoute::class) == true

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Calendar",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    drawerNavItems.forEach { item ->
                        val selected =
                            backStackEntry?.destination?.hasRoute(item.route::class) == true
                        NavigationDrawerItem(
                            label = { Text(item.label) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            selected = selected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(item.route) {
                                    popUpTo(
                                        navController.graph.findStartDestination().id
                                    ) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    if (hasLocation) {
                        Text(
                            text = "LOCATIONS",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                        )
                        NavigationDrawerItem(
                            label = { Text(locationName) },
                            icon = {
                                Icon(Icons.Filled.LocationOn, contentDescription = null)
                            },
                            selected = true,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(CityPickerRoute)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        selected = backStackEntry?.destination?.hasRoute(SettingsRoute::class) == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(SettingsRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        val isCalendarRoute =
            backStackEntry?.destination?.hasRoute(CalendarRoute::class) == true

        Scaffold(
            topBar = {
                if (!isCalendarRoute) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                if (isZmanimRoute) "Halachic Times"
                                else stringResource(R.string.app_name)
                            )
                        },
                        navigationIcon = {
                            if (showDrawer) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } }
                                ) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                                }
                            } else if (canGoBack) {
                                IconButton(
                                    onClick = { navController.popBackStack() }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDest,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<OnboardingRoute> {
                    GpsOnboarding(
                        navController = navController,
                        preferencesRepository = preferencesRepository
                    )
                }

                composable<CityPickerRoute> {
                    CityPickerScreen(
                        onLocationSaved = {
                            navController.navigate(CalendarRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable<ManualLocationRoute> {
                    val scope2 = rememberCoroutineScope()
                    ManualLocationScreen(
                        onSave = { location ->
                            scope2.launch {
                                preferencesRepository.saveLocation(location)
                                navController.navigate(CalendarRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
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
                            navController.navigate(CityPickerRoute)
                        },
                        onSystemCalendars = {
                            navController.navigate(CalendarSelectionRoute)
                        },
                        onHalachicTimesSettings = {
                            navController.navigate(HalachicTimesSettingsRoute)
                        }
                    )
                }

                composable<HalachicTimesSettingsRoute> {
                    HalachicTimesSettingsScreen()
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

@Composable
private fun GpsOnboarding(
    navController: androidx.navigation.NavController,
    preferencesRepository: PreferencesRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService: LocationService = koinInject()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            scope.launch {
                try {
                    val loc = locationService.getCurrentLocation()
                    preferencesRepository.saveLocation(loc)
                    navController.navigate(CalendarRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        "Could not detect location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                context,
                "Location permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    OnboardingScreen(
        onPickCity = { navController.navigate(CityPickerRoute) },
        onUseGps = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        onManualEntry = { navController.navigate(ManualLocationRoute) }
    )
}
