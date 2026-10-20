package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Independent checks (verify-bugfix-integration) through the real
 * [NotificationAlarmRebuilder.rebuild] entry point, driven by a controllable clock.
 * Friday 23 Oct 2026, New York; morning-mode notice at 09:30.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class IntegrationVerifierRebuildEntryTest {

    private class TestClock(var instant: Instant, private val zone: ZoneId) : Clock() {
        override fun getZone() = zone
        override fun withZone(zone: ZoneId): Clock = TestClock(instant, zone)
        override fun instant() = instant
    }

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)
    private val notificationManager get() = app.getSystemService(NotificationManager::class.java)
    private val ny = Location(40.7128, -74.006, timezoneId = "America/New_York", name = "NY")
    private val nyZone = ZoneId.of("America/New_York")
    private val friday = LocalDate.of(2026, 10, 23)
    private fun at(h: Int, m: Int) = ZonedDateTime.of(friday, LocalTime.of(h, m), nyZone).toInstant()

    private val clock = TestClock(at(8, 0), nyZone)
    private val base = UserPreferences(
        currentLocation = ny,
        isInIsrael = false,
        notifyCandleLighting = true,
        candleLightingNotifyMode = "morning",
        candleLightingMorningTime = 9 * 60 + 30
    )
    private val prefs = MutableStateFlow(base)
    private val scheduler = NotificationAlarmScheduler(app) { clock.instant.toEpochMilli() }
    private val rebuilder = NotificationAlarmRebuilder(prefs, ZmanimRepository(), CalendarRepository(), scheduler, clock)
    private val log get() = NotificationDeliveryLog(app)
    private val posted = mutableListOf<LocalDate>()

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(app)
        // The delivery log persists across tests in one JVM
        runBlocking { log.recordCandleLighting(LocalDate.of(2000, 1, 1)) }
    }

    private fun candleAlarm(date: LocalDate = friday): Intent? = shadowOf(alarmManager).scheduledAlarms
        .map { shadowOf(it.operation).savedIntent }
        .singleOrNull {
            it.action == NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING &&
                it.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, -1) == date.toEpochDay()
        }

    private fun settingsRebuild() = runBlocking { rebuilder.rebuild() }
    private fun workerRebuild() = runBlocking {
        rebuilder.rebuild(log) { posted += it.date; NotificationPoster.postCandleLighting(app, it.date, it.candleLightingTime, it.end) }
    }

    @Test
    fun `settings-only rebuild keeps today's overdue undelivered morning alarm`() {
        workerRebuild()
        assertNotNull(candleAlarm())
        assertEquals(emptyList<LocalDate>(), posted)

        clock.instant = at(9, 35)
        prefs.value = base.copy(notifyOmerMorningTime = 400)
        settingsRebuild()
        assertNotNull("kept for delayed delivery", candleAlarm())
        assertEquals("settings rebuild posts nothing", 0, shadowOf(notificationManager).allNotifications.size)
        // Next Friday is also scheduled (forward horizon intact)
        assertNotNull(candleAlarm(friday.plusDays(7)))
    }

    @Test
    fun `worker catch-up cancels the overdue alarm and posts exactly once`() {
        workerRebuild()
        clock.instant = at(9, 35)
        workerRebuild()
        assertNull("catch-up cancels the pending alarm", candleAlarm())
        assertEquals(listOf(friday), posted)
        assertEquals(1, shadowOf(notificationManager).allNotifications.size)

        // Later runs the same day neither repost nor resurrect the alarm
        clock.instant = at(14, 0)
        workerRebuild()
        settingsRebuild()
        assertEquals(listOf(friday), posted)
        assertNull(candleAlarm())
        assertNotNull(candleAlarm(friday.plusDays(7)))
    }

    @Test
    fun `catch-up after the alarm was delivered does not post again`() {
        workerRebuild()
        clock.instant = at(9, 31)
        // Receiver delivered it and recorded the delivery
        val intent = candleAlarm()!!
        deliverIfEnabled(app, intent, base)
        runBlocking { log.recordCandleLighting(friday) }
        clock.instant = at(10, 0)
        workerRebuild()
        assertEquals(emptyList<LocalDate>(), posted)
        assertEquals(1, shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun `disabling candle lighting after the morning time cancels today's alarm and the horizon`() {
        workerRebuild()
        clock.instant = at(9, 35)
        prefs.value = base.copy(notifyCandleLighting = false)
        settingsRebuild()
        assertNull(candleAlarm())
        assertNull(candleAlarm(friday.plusDays(7)))
        // Worker run with it disabled posts nothing either
        workerRebuild()
        assertEquals(emptyList<LocalDate>(), posted)
    }

    @Test
    fun `moving the morning time later reschedules today's alarm to the new time`() {
        workerRebuild()
        clock.instant = at(9, 35)
        prefs.value = base.copy(candleLightingMorningTime = 11 * 60)
        settingsRebuild()
        val alarm = shadowOf(alarmManager).scheduledAlarms.single {
            shadowOf(it.operation).savedIntent.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, -1) == friday.toEpochDay()
        }
        assertEquals(at(11, 0).toEpochMilli(), alarm.triggerAtTime)
    }

    /** Documents behaviour: switching hours_before -> morning after the morning time. */
    @Test
    fun `switching to morning mode after its time keeps the pending hours-before alarm`() {
        prefs.value = base.copy(candleLightingNotifyMode = "hours_before", candleLightingHoursBefore = 2)
        workerRebuild()
        val before = candleAlarm()!!
        clock.instant = at(10, 0)
        prefs.value = base // morning 09:30 already passed
        settingsRebuild()
        // Same reminder/date identity is "wanted" (due) so the afternoon alarm still fires once
        assertNotNull(candleAlarm())
        assertEquals(before.getStringExtra(NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME),
            candleAlarm()!!.getStringExtra(NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME))
    }
}
