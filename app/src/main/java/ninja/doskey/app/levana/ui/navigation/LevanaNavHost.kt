package ninja.doskey.app.levana.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayScreen
import ninja.doskey.app.levana.ui.calendar.CalendarScreen
import ninja.doskey.app.levana.ui.calendarselection.CalendarSelectionScreen
import ninja.doskey.app.levana.ui.events.AddEditEventScreen
import ninja.doskey.app.levana.ui.events.EventsScreen
import ninja.doskey.app.levana.ui.location.CityPickerScreen
import ninja.doskey.app.levana.ui.settings.AboutScreen
import ninja.doskey.app.levana.ui.settings.DeveloperSettingsScreen
import ninja.doskey.app.levana.ui.settings.HalachicTimesSettingsScreen
import ninja.doskey.app.levana.ui.settings.LocationsSettingsScreen
import ninja.doskey.app.levana.ui.settings.SettingsScreen
import ninja.doskey.app.levana.ui.zmanim.ZmanimScreen
import java.time.LocalDate

/**
 * Every app route. [deepLinkEpochDay] opens the calendar on that day; My Events registers its
 * top-bar actions through [onRegisterTopBarActions].
 */
@Composable
internal fun LevanaNavHost(
    navController: NavHostController,
    deepLinkEpochDay: Long,
    onOpenDrawer: () -> Unit,
    onRegisterTopBarActions: (@Composable () -> Unit) -> Unit,
    onUnregisterTopBarActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = CalendarRoute,
        modifier = modifier,
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
                initialDate = deepLinkEpochDay.takeIf { it != 0L }?.let(LocalDate::ofEpochDay),
                onOpenDrawer = onOpenDrawer,
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
                onRegisterTopBarActions = onRegisterTopBarActions,
                onUnregisterTopBarActions = onUnregisterTopBarActions
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
