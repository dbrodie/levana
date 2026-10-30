package ninja.doskey.app.levana.screenshots

import android.Manifest
import android.content.Intent
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.MainActivity
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.notifications.NotificationPoster
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The drawer, the top bar of each app route, and the routes outside Settings. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class AppShellScreenshotTest(variant: Variant) : ScreenshotTest(variant) {

    @Test
    fun calendarAndDrawer() {
        launchCalendar()
        capture("app_calendar")

        openDrawer()
        capture("app_drawer")

        compose.onNodeWithContentDescription(string(R.string.design_switch_city)).performClick()
        awaitText("London")
        capture("app_drawer_cities")
    }

    @Test
    fun personalEvents() {
        launchCalendar()
        openDrawer()
        clickable(string(R.string.design_my_events)).performClick()
        awaitText(string(R.string.events_tab_birthdays))
        capture("app_events_birthdays")

        // The Events tab registers the overflow action in the app's top bar
        clickable(string(R.string.events_tab_events)).performClick()
        await(hasContentDescription(string(R.string.events_more_options)))
        capture("app_events_events")
    }

    @Test
    fun addEvent() {
        launchCalendar()
        compose.onNodeWithContentDescription(string(R.string.design_add_event)).performClick()
        awaitText(string(R.string.design_add_event))
        capture("app_add_event")
    }

    @Test
    fun editEvent() {
        runBlocking {
            db.personalEventDao().insert(
                PersonalEvent(title = "Grandfather", hebrewDay = 12, hebrewMonth = 9, hebrewYear = 5760, useYahrzeitRules = true)
            )
        }
        launchCalendar()
        openDrawer()
        clickable(string(R.string.design_my_events)).performClick()
        awaitText(string(R.string.events_tab_events))
        clickable(string(R.string.events_tab_events)).performClick()
        awaitText("Grandfather")
        clickable("Grandfather").performClick()
        awaitText(string(R.string.design_edit_event))
        capture("app_edit_event")
    }

    @Test
    fun contactBirthday() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        launchCalendar()
        openDrawer()
        clickable(string(R.string.design_my_events)).performClick()
        awaitText(string(R.string.events_tab_birthdays))
        compose.onNodeWithContentDescription(string(R.string.events_add)).performClick()
        awaitText(string(R.string.design_add_birthday))
        // A new birthday starts on 1 Tishrei of the device's (TODAY's) Hebrew year, so the image never ages
        val tishrei1 = JewishDate(HebrewDateMatcher.hebrewYearOf(TODAY), JewishDate.TISHREI, 1).localDate
        awaitText(tishrei1.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(app.resources.configuration.locales[0])))
        capture("app_contact_birthday")
    }

    @Test
    fun zmanimFromDeepLink() {
        // The widget's zmanim rows open that day's Zmanim screen over the calendar
        launch(
            hasText(string(R.string.nav_zmanim)),
            Intent(app, MainActivity::class.java)
                .putExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, TODAY.toEpochDay())
                .putExtra(MainActivity.EXTRA_OPEN_ZMANIM, true)
        )
        capture("app_zmanim")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = variants()
    }
}
