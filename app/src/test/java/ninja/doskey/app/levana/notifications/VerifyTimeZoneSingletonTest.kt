package ninja.doskey.app.levana.notifications

import android.app.AlarmManager
import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone

/**
 * Independent verification (device time-zone reminders). Written against the public surface the
 * baseline already had — the 4-argument constructor Koin uses — so the same file compiles and
 * runs on baseline to show the bug.
 *
 * The plan is pinned to the 2030 Omer with devDateOverride, so every trigger is in the future
 * relative to the real wall clock the default constructor reads.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class VerifyTimeZoneSingletonTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)
    private val ny = ZoneId.of("America/New_York")
    private val moscow = ZoneId.of("Europe/Moscow")
    private val newYork = Location(40.7128, -74.0060, 0.0, ny.id, "New York")
    // 26 Apr 2030 (Friday) is the 8th day of the Omer
    private val today = LocalDate.of(2030, 4, 26)
    private val window = (0..7L).map { today.plusDays(it) }
    private val savedZone = TimeZone.getDefault()

    private val prefs = MutableStateFlow(
        UserPreferences(
            currentLocation = newYork,
            isInIsrael = false,
            notifyCandleLighting = true,
            candleLightingNotifyMode = "morning",
            candleLightingMorningTime = 9 * 60,
            notifyOmer = true,
            notifyOmerTzait = true,
            notifyOmerMorning = true,
            notifyOmerMorningTime = 7 * 60,
            devDateOverride = today
        )
    )
    private lateinit var singleton: NotificationAlarmRebuilder

    @Before
    fun setUp() {
        setZone(ny)
        // Exactly how di/AppModule.kt builds it: the singleton is created in New York
        singleton = NotificationAlarmRebuilder(
            prefs, ZmanimRepository(), CalendarRepository(), NotificationAlarmScheduler(app)
        )
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(savedZone)
    }

    private fun setZone(zone: ZoneId) = TimeZone.setDefault(TimeZone.getTimeZone(zone))

    private fun at7(date: LocalDate, zone: ZoneId) =
        ZonedDateTime.of(date, LocalTime.of(7, 0), zone).toInstant().toEpochMilli()

    private fun alarms(action: String) = shadowOf(alarmManager).scheduledAlarms
        .filter { shadowOf(it.operation).savedIntent.action == action }
        .groupBy {
            LocalDate.ofEpochDay(
                shadowOf(it.operation).savedIntent
                    .getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0)
            )
        }
        .mapValues { (_, list) -> list.map { it.triggerAtMs } }

    private fun morning() = alarms(NotificationAlarmScheduler.ACTION_OMER_MORNING)

    @Test
    fun singletonCreatedInNewYorkPlansMorningAlarmsInMoscowAfterZoneChange() {
        runBlocking { singleton.rebuild() }
        assertEquals(window.associateWith { listOf(at7(it, ny)) }, morning())
        val tzait = alarms(NotificationAlarmScheduler.ACTION_OMER)
        val candle = alarms(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)

        setZone(moscow)
        runBlocking { singleton.rebuild() }

        // The reported bug: 07:00 New York == 14:00 Moscow
        assertEquals(
            "one 07:00 Moscow alarm per date",
            window.associateWith { listOf(at7(it, moscow)) },
            morning()
        )
        // Nightfall and candle lighting follow the location (New York), not the device
        assertEquals(tzait, alarms(NotificationAlarmScheduler.ACTION_OMER))
        assertEquals(candle, alarms(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING))
        assertEquals(
            listOf(ZonedDateTime.of(today, LocalTime.of(9, 0), ny).toInstant().toEpochMilli()),
            candle.getValue(today)
        )
    }
}
