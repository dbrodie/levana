package ninja.doskey.app.levana.notifications

import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.Minhag
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.YahrzeitFirstYear
import ninja.doskey.app.levana.domain.model.YahrzeitRules
import ninja.doskey.app.levana.notifications.DailyCategory.FASTS
import ninja.doskey.app.levana.notifications.DailyCategory.HOLIDAYS
import ninja.doskey.app.levana.notifications.DailyCategory.PERSONAL_EVENTS
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class DailyCategorySettingsTest {

    private val newYork = Location(40.7128, -74.0060, 0.0, "America/New_York", "New York")

    @Test
    fun refreshesOnlyTheEnabledCategoriesWhoseSettingsChanged() = runBlocking {
        val prefs = MutableStateFlow(UserPreferences(currentLocation = Location.JERUSALEM))
        val refreshed = mutableListOf<Set<DailyCategory>>()
        val disabled = mutableListOf<Set<DailyCategory>>()
        val job = launch {
            refreshOnDailySettingsChange(prefs, { refreshed += it }, { disabled += it })
        }
        suspend fun set(update: (UserPreferences) -> UserPreferences): List<Set<DailyCategory>> {
            val before = refreshed.size
            prefs.value = update(prefs.value)
            repeat(10) { yield() }
            return refreshed.drop(before)
        }

        assertEquals("starting value", emptyList<Set<DailyCategory>>(), set { it })

        // Settings of a category that is off don't matter yet
        assertEquals(emptyList<Set<DailyCategory>>(), set { it.copy(holidayNotifyDaysBefore = 2) })

        assertEquals(listOf(setOf(HOLIDAYS)), set { it.copy(notifyHolidays = true) })
        assertEquals(listOf(setOf(FASTS)), set { it.copy(notifyFasts = true) })
        assertEquals(listOf(setOf(PERSONAL_EVENTS)), set { it.copy(notifyPersonalEvents = true) })
        assertEquals(listOf(setOf(HOLIDAYS)), set { it.copy(holidayNotifyDaysBefore = 3) })
        assertEquals(listOf(setOf(HOLIDAYS, FASTS)), set { it.copy(isInIsrael = true) })
        assertEquals(
            "active location",
            listOf(setOf(FASTS)),
            set {
                it.copy(savedLocations = listOf(SavedLocation("ny", newYork)), activeLocationId = "ny")
            }
        )
        assertEquals(
            listOf(setOf(PERSONAL_EVENTS)),
            set { it.copy(yahrzeitRules = YahrzeitRules(firstYear = YahrzeitFirstYear.entries.last())) }
        )
        assertEquals(
            listOf(setOf(HOLIDAYS, FASTS, PERSONAL_EVENTS)),
            set { it.copy(devDateOverride = LocalDate.of(2030, 9, 1)) }
        )

        // Unrelated settings, including the alarm-backed categories
        val unrelated = set {
            it.copy(
                minhag = Minhag.SEPHARDI,
                dynamicHolidayTheme = false,
                calendarHebrewMode = true,
                selectedZmanim = setOf("Sunrise"),
                notifyCandleLighting = true,
                candleLightingHoursBefore = 5,
                notifyOmer = true,
                notifyOmerMorning = true
            )
        }
        assertEquals(emptyList<Set<DailyCategory>>(), unrelated)

        // Switching off reports the category as disabled and never refreshes it
        assertEquals(emptyList<Set<DailyCategory>>(), set { it.copy(notifyFasts = false) })
        assertEquals(listOf(setOf(FASTS)), disabled)
        assertEquals(
            "location no longer matters with fasts off",
            emptyList<Set<DailyCategory>>(),
            set { it.copy(activeLocationId = null) }
        )

        job.cancel()
    }
}
