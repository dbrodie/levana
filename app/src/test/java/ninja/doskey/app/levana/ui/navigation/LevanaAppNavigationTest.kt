package ninja.doskey.app.levana.ui.navigation

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.MainActivity
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.notifications.NotificationPoster
import ninja.doskey.app.levana.screenshots.ScreenshotTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The app shell's drawer and top-bar navigation, on the real [MainActivity] through the
 * screenshot harness (no images are captured).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = ScreenshotTest.DEVICE)
class LevanaAppNavigationTest : ScreenshotTest(Variant.EN_LIGHT) {

    private val menu get() = hasContentDescription(string(R.string.design_open_menu))
    private val back get() = hasContentDescription(string(R.string.back))

    @Test
    fun drawerItemsNavigateToEachDestination() {
        launchCalendar()

        openDrawer()
        clickable(string(R.string.design_my_events)).performClick()
        awaitRoute(R.string.design_my_events)

        // The Calendar section: Agenda, then back to Month
        openDrawer()
        clickable(string(R.string.nav_agenda)).performClick()
        awaitDrawerClosed()
        await(hasContentDescription(string(R.string.agenda_search)))

        openDrawer()
        clickable(string(R.string.nav_month)).performClick()
        awaitDrawerClosed()
        awaitGone(hasContentDescription(string(R.string.agenda_search)))
        await(hasContentDescription(string(R.string.design_add_event)))

        openDrawer()
        clickable(string(R.string.nav_settings)).performClick()
        awaitRoute(R.string.nav_settings)
        compose.onNode(back).performClick()
        await(hasContentDescription(string(R.string.design_add_event)))

        openDrawer()
        clickable(string(R.string.design_manage_cities)).performClick()
        awaitRoute(R.string.design_cities)
        await(hasText("London") and onScreen())
    }

    @Test
    fun cityTapSelectsItAndClosesTheDrawer() {
        launchCalendar()
        openDrawer()
        compose.onNodeWithContentDescription(string(R.string.design_switch_city)).performClick()
        awaitText("London")
        clickable("London").performClick()
        awaitDrawerClosed()

        val prefs = runBlocking { preferences.preferences.first() }
        assertEquals(prefs.savedLocations.first { it.location.name == "London" }.id, prefs.activeLocationId)
        // Reopened, the picker is collapsed and shows the new city
        openDrawer()
        awaitText("London")
        awaitGone(hasText("Jerusalem") and onScreen())
    }

    @Test
    fun backArrowShowsOffTheDrawerRoutes() {
        launchCalendar()
        openDrawer()
        clickable(string(R.string.design_my_events)).performClick()
        awaitRoute(R.string.design_my_events)
        await(menu)
        awaitGone(back)

        openDrawer()
        clickable(string(R.string.nav_settings)).performClick()
        awaitRoute(R.string.nav_settings)
        await(back)
        awaitGone(menu)

        clickable(string(R.string.settings_halachic_times_title)).performScrollTo().performClick()
        awaitRoute(R.string.design_halachic_times)
        compose.onNode(back).performClick()
        awaitRoute(R.string.nav_settings)
    }

    @Test
    fun deepLinkedZmanimGoesBackToTheCalendar() {
        launch(
            hasText(string(R.string.nav_zmanim)),
            Intent(app, MainActivity::class.java)
                .putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, TODAY.toEpochDay())
                .putExtra(MainActivity.EXTRA_OPEN_ZMANIM, true)
        )
        await(back)
        awaitGone(menu)
        compose.onNode(back).performClick()
        await(hasContentDescription(string(R.string.design_add_event)))
        awaitGone(hasText(string(R.string.nav_zmanim)) and onScreen())
    }

    private fun awaitDrawerClosed() = awaitGone(hasText(string(R.string.design_manage_cities)) and onScreen())

    /** The drawer has closed and the top bar shows [title]. */
    private fun awaitRoute(title: Int) {
        awaitDrawerClosed()
        awaitText(string(title))
    }
}
