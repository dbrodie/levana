package ninja.doskey.app.levana.ui.navigation

import ninja.doskey.app.levana.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TopBarTitlesTest {

    @Test
    fun `each route has its title`() {
        val expected = mapOf(
            ZmanimRoute::class to R.string.nav_zmanim,
            HalachicTimesSettingsRoute::class to R.string.design_halachic_times,
            DeveloperSettingsRoute::class to R.string.design_developer_settings,
            AboutRoute::class to R.string.design_about,
            SettingsRoute::class to R.string.nav_settings,
            PersonalEventsRoute::class to R.string.design_my_events,
            CityPickerRoute::class to R.string.design_cities,
            LocationsSettingsRoute::class to R.string.design_cities,
            CalendarSelectionRoute::class to R.string.design_system_calendars,
            AddEditEventRoute::class to R.string.design_add_event,
            ContactBirthdayRoute::class to R.string.design_birthday,
        )
        expected.forEach { (route, title) -> assertEquals(route.simpleName, title, topBarTitleFor(route)) }
        // Every titled route is matched against the destination
        assertEquals(expected.keys, TitledRoutes.toSet())
    }

    @Test
    fun `the event editor is Add for a new event and Edit for a saved one`() {
        assertEquals(R.string.design_add_event, topBarTitleFor(AddEditEventRoute::class, eventId = 0L))
        assertEquals(R.string.design_edit_event, topBarTitleFor(AddEditEventRoute::class, eventId = 42L))
    }

    @Test
    fun `the calendar and an unknown destination show the app name`() {
        assertEquals(R.string.app_name, topBarTitleFor(CalendarRoute::class))
        assertEquals(R.string.app_name, topBarTitleFor(null))
        assertNotEquals(R.string.app_name, topBarTitleFor(SettingsRoute::class))
    }
}
