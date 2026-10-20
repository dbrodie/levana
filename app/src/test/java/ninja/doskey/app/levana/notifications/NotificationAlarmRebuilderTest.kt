package ninja.doskey.app.levana.notifications

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NotificationAlarmRebuilderTest {

    private val app: Context get() = RuntimeEnvironment.getApplication()
    private val zmanim = ZmanimRepository()
    private val calendar = CalendarRepository()

    private val newYork = Location(40.7128, -74.0060, 0.0, "America/New_York", "New York")

    // Future dates so every alarm is still ahead of the real clock
    private val friday: LocalDate = generateSequence(LocalDate.of(2030, 1, 1)) { it.plusDays(1) }
        .first { it.dayOfWeek == DayOfWeek.FRIDAY && zmanim.hasCandles(it) }
    private val omerDate: LocalDate = generateSequence(LocalDate.of(2030, 3, 1)) { it.plusDays(1) }
        .first { it.dayOfWeek != DayOfWeek.FRIDAY && calendar.getDayInfo(it, inIsrael = false).omerDay != null }

    private val prefs = MutableStateFlow(
        UserPreferences(
            currentLocation = Location.JERUSALEM,
            devDateOverride = friday,
            candleLightingNotifyMode = "hours_before",
            candleLightingHoursBefore = 2
        )
    )

    private val rebuilder = NotificationAlarmRebuilder(
        prefs, zmanim, calendar, NotificationAlarmScheduler(app)
    )

    private fun rebuild(update: (UserPreferences) -> UserPreferences) = runBlocking {
        prefs.value = update(prefs.value)
        rebuilder.rebuild()
    }

    private fun alarms(): List<ScheduledAlarm> =
        shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms

    private fun ScheduledAlarm.action(): String? = shadowOf(operation).savedIntent.action

    private fun millis(date: LocalDate, time: LocalTime, location: Location): Long =
        ZonedDateTime.of(date, time, ZoneId.of(location.timezoneId)).toInstant().toEpochMilli()

    private fun candleLighting(location: Location): LocalTime =
        zmanim.getShabbatInfo(friday, location, 18.0).candleLightingTime!!

    @Test
    fun enablingCandleLightingSchedulesItsAlarm() {
        rebuild { it }
        assertTrue(alarms().isEmpty())

        rebuild { it.copy(notifyCandleLighting = true) }

        val alarm = alarms().single()
        assertEquals(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, alarm.action())
        assertEquals(
            millis(friday, candleLighting(Location.JERUSALEM).minusHours(2), Location.JERUSALEM),
            alarm.triggerAtMs
        )
    }

    @Test
    fun changingReminderTypeAndTimeReplacesTheAlarm() {
        rebuild { it.copy(notifyCandleLighting = true) }

        rebuild { it.copy(candleLightingNotifyMode = "morning", candleLightingMorningTime = 9 * 60) }
        assertEquals(
            millis(friday, LocalTime.of(9, 0), Location.JERUSALEM),
            alarms().single().triggerAtMs
        )

        rebuild { it.copy(candleLightingMorningTime = 10 * 60 + 30) }
        assertEquals(
            millis(friday, LocalTime.of(10, 30), Location.JERUSALEM),
            alarms().single().triggerAtMs
        )
    }

    @Test
    fun changingTheActiveLocationMovesTheAlarm() {
        rebuild { it.copy(notifyCandleLighting = true) }
        val jerusalem = alarms().single().triggerAtMs

        rebuild {
            it.copy(
                savedLocations = listOf(SavedLocation("ny", newYork)),
                activeLocationId = "ny"
            )
        }

        val alarm = alarms().single()
        assertNotEquals(jerusalem, alarm.triggerAtMs)
        assertEquals(millis(friday, candleLighting(newYork).minusHours(2), newYork), alarm.triggerAtMs)
    }

    @Test
    fun disablingCandleLightingCancelsItsAlarm() {
        rebuild { it.copy(notifyCandleLighting = true) }
        assertEquals(1, alarms().size)

        rebuild { it.copy(notifyCandleLighting = false) }

        assertTrue(alarms().isEmpty())
    }

    @Test
    fun omerAlarmsFollowTheirSettings() {
        rebuild { it.copy(devDateOverride = omerDate, notifyOmer = true, notifyOmerMorning = true) }
        assertEquals(
            setOf(NotificationAlarmScheduler.ACTION_OMER, NotificationAlarmScheduler.ACTION_OMER_MORNING),
            alarms().map { it.action() }.toSet()
        )

        rebuild { it.copy(notifyOmerMorning = false) }
        assertEquals(NotificationAlarmScheduler.ACTION_OMER, alarms().single().action())

        rebuild { it.copy(notifyOmer = false) }
        assertTrue(alarms().isEmpty())
    }
}
