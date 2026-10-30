package ninja.doskey.app.levana.ui.navigation

import androidx.annotation.StringRes
import kotlin.reflect.KClass
import ninja.doskey.app.levana.R

/** The routes with their own top-bar title, matched against the current destination. */
internal val TitledRoutes: List<KClass<*>> = listOf(
    ZmanimRoute::class,
    HalachicTimesSettingsRoute::class,
    DeveloperSettingsRoute::class,
    AboutRoute::class,
    SettingsRoute::class,
    PersonalEventsRoute::class,
    CityPickerRoute::class,
    LocationsSettingsRoute::class,
    CalendarSelectionRoute::class,
    AddEditEventRoute::class,
    ContactBirthdayRoute::class,
)

/**
 * The top-bar title for [route]; [eventId] is the [AddEditEventRoute] argument, non-zero when
 * editing, and [birthdayEditing] whether the birthday editor edits a stored birthday. Other
 * routes (the calendar has its own header) show the app name.
 */
@StringRes
internal fun topBarTitleFor(route: KClass<*>?, eventId: Long = 0L, birthdayEditing: Boolean = false): Int = when (route) {
    ZmanimRoute::class -> R.string.nav_zmanim
    HalachicTimesSettingsRoute::class -> R.string.design_halachic_times
    DeveloperSettingsRoute::class -> R.string.design_developer_settings
    AboutRoute::class -> R.string.design_about
    SettingsRoute::class -> R.string.nav_settings
    PersonalEventsRoute::class -> R.string.design_my_events
    CityPickerRoute::class -> R.string.design_cities
    LocationsSettingsRoute::class -> R.string.design_cities
    CalendarSelectionRoute::class -> R.string.design_system_calendars
    AddEditEventRoute::class -> if (eventId != 0L) R.string.design_edit_event else R.string.design_add_event
    ContactBirthdayRoute::class -> if (birthdayEditing) R.string.design_edit_birthday else R.string.design_add_birthday
    else -> R.string.app_name
}
