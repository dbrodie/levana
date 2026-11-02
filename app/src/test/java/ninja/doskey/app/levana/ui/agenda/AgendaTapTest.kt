package ninja.doskey.app.levana.ui.agenda

import android.app.Application
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.domain.agenda.AgendaDay
import ninja.doskey.app.levana.domain.agenda.AgendaItem
import ninja.doskey.app.levana.domain.model.ContactBirthday
import ninja.doskey.app.levana.domain.model.Holiday
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.domain.model.PaneSplit
import ninja.doskey.app.levana.domain.model.SystemCalendarEvent
import ninja.doskey.app.levana.ui.panes.PaneLayout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * What a tap in the agenda does, with the day panel beside the list (two panes) and without it
 * (phone). Only a day's date rail changes the panel's day (TASK-59); every card opens what it
 * opens on a phone, and Today, Go to date and a search result only ask the list to scroll.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1000dp-h1400dp")
class AgendaTapTest(private val twoPanes: Boolean) {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val calendar = CalendarRepository()
    private val today = LocalDate.of(2026, 11, 17)
    private val friday = LocalDate.of(2026, 11, 20)
    private val shabbat = LocalDate.of(2026, 11, 21)

    private val intents = mutableListOf<AgendaIntent>()
    private val opened = mutableListOf<String>()
    private val navigation = AgendaNavigation(
        onEditEvent = { opened += "event $it" },
        onEditBirthday = { opened += "birthday $it" },
        onShowZmanim = { opened += "zmanim $it" },
        onShowDay = { opened += "month $it" },
    )

    private fun day(date: LocalDate, vararg items: AgendaItem) = AgendaDay(date, calendar.getHebrewDay(date), items.toList())

    private val days = listOf(
        day(
            friday,
            AgendaItem.HolidayItem(Holiday("Test holiday", "חג", HolidayCategory.MINOR)),
            AgendaItem.PersonalEventItem(PersonalEvent(id = 7, title = "Anniversary", hebrewDay = 10, hebrewMonth = JewishDate.KISLEV, hebrewYear = 5770), null),
            AgendaItem.BirthdayItem(ContactBirthday("sarah", "Sarah Cohen", null, 10, JewishDate.KISLEV, 5750), null),
            AgendaItem.CandleLighting(LocalTime.of(16, 13)),
            AgendaItem.DeviceEventItem(
                SystemCalendarEvent(3, "Dinner at the Levis", friday.atTime(19, 0), friday.atTime(21, 0), false, 4, 0xFF3F6FB5.toInt()),
                LocalTime.of(19, 0),
            ),
        ),
        // Havdalah is in the Shabbat card's text; a card of its own here keeps them apart
        day(shabbat, AgendaItem.Shabbat("Vayetzei", "ויצא", null, havdalah = null), AgendaItem.Havdalah(LocalTime.of(17, 20))),
    )

    private fun show(state: AgendaState) {
        compose.setContent {
            AgendaContent(
                state = state,
                onIntent = { intents += it },
                navigation = navigation,
                panes = if (twoPanes) PaneLayout(PaneSplit(), loaded = true, onIntent = {}) else null,
            )
        }
        compose.waitForIdle()
        // What the list asks for by itself (its top day, more days) is not a tap's
        intents.clear()
    }

    private fun showAgenda(selected: LocalDate? = null) = show(
        AgendaState(today = today, days = days, start = friday, end = shabbat, isLoading = false, selectedDate = selected)
    )

    private fun string(id: Int) = app.getString(id)

    private fun card(description: String) =
        compose.onNode(hasContentDescription(description, substring = true) and hasClickAction() and isRail().not())

    private fun rail(date: String) = compose.onNode(hasContentDescription(date, substring = true) and isRail())

    /** Date rails are headings; cards are not. */
    private fun isRail() = SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.Heading)

    private fun assertNoSelection() = assertEquals(emptyList<AgendaIntent>(), intents.filterIsInstance<AgendaIntent.SelectDay>())

    @Test
    fun aDateRailTapSelectsItsDayOnlyBesideThePanel() {
        showAgenda(selected = friday)
        rail("November 21, 2026").performClick()
        if (twoPanes) {
            assertEquals(listOf(AgendaIntent.SelectDay(shabbat)), intents)
            // The highlight follows the state's day
            compose.onNode(hasContentDescription("November 20, 2026", substring = true) and isRail() and isSelected()).assertExists()
        } else {
            // No panel: the rail is not a button and nothing is selected
            assertEquals(emptyList<AgendaIntent>(), intents)
            compose.onNode(hasContentDescription("November 20, 2026", substring = true) and isRail() and isSelected()).assertDoesNotExist()
        }
        assertEquals(emptyList<String>(), opened)
    }

    @Test
    fun aHolidayCardOpensTheMonthOnItsDay() {
        showAgenda(selected = shabbat)
        card("Test holiday").performClick()
        assertEquals(listOf("month $friday"), opened)
        assertNoSelection()
    }

    @Test
    fun candleLightingShabbatAndHavdalahCardsOpenZmanim() {
        showAgenda(selected = today)
        card(string(R.string.agenda_candle_lighting)).performClick()
        card("Vayetzei").performClick()
        compose.onNode(
            hasContentDescription(string(R.string.agenda_havdalah), substring = true) and hasClickAction() and isRail().not() and
                hasContentDescription("Vayetzei", substring = true).not()
        ).performClick()
        assertEquals(listOf("zmanim $friday", "zmanim $shabbat", "zmanim $shabbat"), opened)
        assertNoSelection()
    }

    @Test
    fun eventAndBirthdayCardsOpenTheirEditor() {
        showAgenda(selected = shabbat)
        card("Anniversary").performClick()
        card("Sarah Cohen").performClick()
        assertEquals(listOf("event 7", "birthday sarah"), opened)
        assertNoSelection()
    }

    @Test
    fun aDeviceEventCardOpensTheCalendarApp() {
        showAgenda(selected = shabbat)
        card("Dinner at the Levis").performClick()
        val started = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(CalendarContract.Events.CONTENT_URI.buildUpon().appendPath("3").build(), started.data)
        assertNoSelection()
    }

    @Test
    fun todayAndGoToDateOnlyAskForTheirIntents() {
        showAgenda(selected = shabbat)
        compose.onNode(hasContentDescription(string(R.string.calendar_today))).performClick()
        compose.onNode(hasContentDescription(string(R.string.calendar_go_to_date))).performClick()
        // The picker opens on the day at the top of the list
        assertEquals(listOf(AgendaIntent.GoToToday, AgendaIntent.OpenDatePicker(friday)), intents)
        assertEquals(emptyList<String>(), opened)
    }

    @Test
    fun aSearchResultOnlyShowsItsDayInTheList() {
        show(
            AgendaState(
                today = today, days = days, start = friday, end = shabbat, isLoading = false, selectedDate = shabbat,
                search = AgendaSearchState(query = "Sarah", results = days.take(1), isLoading = false),
            )
        )
        card("Sarah Cohen").performClick()
        assertEquals(listOf(AgendaIntent.SelectSearchResult(friday)), intents)
        assertEquals(emptyList<String>(), opened)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "twoPanes={0}")
        fun params() = listOf(arrayOf<Any>(true), arrayOf<Any>(false))
    }
}
