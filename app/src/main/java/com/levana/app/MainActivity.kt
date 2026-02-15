package com.levana.app

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.levana.app.ui.calendar.CalendarScreen
import com.levana.app.ui.daydetail.DayDetailScreen
import com.levana.app.ui.events.AddEditEventScreen
import com.levana.app.ui.events.EventsScreen
import com.levana.app.ui.location.CityPickerScreen
import com.levana.app.ui.location.ManualLocationScreen
import com.levana.app.ui.navigation.AddEditEventRoute
import com.levana.app.ui.navigation.CalendarRoute
import com.levana.app.ui.navigation.CityPickerRoute
import com.levana.app.ui.navigation.DayDetailRoute
import com.levana.app.ui.navigation.ManualLocationRoute
import com.levana.app.ui.navigation.OnboardingRoute
import com.levana.app.ui.navigation.PersonalEventsRoute
import com.levana.app.ui.navigation.SettingsRoute
import com.levana.app.ui.navigation.ZmanimRoute
import com.levana.app.ui.onboarding.OnboardingScreen
import com.levana.app.ui.settings.SettingsScreen
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
        setContent {
            val preferencesRepository: PreferencesRepository = koinInject()
            val prefs by preferencesRepository.preferences.collectAsState(
                initial = null
            )
            val holidayTheme = if (prefs?.dynamicHolidayTheme == true) {
                HolidayThemeResolver.resolve()
            } else {
                null
            }
            LevanaTheme(holidayTheme = holidayTheme) {
                LevanaApp()
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevanaApp() {
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

    val bottomNavItems = listOf(
        BottomNavItem("Calendar", Icons.Filled.CalendarMonth, CalendarRoute),
        BottomNavItem("Zmanim", Icons.Filled.WbSunny, ZmanimRoute()),
        BottomNavItem(
            "Settings",
            Icons.Filled.Settings,
            SettingsRoute
        )
    )

    val showBottomBar = hasLocation && backStackEntry?.destination?.let { dest ->
        bottomNavItems.any { item ->
            dest.hasRoute(item.route::class)
        }
    } == true

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    if (canGoBack && !showBottomBar) {
                        IconButton(
                            onClick = { navController.popBackStack() }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (hasLocation) {
                        Text(
                            text = locationName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme
                                .onSurfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    navController.navigate(CityPickerRoute)
                                }
                                .padding(end = 4.dp)
                        )
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Change location",
                            tint = MaterialTheme.colorScheme
                                .onSurfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    navController.navigate(CityPickerRoute)
                                }
                                .padding(end = 8.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected =
                            backStackEntry?.destination?.hasRoute(
                                item.route::class
                            ) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(
                                        navController.graph
                                            .findStartDestination().id
                                    ) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(item.icon, contentDescription = null)
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
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
                val scope = rememberCoroutineScope()
                ManualLocationScreen(
                    onSave = { location ->
                        scope.launch {
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
                    onDayClick = { date ->
                        navController.navigate(
                            DayDetailRoute(date.toEpochDay())
                        )
                    }
                )
            }

            composable<DayDetailRoute> { backStack ->
                val route = backStack.toRoute<DayDetailRoute>()
                DayDetailScreen(
                    dateEpochDay = route.dateEpochDay,
                    onShowZmanim = { date ->
                        navController.navigate(
                            ZmanimRoute(date.toEpochDay())
                        )
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
                    onPersonalEvents = {
                        navController.navigate(PersonalEventsRoute)
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
