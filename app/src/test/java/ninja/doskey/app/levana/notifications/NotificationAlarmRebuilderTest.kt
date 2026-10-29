package ninja.doskey.app.levana.notifications

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.device.fixedClock
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
            candleLightingNotifyMode = "hours_before",
            candleLightingHoursBefore = 2
        )
    )

    // The device is in Jerusalem; its clock reads midnight on [today]
    private var today = friday
    private val rebuilder = NotificationAlarmRebuilder(
        prefs, zmanim, calendar, NotificationAlarmScheduler(app)
    ) { fixedClock(today, LocalTime.MIDNIGHT, ZoneId.of(Location.JERUSALEM.timezoneId)) }

    private fun rebuild(update: (UserPreferences) -> UserPreferences) = runBlocking {
        prefs.value = update(prefs.value)
        rebuilder.rebuild()
    }

    private fun alarms(): List<ScheduledAlarm> =
        shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms

    private fun ScheduledAlarm.action(): String? = shadowOf(operation).savedIntent.action

    private fun alarmsOn(date: LocalDate): List<ScheduledAlarm> = alarms().filter {
        shadowOf(it.operation).savedIntent.getLongExtra(
            NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0L
        ) == date.toEpochDay()
    }

    private fun millis(date: LocalDate, time: LocalTime, location: Location): Long =
        ZonedDateTime.of(date, time, ZoneId.of(location.timezoneId)).toInstant().toEpochMilli()

    private fun candleLighting(location: Location): LocalTime =
        zmanim.getShabbatInfo(friday, location, 18.0).candleLightingTime!!

    @Test
    fun enablingCandleLightingSchedulesItsAlarm() {
        rebuild { it }
        assertTrue(alarms().isEmpty())

        rebuild { it.copy(notifyCandleLighting = true) }

        val alarm = alarmsOn(friday).single()
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
            alarmsOn(friday).single().triggerAtMs
        )

        rebuild { it.copy(candleLightingMorningTime = 10 * 60 + 30) }
        assertEquals(
            millis(friday, LocalTime.of(10, 30), Location.JERUSALEM),
            alarmsOn(friday).single().triggerAtMs
        )
    }

    // TASK-5: previously selecting New York moved the alarm to New York's candle lighting.
    // The selected city is display-only; only the device position moves it.
    @Test
    fun selectingACityLeavesTheAlarmAndMovingTheDeviceMovesIt() {
        rebuild { it.copy(notifyCandleLighting = true) }
        val jerusalem = alarmsOn(friday).single().triggerAtMs

        rebuild {
            it.copy(
                savedLocations = listOf(SavedLocation("ny", newYork)),
                activeLocationId = "ny"
            )
        }
        assertEquals(jerusalem, alarmsOn(friday).single().triggerAtMs)

        rebuild { it.copy(currentLocation = newYork) }
        val alarm = alarmsOn(friday).single()
        assertNotEquals(jerusalem, alarm.triggerAtMs)
        assertEquals(millis(friday, candleLighting(newYork).minusHours(2), newYork), alarm.triggerAtMs)
    }

    @Test
    fun disablingCandleLightingCancelsItsAlarm() {
        rebuild { it.copy(notifyCandleLighting = true) }
        assertEquals(2, alarms().size)
        assertEquals(1, alarmsOn(friday).size)
        assertEquals(1, alarmsOn(friday.plusDays(7)).size)

        rebuild { it.copy(notifyCandleLighting = false) }

        assertTrue(alarms().isEmpty())
    }

    @Test
    fun omerAlarmsFollowTheirSettings() {
        today = omerDate
        rebuild { it.copy(notifyOmer = true, notifyOmerMorning = true) }
        assertEquals(
            setOf(NotificationAlarmScheduler.ACTION_OMER, NotificationAlarmScheduler.ACTION_OMER_MORNING),
            alarms().map { it.action() }.toSet()
        )

        rebuild { it.copy(notifyOmerMorning = false) }
        assertTrue(alarms().isNotEmpty())
        assertTrue(alarms().all { it.action() == NotificationAlarmScheduler.ACTION_OMER })

        rebuild { it.copy(notifyOmer = false) }
        assertTrue(alarms().isEmpty())
    }
}
