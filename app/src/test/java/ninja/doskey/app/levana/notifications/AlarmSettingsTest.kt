package ninja.doskey.app.levana.notifications

import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class AlarmSettingsTest {

    private val base = UserPreferences(currentLocation = Location.JERUSALEM)
    private val newYork = Location(40.7128, -74.0060, 0.0, "America/New_York", "New York")

    @Test
    fun rebuildsOnlyWhenAlarmSettingsChange() = runBlocking {
        val prefs = MutableStateFlow(base)
        var rebuilds = 0
        val job = launch { rebuildOnAlarmSettingsChange(prefs) { rebuilds++ } }
        suspend fun set(update: (UserPreferences) -> UserPreferences) {
            prefs.value = update(prefs.value)
            repeat(10) { yield() }
        }

        set { it }
        assertEquals("starting value", 0, rebuilds)

        // Unrelated changes, including the categories posted by the daily worker
        set { it.copy(calendarHebrewMode = true) }
        set { it.copy(dynamicHolidayTheme = false) }
        set { it.copy(notifyHolidays = true, holidayNotifyDaysBefore = 3) }
        set { it.copy(notifyFasts = true, notifyPersonalEvents = true) }
        set { it.copy(selectedZmanim = setOf("Sunrise")) }
        assertEquals("unrelated changes", 0, rebuilds)

        set { it.copy(notifyCandleLighting = true) }
        assertEquals(1, rebuilds)
        set { it.copy(candleLightingNotifyMode = "hours_before") }
        assertEquals(2, rebuilds)
        set { it.copy(candleLightingHoursBefore = 3) }
        assertEquals(3, rebuilds)
        set { it.copy(savedLocations = listOf(SavedLocation("ny", newYork))) }
        assertEquals("saving a location that is not active", 3, rebuilds)
        set { it.copy(activeLocationId = "ny") }
        assertEquals(4, rebuilds)
        set { it.copy(notifyOmer = true) }
        assertEquals(5, rebuilds)
        set { it.copy(notifyOmerMorningTime = 6 * 60) }
        assertEquals(6, rebuilds)
        set { it.copy(notifyCandleLighting = false) }
        assertEquals(7, rebuilds)

        job.cancel()
    }

    @Test
    fun currentLocationFixMattersOnlyWhenItIsActive() {
        val moved = base.copy(currentLocation = newYork)
        assertTrue(base.alarmSettings() != moved.alarmSettings())

        val savedActive = base.copy(
            savedLocations = listOf(SavedLocation("j", Location.JERUSALEM)),
            activeLocationId = "j"
        )
        assertEquals(
            savedActive.alarmSettings(),
            savedActive.copy(currentLocation = newYork).alarmSettings()
        )
    }

    @Test
    fun devDateOverrideAndIsraelAreAlarmSettings() {
        assertTrue(
            base.alarmSettings() != base.copy(devDateOverride = LocalDate.of(2030, 1, 4)).alarmSettings()
        )
        assertTrue(base.alarmSettings() != base.copy(isInIsrael = !base.isInIsrael).alarmSettings())
    }

    @Test
    fun alarmsAreDeliveredOnlyWhileTheirCategoryIsEnabled() {
        val candle = NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING
        val omer = NotificationAlarmScheduler.ACTION_OMER
        val omerMorning = NotificationAlarmScheduler.ACTION_OMER_MORNING

        assertFalse(isAlarmEnabled(candle, base))
        assertTrue(isAlarmEnabled(candle, base.copy(notifyCandleLighting = true)))

        val omerOn = base.copy(notifyOmer = true, notifyOmerTzait = true, notifyOmerMorning = true)
        assertTrue(isAlarmEnabled(omer, omerOn))
        assertTrue(isAlarmEnabled(omerMorning, omerOn))
        assertFalse(isAlarmEnabled(omer, omerOn.copy(notifyOmerTzait = false)))
        assertFalse(isAlarmEnabled(omerMorning, omerOn.copy(notifyOmerMorning = false)))
        assertFalse(isAlarmEnabled(omer, omerOn.copy(notifyOmer = false)))
        assertFalse(isAlarmEnabled(omerMorning, omerOn.copy(notifyOmer = false)))
        assertFalse(isAlarmEnabled("unknown", omerOn.copy(notifyCandleLighting = true)))
    }
}
