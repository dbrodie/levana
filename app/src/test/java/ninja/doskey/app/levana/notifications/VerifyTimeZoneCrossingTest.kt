package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Looper
import androidx.work.Configuration
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm
import org.robolectric.shadows.ShadowBroadcastPendingResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Independent verification of time-zone / clock changes against the candidate's
 * [TimeChangeReceiver] and live-zone clock. The device clock is a controllable instant whose
 * zone is always the JVM default, the way Android's default zone follows the system setting.
 *
 * Omer 2030: Fri 26 Apr is day 8. Morning reminder configured for 07:00.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class VerifyTimeZoneCrossingTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)
    private val ny = ZoneId.of("America/New_York")
    private val moscow = ZoneId.of("Europe/Moscow")
    private val jerusalemZone = ZoneId.of("Asia/Jerusalem")
    private val newYork = Location(40.7128, -74.0060, 0.0, ny.id, "New York")
    private val jerusalem = Location(31.7683, 35.2137, 754.0, jerusalemZone.id, "Jerusalem")
    private val fri = LocalDate.of(2030, 4, 26)
    private val savedZone = TimeZone.getDefault()

    private var now: Instant = Instant.EPOCH
    private val deviceClock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.systemDefault()
        override fun withZone(zone: ZoneId): Clock = fixed(now, zone)
        override fun instant() = now
    }
    private var failRebuild = false
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
            notifyOmerMorningTime = 7 * 60
        )
    )
    private lateinit var rebuilder: NotificationAlarmRebuilder
    private val receivers = mutableListOf<BroadcastReceiver>()

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(app)
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(app, Configuration.Builder().build())
        }
        rebuilder = NotificationAlarmRebuilder(
            flow {
                check(!failRebuild) { "simulated failure" }
                emitAll(prefs)
            },
            ZmanimRepository(),
            CalendarRepository(),
            NotificationAlarmScheduler(app) { now.toEpochMilli() }
        ) { deviceClock }
        startKoin { modules(module { single { rebuilder } }) }
        runBlocking { NotificationDeliveryLog(app).recordCandleLighting(LocalDate.of(2001, 1, 1)) }
    }

    @After
    fun tearDown() {
        stopKoin()
        TimeZone.setDefault(savedZone)
    }

    // --- helpers ---

    private fun setZone(zone: ZoneId) = TimeZone.setDefault(TimeZone.getTimeZone(zone))

    /** Puts the device in [zone] at the wall time [date] [time] there. */
    private fun deviceAt(zone: ZoneId, date: LocalDate, time: LocalTime) {
        setZone(zone)
        now = ZonedDateTime.of(date, time, zone).toInstant()
    }

    private fun at(date: LocalDate, h: Int, m: Int, zone: ZoneId) =
        ZonedDateTime.of(date, LocalTime.of(h, m), zone).toInstant().toEpochMilli()

    private fun at7(date: LocalDate, zone: ZoneId) = at(date, 7, 0, zone)

    private fun scheduled(action: String): Map<LocalDate, List<ScheduledAlarm>> =
        shadowOf(alarmManager).scheduledAlarms
            .filter { shadowOf(it.operation).savedIntent.action == action }
            .groupBy {
                LocalDate.ofEpochDay(
                    shadowOf(it.operation).savedIntent
                        .getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0)
                )
            }

    private fun triggers(action: String) = scheduled(action).mapValues { (_, l) -> l.map { it.triggerAtMs } }
    private fun morning() = triggers(NotificationAlarmScheduler.ACTION_OMER_MORNING)
    private fun tzait() = triggers(NotificationAlarmScheduler.ACTION_OMER)
    private fun candle() = triggers(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)

    /** Android removes a one-shot alarm once it is delivered. */
    private fun deliver(action: String, date: LocalDate) {
        val alarm = scheduled(action).getValue(date).single()
        alarmManager.cancel(alarm.operation!!)
    }

    /** Sends [action] through a registered [TimeChangeReceiver] and waits for goAsync to finish. */
    private fun broadcast(action: String): BroadcastReceiver.PendingResult {
        val receiver = TimeChangeReceiver()
        receivers += receiver
        app.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_EXPORTED)
        app.sendBroadcast(Intent(action))
        shadowOf(Looper.getMainLooper()).idle()
        val pending = shadowOf(receiver).originalPendingResult
        assertNotNull("receiver should go async", pending)
        Shadow.extract<ShadowBroadcastPendingResult>(pending).future.get(5, TimeUnit.SECONDS)
        app.unregisterReceiver(receiver)
        return pending!!
    }

    private fun postedCount() =
        shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.size

    private fun assertNoSideEffects() {
        assertEquals("a reconcile posts nothing", 0, postedCount())
        assertTrue(
            "no daily category work enqueued",
            WorkManager.getInstance(app)
                .getWorkInfosByTag(DailyNotificationWorker::class.java.name).get().isEmpty()
        )
        assertEquals(
            "delivery log untouched",
            LocalDate.of(2001, 1, 1),
            runBlocking { NotificationDeliveryLog(app).lastCandleLightingDate() }
        )
    }

    private fun window(today: LocalDate) = (0..7L).map { today.plusDays(it) }

    // --- eastward: New York -> Moscow (+7h) ---

    @Test
    fun eastward_beforeMorningTime_dateCrossesForward() {
        // Thu 23:30 NY == Fri 06:30 Moscow
        deviceAt(ny, fri.minusDays(1), LocalTime.of(23, 30))
        runBlocking { rebuilder.rebuild() }
        assertEquals(at7(fri, ny), morning().getValue(fri).single())
        val tzaitBefore = tzait()

        setZone(moscow)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)

        // Fri morning moves to 07:00 Moscow (30 min away), not 14:00 Moscow
        assertEquals(window(fri).associateWith { listOf(at7(it, moscow)) }, morning())
        // Nightfall stays at New York tzait for every date planned before and after
        for (d in window(fri).dropLast(1)) assertEquals("tzait $d", tzaitBefore[d], tzait()[d])
        val nyTzait = ZmanimRepository().getTzaitTime(fri, newYork)!!
        assertEquals(
            ZonedDateTime.of(fri, nyTzait, ny).toInstant().toEpochMilli(),
            tzait().getValue(fri).single()
        )
        assertNoSideEffects()
    }

    @Test
    fun eastward_afterMorningTime_sameDate_keepsUndeliveredAlarmAndMovesTheRest() {
        // Fri 03:00 NY == Fri 10:00 Moscow; Fri 07:00 NY alarm (14:00 Moscow) is still pending
        deviceAt(ny, fri, LocalTime.of(3, 0))
        runBlocking { rebuilder.rebuild() }
        val friAlarm = scheduled(NotificationAlarmScheduler.ACTION_OMER_MORNING).getValue(fri).single()

        setZone(moscow)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)

        val after = scheduled(NotificationAlarmScheduler.ACTION_OMER_MORNING)
        // Policy: 07:00 Moscow already passed and Fri's reminder was never delivered, so the
        // pending alarm is kept (fires late, 14:00 Moscow) rather than dropped
        val kept = after.getValue(fri).single()
        assertSame(friAlarm.operation, kept.operation)
        assertEquals(at7(fri, ny), kept.triggerAtMs)
        for (d in window(fri).drop(1)) {
            assertEquals("$d", listOf(at7(d, moscow)), after.getValue(d).map { it.triggerAtMs })
        }
        assertNoSideEffects()
    }

    // --- westward: Moscow -> New York (-7h) ---

    @Test
    fun westward_beforeNyMorning_afterMoscowDelivery_replansSameDayWithoutReposting() {
        val omerLog = NotificationDeliveryLog(testDataStore())
        fun post(alarm: ScheduledAlarm) = runBlocking {
            deliverAlarm(app, shadowOf(alarm.operation).savedIntent, prefs.value, omerLog)
        }
        // Fri 10:00 Moscow: Fri's 07:00 Moscow reminder was delivered
        deviceAt(moscow, fri, LocalTime.of(5, 0))
        runBlocking { rebuilder.rebuild() }
        now = ZonedDateTime.of(fri, LocalTime.of(10, 0), moscow).toInstant()
        assertTrue(post(scheduled(NotificationAlarmScheduler.ACTION_OMER_MORNING).getValue(fri).single()))
        deliver(NotificationAlarmScheduler.ACTION_OMER_MORNING, fri)
        app.getSystemService(NotificationManager::class.java).cancelAll()

        setZone(ny) // Fri 03:00 NY
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)

        val after = morning()
        // Fri is planned again at 07:00 NY ...
        assertEquals(window(fri).associateWith { listOf(at7(it, ny)) }, after)
        assertNoSideEffects()
        // ... but when it fires, the delivered Omer-day morning notice is not posted again
        assertFalse(post(scheduled(NotificationAlarmScheduler.ACTION_OMER_MORNING).getValue(fri).single()))
        assertEquals(0, postedCount())
        assertTrue(post(scheduled(NotificationAlarmScheduler.ACTION_OMER_MORNING).getValue(fri.plusDays(1)).single()))
    }

    @Test
    fun westward_afterMorningTimeInBothZones_doesNotRepeatToday() {
        // Fri 16:00 Moscow == Fri 09:00 NY
        deviceAt(moscow, fri, LocalTime.of(5, 0))
        runBlocking { rebuilder.rebuild() }
        now = ZonedDateTime.of(fri, LocalTime.of(16, 0), moscow).toInstant()
        deliver(NotificationAlarmScheduler.ACTION_OMER_MORNING, fri)

        setZone(ny)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)

        val after = morning()
        assertNull("Fri not re-planned", after[fri])
        for (d in window(fri).drop(1)) assertEquals("$d", listOf(at7(d, ny)), after[d])
    }

    @Test
    fun westward_dateCrossesBackward_cancelsLastOldWindowDayAndNextRebuildReplansIt() {
        // Sat 02:00 Moscow == Fri 19:00 NY
        val sat = fri.plusDays(1)
        deviceAt(moscow, sat, LocalTime.of(2, 0))
        runBlocking { rebuilder.rebuild() }
        assertEquals(window(sat).toSet(), morning().keys)

        setZone(ny)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)

        val after = morning()
        assertNull("Fri 07:00 NY passed; nothing re-added", after[fri])
        for (d in window(fri).drop(1)) assertEquals("$d", listOf(at7(d, ny)), after[d])
        // Sat+7 lies outside the new Fri..Fri+7 window: its 07:00 Moscow (00:00 NY) alarm is
        // cancelled rather than left to fire at the wrong time
        val outside = sat.plusDays(7)
        assertNull(after[outside])

        // The next day's rebuild (daily worker / any later reconcile) plans it at 07:00 NY
        now = ZonedDateTime.of(sat, LocalTime.of(9, 0), ny).toInstant()
        runBlocking { rebuilder.rebuild() }
        assertEquals(listOf(at7(outside, ny)), morning()[outside])
    }

    // --- round trips, duplicates, manual clock changes ---

    @Test
    fun repeatedBroadcastsAndRoundTripLeaveExactlyOneAlarmPerDate() {
        // Thu 10:00 NY == Thu 17:00 Moscow: same civil date, so both windows match
        deviceAt(ny, fri.minusDays(1), LocalTime.of(10, 0))
        runBlocking { rebuilder.rebuild() }
        val before = triggers(NotificationAlarmScheduler.ACTION_OMER_MORNING)

        setZone(moscow)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)
        broadcast(Intent.ACTION_TIME_CHANGED)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)
        setZone(ny)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)

        assertEquals(before, morning())
        val all = shadowOf(alarmManager).scheduledAlarms
        val keys = all.map {
            val i = shadowOf(it.operation).savedIntent
            i.action to i.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0)
        }
        assertEquals("no duplicate pending alarms", keys.size, keys.toSet().size)
    }

    @Test
    fun manualClockJumpForward_timeSetMovesTheWindowAndCancelsSkippedDays() {
        deviceAt(ny, fri, LocalTime.of(5, 0))
        runBlocking { rebuilder.rebuild() }
        assertEquals(window(fri).toSet(), morning().keys)

        // User sets the clock two days ahead, same zone
        now = ZonedDateTime.of(fri.plusDays(2), LocalTime.of(5, 0), ny).toInstant()
        broadcast(Intent.ACTION_TIME_CHANGED)

        val after = morning()
        for (d in window(fri.plusDays(2))) assertEquals("$d", listOf(at7(d, ny)), after[d])
        // Fri's alarm is now in the past and outside the new window, so it is cancelled
        // instead of firing immediately as a late delivery
        assertNull(after[fri])
        assertNoSideEffects()
    }

    @Test
    fun manualClockJumpBackward_timeSetReplansWithoutDuplicates() {
        deviceAt(ny, fri.plusDays(2), LocalTime.of(5, 0))
        runBlocking { rebuilder.rebuild() }

        now = ZonedDateTime.of(fri, LocalTime.of(5, 0), ny).toInstant()
        broadcast(Intent.ACTION_TIME_CHANGED)

        val after = morning()
        for (d in window(fri)) assertEquals("$d", listOf(at7(d, ny)), after[d])
        assertTrue(after.values.all { it.size == 1 })
    }

    // --- disabled reminders, location zone, receiver lifecycle ---

    @Test
    fun disabledMorningReminderIsCancelledOnZoneChangeAndNightfallKept() {
        deviceAt(ny, fri.minusDays(1), LocalTime.of(22, 0))
        runBlocking { rebuilder.rebuild() }
        val tzaitBefore = tzait()
        assertTrue(morning().isNotEmpty())

        prefs.value = prefs.value.copy(notifyOmerMorning = false)
        setZone(moscow)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)

        assertTrue(morning().isEmpty())
        for (d in window(fri).dropLast(1)) assertEquals("$d", tzaitBefore[d], tzait()[d])
    }

    @Test
    fun explicitLocationZoneStaysTiedToLocation() {
        // Location Jerusalem, device moves New York -> Moscow
        prefs.value = prefs.value.copy(currentLocation = jerusalem)
        deviceAt(ny, fri.minusDays(1), LocalTime.of(20, 0))
        runBlocking { rebuilder.rebuild() }
        setZone(moscow)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)

        assertEquals(listOf(at(fri, 9, 0, jerusalemZone)), candle()[fri])
        val tz = ZmanimRepository().getTzaitTime(fri, jerusalem)!!
        assertEquals(
            listOf(ZonedDateTime.of(fri, tz, jerusalemZone).toInstant().toEpochMilli()),
            tzait()[fri]
        )
        assertEquals(listOf(at7(fri, moscow)), morning()[fri])
    }

    @Test
    fun rebuildFailureStillFinishesTheBroadcastAndNextChangeRecovers() {
        deviceAt(ny, fri.minusDays(1), LocalTime.of(22, 0))
        runBlocking { rebuilder.rebuild() }

        failRebuild = true
        setZone(moscow)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED) // future completes => finish() called
        assertEquals("unchanged after failure", listOf(at7(fri, ny)), morning()[fri])

        failRebuild = false
        broadcast(Intent.ACTION_TIME_CHANGED)
        assertEquals(listOf(at7(fri, moscow)), morning()[fri])
    }

    @Test
    fun unrelatedActionIsIgnoredWithoutGoingAsync() {
        deviceAt(ny, fri.minusDays(1), LocalTime.of(22, 0))
        val receiver = TimeChangeReceiver()
        receiver.onReceive(app, Intent(Intent.ACTION_DATE_CHANGED))
        assertFalse(shadowOf(receiver).wentAsync())
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun manifestReceiverIsExportedForProtectedSystemBroadcastsOnly() {
        val info = app.packageManager.getReceiverInfo(
            android.content.ComponentName(app, TimeChangeReceiver::class.java), 0
        )
        assertTrue(info.exported)
        for (action in listOf(Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED)) {
            val names = app.packageManager
                .queryBroadcastReceivers(Intent(action).setPackage(app.packageName), 0)
                .map { it.activityInfo.name }
            assertEquals(listOf(TimeChangeReceiver::class.java.name), names)
        }
    }
}
