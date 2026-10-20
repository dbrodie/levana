package ninja.doskey.app.levana.notifications

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

/** Today's overdue morning candle-lighting alarm across the real [NotificationAlarmRebuilder.rebuild]. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsRebuildOverdueCandleTest {

    private class MutableClock(var instant: Instant, private val zone: ZoneId) : Clock() {
        override fun getZone() = zone
        override fun withZone(zone: ZoneId) = MutableClock(instant, zone)
        override fun instant() = instant
    }

    private val app: Context get() = RuntimeEnvironment.getApplication()
    private val nyZone = ZoneId.of("America/New_York")
    private val newYork = Location(40.7128, -74.0060, 0.0, nyZone.id, "New York")
    private val friday = LocalDate.of(2026, 10, 23)

    private val clock = MutableClock(at(8, 0), nyZone)
    private val prefs = MutableStateFlow(
        UserPreferences(
            currentLocation = newYork,
            notifyCandleLighting = true,
            candleLightingNotifyMode = "morning",
            candleLightingMorningTime = 9 * 60 + 30
        )
    )
    private val rebuilder = NotificationAlarmRebuilder(
        prefs,
        ZmanimRepository(),
        CalendarRepository(),
        NotificationAlarmScheduler(app) { clock.millis() },
        clock
    )
    private val deliveryLog = NotificationDeliveryLog(app)
    private var posted = 0

    private fun at(h: Int, m: Int): Instant =
        ZonedDateTime.of(friday, LocalTime.of(h, m), nyZone).toInstant()

    private fun candleAlarm(): ScheduledAlarm? =
        shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms.singleOrNull {
            val intent = shadowOf(it.operation).savedIntent
            intent.action == NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING &&
                intent.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0) ==
                friday.toEpochDay()
        }

    private fun settingsRebuild(update: (UserPreferences) -> UserPreferences) = runBlocking {
        prefs.value = update(prefs.value)
        rebuilder.rebuild()
    }

    private fun workerRebuild() = runBlocking { rebuilder.rebuild(deliveryLog) { posted++ } }

    private fun scheduleAtEight(): ScheduledAlarm {
        settingsRebuild { it }
        val alarm = candleAlarm()!!
        assertEquals(at(9, 30).toEpochMilli(), alarm.triggerAtMs)
        return alarm
    }

    @Test
    fun unrelatedSettingsChangeKeepsTheUndeliveredMorningAlarm() {
        val alarm = scheduleAtEight()

        clock.instant = at(9, 35)
        settingsRebuild { it.copy(notifyOmerMorningTime = 400) }

        val kept = candleAlarm()
        assertSame("same pending alarm, untouched", alarm.operation, kept?.operation)
        assertEquals(at(9, 30).toEpochMilli(), kept!!.triggerAtMs)
        assertEquals("a settings rebuild never posts", 0, posted)
    }

    @Test
    fun switchingCandleLightingOffStillCancelsTheOverdueAlarm() {
        scheduleAtEight()

        clock.instant = at(9, 35)
        settingsRebuild { it.copy(notifyCandleLighting = false) }

        assertNull(candleAlarm())
    }

    @Test
    fun dailyCatchUpCancelsTheAlarmAndPostsOnce() = runBlocking {
        deliveryLog.recordCandleLighting(friday.minusDays(7))
        scheduleAtEight()

        clock.instant = at(9, 35)
        workerRebuild()
        assertNull("posted by the worker instead", candleAlarm())
        assertEquals(1, posted)
        assertEquals(friday, deliveryLog.lastCandleLightingDate())

        workerRebuild()
        settingsRebuild { it.copy(notifyOmerMorningTime = 410) }
        assertEquals("posted only once", 1, posted)
        assertNull(candleAlarm())
    }
}
