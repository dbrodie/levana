package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.CityRepository
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Independent planner -> scheduler -> receiver checks (verify-notification-lookahead). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class VerifyLookaheadPathTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)
    private val notificationManager get() = app.getSystemService(NotificationManager::class.java)

    private lateinit var preferences: PreferencesRepository
    private val zmanim = ZmanimRepository()
    private val calendar = CalendarRepository()
    private val ny = Location(40.7128, -74.006, timezoneId = "America/New_York", name = "NY")
    private val nyZone = ZoneId.of("America/New_York")

    private val candles = UserPreferences(
        notifyCandleLighting = true,
        candleLightingNotifyMode = "morning",
        candleLightingMorningTime = 9 * 60 + 30
    )
    private val omer = UserPreferences(
        notifyCandleLighting = false,
        notifyOmer = true,
        notifyOmerTzait = true,
        notifyOmerMorning = true,
        notifyOmerMorningTime = 6 * 60 + 45
    )

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(app)
        preferences = PreferencesRepository(app, CityRepository(app))
        startKoin { modules(module { single { preferences } }) }
    }

    @After
    fun tearDown() { stopKoin() }

    /** Simulates one worker run at [now] (device zone NY) with the production apply path. */
    private fun workerRun(today: LocalDate, at: LocalTime, prefs: UserPreferences, posted: MutableList<LocalDate>) {
        runBlocking {
            preferences.saveNotifyCandleLighting(prefs.notifyCandleLighting)
            preferences.saveNotifyOmer(prefs.notifyOmer)
            preferences.saveNotifyOmerTzait(prefs.notifyOmerTzait)
            preferences.saveNotifyOmerMorning(prefs.notifyOmerMorning)
        }
        val now = ZonedDateTime.of(today, at, nyZone).toInstant()
        val plan = planReminderAlarms(today, now, nyZone, ny, prefs, zmanim, calendar)
        val scheduler = NotificationAlarmScheduler(app) { now.toEpochMilli() }
        runBlocking {
            applyReminderAlarmPlan(plan, scheduler, NotificationDeliveryLog(app)) {
                posted += it.date
                NotificationPoster.postCandleLighting(app, it.date, it.candleLightingTime, it.end)
            }
        }
    }

    private fun alarms(): List<ShadowAlarmManager.ScheduledAlarm> = shadowOf(alarmManager).scheduledAlarms
    private fun ShadowAlarmManager.ScheduledAlarm.intent(): Intent = shadowOf(operation).savedIntent
    private fun ShadowAlarmManager.ScheduledAlarm.key() =
        intent().action to LocalDate.ofEpochDay(intent().getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0))

    private fun alarmFor(action: String, date: LocalDate) = alarms().single { it.key() == action to date }

    /** Fires an alarm as AlarmManager would: broadcast its saved intent to the manifest receiver. */
    private fun fire(alarm: ShadowAlarmManager.ScheduledAlarm) {
        // The manifest receiver has no intent-filter, so Robolectric cannot route the explicit
        // intent; register the real receiver class and send the same action + extras.
        val receiver = NotificationAlarmReceiver()
        val before = shadowOf(notificationManager).allNotifications.size
        val filter = IntentFilter(alarm.intent().action).apply { addDataScheme("levana-alarm") }
        app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        app.sendBroadcast(Intent(alarm.intent()).setComponent(null).setPackage(app.packageName))
        shadowOf(Looper.getMainLooper()).idle()
        val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5)
        while (shadowOf(notificationManager).allNotifications.size == before && System.nanoTime() < deadline) {
            Thread.sleep(10)
            shadowOf(Looper.getMainLooper()).idle()
        }
        app.unregisterReceiver(receiver)
        shadowOf(Looper.getMainLooper()).idle()
        alarmManager.cancel(alarm.operation!!)
    }

    private fun awaitLoggedDate(expected: LocalDate) {
        repeat(100) {
            if (runBlocking { NotificationDeliveryLog(app).lastCandleLightingDate() } == expected) return
            Thread.sleep(20)
        }
        assertEquals(expected, runBlocking { NotificationDeliveryLog(app).lastCandleLightingDate() })
    }

    private val ACTION_C = NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING
    private val ACTION_E = NotificationAlarmScheduler.ACTION_OMER
    private val ACTION_M = NotificationAlarmScheduler.ACTION_OMER_MORNING

    @Test
    fun `late Friday run posts today once and leaves next Friday's alarm, which delivers`() {
        val fri = LocalDate.of(2026, 11, 13)
        val posted = mutableListOf<LocalDate>()
        workerRun(fri, LocalTime.of(21, 0), candles, posted)
        assertEquals(listOf(fri), posted)

        val next = alarmFor(ACTION_C, fri.plusWeeks(1))
        assertEquals(AlarmManager.RTC_WAKEUP, next.type)
        assertEquals(ZonedDateTime.of(fri.plusWeeks(1), LocalTime.of(9, 30), nyZone).toInstant().toEpochMilli(), next.triggerAtTime)
        assertEquals(1, alarms().size)

        val before = shadowOf(notificationManager).allNotifications.size
        fire(next)
        assertEquals(before + 1, shadowOf(notificationManager).allNotifications.size)
        awaitLoggedDate(fri.plusWeeks(1))
    }

    @Test
    fun `alarm delivered in the morning prevents a later run from posting again`() {
        val fri = LocalDate.of(2026, 12, 4)
        val posted = mutableListOf<LocalDate>()
        workerRun(fri.minusDays(1), LocalTime.of(23, 0), candles, posted)
        fire(alarmFor(ACTION_C, fri))
        awaitLoggedDate(fri)

        workerRun(fri, LocalTime.of(20, 0), candles, posted)
        assertEquals(emptyList<LocalDate>(), posted)
        assertTrue(alarms().none { it.key() == ACTION_C to fri })
    }

    @Test
    fun `overdue undelivered candle alarm is cancelled and the notice is posted exactly once`() {
        val fri = LocalDate.of(2026, 12, 11)
        val posted = mutableListOf<LocalDate>()
        workerRun(fri.minusDays(1), LocalTime.of(23, 0), candles, posted)
        assertEquals(1, alarms().count { it.key() == ACTION_C to fri })

        workerRun(fri, LocalTime.of(10, 0), candles, posted)
        workerRun(fri, LocalTime.of(15, 0), candles, posted)
        assertEquals(listOf(fri), posted)
        assertTrue(alarms().none { it.key() == ACTION_C to fri })
    }

    @Test
    fun `daily runs slide the window without duplicating alarms`() {
        val firstNight = LocalDate.of(2027, 4, 22) // 15 Nisan 5787
        val start = firstNight.plusDays(5)
        val posted = mutableListOf<LocalDate>()
        for (d in 0..3L) {
            workerRun(start.plusDays(d), LocalTime.of(5, 0), omer, posted)
            workerRun(start.plusDays(d), LocalTime.of(5, 0), omer, posted) // repeated run
        }
        val keys = alarms().map { it.key() }
        assertEquals(keys.size, keys.toSet().size)
        val last = start.plusDays(3)
        val expected = (0..7L).flatMap { listOf(ACTION_E to last.plusDays(it), ACTION_M to last.plusDays(it)) }.toSet() +
            (0..2L).flatMap { listOf(ACTION_E to start.plusDays(it), ACTION_M to start.plusDays(it)) }
        assertEquals(expected, keys.toSet())

        val e = alarmFor(ACTION_E, last.plusDays(7))
        assertEquals((last.plusDays(7).toEpochDay() - firstNight.toEpochDay() + 1).toInt(),
            e.intent().getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0))
        assertEquals(ZonedDateTime.of(last.plusDays(7), zmanim.getTzaitTime(last.plusDays(7), ny)!!, nyZone).toInstant().toEpochMilli(),
            e.triggerAtTime)

        val before = shadowOf(notificationManager).allNotifications.size
        fire(e)
        fire(alarmFor(ACTION_M, last.plusDays(7)))
        assertEquals(before + 2, shadowOf(notificationManager).allNotifications.size)
        assertEquals(emptyList<LocalDate>(), posted)
    }

    @Test
    fun `window crossing the end of the Omer schedules nothing past day 49 and cleans up`() {
        val lastNight = LocalDate.of(2027, 6, 9) // 4 Sivan 5787
        val posted = mutableListOf<LocalDate>()
        workerRun(lastNight.minusDays(1), LocalTime.of(5, 0), omer, posted)
        val keys = alarms().map { it.key() }
        assertEquals(listOf(lastNight.minusDays(1), lastNight), keys.filter { it.first == ACTION_E }.map { it.second }.sorted())
        assertEquals(listOf(lastNight.minusDays(1), lastNight, lastNight.plusDays(1)),
            keys.filter { it.first == ACTION_M }.map { it.second }.sorted())
        assertEquals(49, alarmFor(ACTION_E, lastNight).intent().getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0))
        assertEquals(49, alarmFor(ACTION_M, lastNight.plusDays(1)).intent().getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0))
    }

    private fun notifications() = shadowOf(notificationManager).allNotifications.size

    /**
     * Required: an Omer alarm whose trigger has passed but which AlarmManager has not yet
     * delivered (e.g. deferred by Doze) survives later runs unchanged and then delivers once.
     */
    @Test
    fun `overdue undelivered Omer evening alarm survives repeated runs and delivers once`() {
        val day = LocalDate.of(2027, 5, 3)
        val posted = mutableListOf<LocalDate>()
        workerRun(day, LocalTime.of(5, 0), omer, posted)
        val original = alarmFor(ACTION_E, day)
        val trigger = original.triggerAtTime
        val omerDay = original.intent().getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0)

        for (t in listOf(LocalTime.of(23, 0), LocalTime.of(23, 30), LocalTime.of(23, 50))) {
            workerRun(day, t, omer, posted)
            val kept = alarms().filter { it.key() == ACTION_E to day }
            assertEquals("run at $t", 1, kept.size)
            assertEquals(trigger, kept[0].triggerAtTime)
            assertEquals(omerDay, kept[0].intent().getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0))
        }

        val before = notifications()
        fire(alarmFor(ACTION_E, day))
        assertEquals(before + 1, notifications())

        // After delivery, later runs must not re-create or re-post it
        workerRun(day, LocalTime.of(23, 55), omer, posted)
        assertTrue(alarms().none { it.key() == ACTION_E to day })
        assertEquals(before + 1, notifications())
        assertEquals(emptyList<LocalDate>(), posted)
    }

    @Test
    fun `overdue undelivered Omer morning alarm survives repeated runs and delivers once`() {
        val day = LocalDate.of(2027, 5, 4)
        val posted = mutableListOf<LocalDate>()
        workerRun(day, LocalTime.of(5, 0), omer, posted)
        val trigger = alarmFor(ACTION_M, day).triggerAtTime

        workerRun(day, LocalTime.of(12, 0), omer, posted)
        workerRun(day, LocalTime.of(12, 0), omer, posted)
        val kept = alarms().filter { it.key() == ACTION_M to day }
        assertEquals(1, kept.size)
        assertEquals(trigger, kept[0].triggerAtTime)
        // Tonight's evening alarm is still future and still scheduled once
        assertEquals(1, alarms().count { it.key() == ACTION_E to day })

        val before = notifications()
        fire(kept[0])
        assertEquals(before + 1, notifications())
        workerRun(day, LocalTime.of(13, 0), omer, posted)
        assertTrue(alarms().none { it.key() == ACTION_M to day })
        assertEquals(before + 1, notifications())
    }

    @Test
    fun `disabling Omer cancels overdue undelivered evening and morning alarms`() {
        val day = LocalDate.of(2027, 5, 5)
        val posted = mutableListOf<LocalDate>()
        workerRun(day, LocalTime.of(5, 0), omer, posted)
        assertEquals(8, alarms().count { it.key().first == ACTION_E })

        workerRun(day, LocalTime.of(23, 50), omer.copy(notifyOmer = false), posted)
        assertTrue(alarms().none { it.key().first == ACTION_E || it.key().first == ACTION_M })
    }

    @Test
    fun `turning off only one Omer reminder cancels that kind and keeps the other overdue alarm`() {
        val day = LocalDate.of(2027, 5, 6)
        val posted = mutableListOf<LocalDate>()
        workerRun(day, LocalTime.of(5, 0), omer, posted)

        // Both of today's are overdue at 23:50; tzait turned off -> evenings go, mornings stay
        workerRun(day, LocalTime.of(23, 50), omer.copy(notifyOmerTzait = false), posted)
        assertTrue(alarms().none { it.key().first == ACTION_E })
        assertEquals(1, alarms().count { it.key() == ACTION_M to day })

        // Morning turned off too -> today's overdue morning is cancelled as well
        workerRun(day, LocalTime.of(23, 55), omer.copy(notifyOmerTzait = false, notifyOmerMorning = false), posted)
        assertTrue(alarms().none { it.key().first == ACTION_M })
    }

    @Test
    fun `overdue undelivered hours-before candle alarm is kept and not posted by the worker`() {
        val fri = LocalDate.of(2026, 12, 18)
        val prefs = UserPreferences(notifyCandleLighting = true, candleLightingNotifyMode = "hours_before", candleLightingHoursBefore = 2)
        val posted = mutableListOf<LocalDate>()
        workerRun(fri, LocalTime.of(5, 0), prefs, posted)
        val trigger = alarmFor(ACTION_C, fri).triggerAtTime

        workerRun(fri, LocalTime.of(23, 0), prefs, posted)
        workerRun(fri, LocalTime.of(23, 0), prefs, posted)
        assertEquals(trigger, alarms().single { it.key() == ACTION_C to fri }.triggerAtTime)
        assertEquals(emptyList<LocalDate>(), posted)

        val before = notifications()
        fire(alarmFor(ACTION_C, fri))
        assertEquals(before + 1, notifications())

        // Disabling candle lighting cancels pending ones (next Friday's)
        workerRun(fri, LocalTime.of(23, 30), prefs.copy(notifyCandleLighting = false), posted)
        assertTrue(alarms().none { it.key().first == ACTION_C })
    }
}
