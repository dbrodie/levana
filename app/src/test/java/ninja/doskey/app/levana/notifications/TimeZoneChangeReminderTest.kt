package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import androidx.work.Configuration
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.di.allModules
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
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
import java.util.TimeZone

/**
 * Device time-zone changes in a running process. Friday 26 Apr 2030 is the 8th day of the Omer
 * and a candle-lighting day; the device position is New York.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TimeZoneChangeReminderTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)
    private val nyZone = ZoneId.of("America/New_York")
    private val moscowZone = ZoneId.of("Europe/Moscow")
    private val newYork = Location(40.7128, -74.0060, 0.0, nyZone.id, "New York")
    private val friday = LocalDate.of(2030, 4, 26)

    private var instant: Instant = Instant.EPOCH
    private val deviceClock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.systemDefault()
        override fun withZone(zone: ZoneId): Clock = fixed(instant, zone)
        override fun instant() = instant
    }
    private val prefs = MutableStateFlow(
        UserPreferences(
            currentLocation = newYork,
            isInIsrael = false,
            notifyCandleLighting = true,
            candleLightingNotifyMode = "morning",
            candleLightingMorningTime = 9 * 60 + 30,
            notifyOmer = true,
            notifyOmerTzait = true,
            notifyOmerMorning = true,
            notifyOmerMorningTime = 7 * 60
        )
    )
    private lateinit var rebuilder: NotificationAlarmRebuilder
    private val savedZone = TimeZone.getDefault()
    private val deliveryLog get() = NotificationDeliveryLog(app)

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(app)
        setDeviceZone(nyZone)
        rebuilder = NotificationAlarmRebuilder(
            prefs,
            ZmanimRepository(),
            CalendarRepository(),
            NotificationAlarmScheduler(app) { instant.toEpochMilli() }
        ) { deviceClock }
        // The delivery log persists across tests in one JVM
        runBlocking { deliveryLog.recordCandleLighting(LocalDate.of(2000, 1, 1)) }
    }

    @After
    fun tearDown() {
        stopKoin()
        TimeZone.setDefault(savedZone)
    }

    private fun setDeviceZone(zone: ZoneId) = TimeZone.setDefault(TimeZone.getTimeZone(zone))

    private fun startKoinWithRebuilder() {
        startKoin { modules(module { single { rebuilder } }) }
    }

    private fun alarms(action: String): Map<LocalDate, List<ScheduledAlarm>> =
        shadowOf(alarmManager).scheduledAlarms
            .filter { shadowOf(it.operation).savedIntent.action == action }
            .groupBy {
                LocalDate.ofEpochDay(
                    shadowOf(it.operation).savedIntent
                        .getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0)
                )
            }

    private fun triggers(action: String): Map<LocalDate, Long> =
        alarms(action).mapValues { (_, list) -> list.single().triggerAtMs }

    private fun morningTriggers() = triggers(NotificationAlarmScheduler.ACTION_OMER_MORNING)

    private fun sevenAm(date: LocalDate, zone: ZoneId) =
        ZonedDateTime.of(date, LocalTime.of(7, 0), zone).toInstant().toEpochMilli()

    /**
     * Delivers [action] to the receiver the package manager resolves for it from the app
     * manifest. Robolectric's unit-test manifest does not register the app's own receivers.
     */
    private fun broadcast(action: String) {
        val intent = Intent(action)
        val resolved = app.packageManager
            .queryBroadcastReceivers(Intent(intent).setPackage(app.packageName), 0)
            .single { it.activityInfo.packageName == app.packageName }
        val receiver = Class.forName(resolved.activityInfo.name)
            .getDeclaredConstructor().newInstance() as BroadcastReceiver
        app.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_EXPORTED)
        app.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertTrue("timed out waiting for the rebuild", condition())
    }

    private fun initializeWorkManager() {
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(app, Configuration.Builder().build())
        }
    }

    private fun postedCount() =
        shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.size

    @Test
    fun manifestRegistersTheReceiverForTimeZoneAndTimeSetBroadcasts() {
        for (action in listOf(Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED)) {
            val receivers = app.packageManager.queryBroadcastReceivers(
                Intent(action).setPackage(app.packageName), 0
            ).map { it.activityInfo.name }
            assertTrue("$action: $receivers", TimeChangeReceiver::class.java.name in receivers)
        }
    }

    @Test
    fun koinSingletonUsesTheDeviceZoneCurrentAtRebuildTime() = runBlocking {
        // The app's modules, with the device clock fixed at Friday 00:30 in Moscow (Thursday
        // evening in New York) but reading the JVM's zone on every call, like the real one
        instant = ZonedDateTime.of(friday, LocalTime.of(0, 30), moscowZone).toInstant()
        val koin = startKoin {
            androidContext(app)
            allowOverride(true)
            modules(allModules + module {
                single {
                    DeviceContext(get<PreferencesRepository>().preferences) {
                        Clock.fixed(instant, ZoneId.systemDefault())
                    }
                }
            })
        }.koin
        val repository = koin.get<PreferencesRepository>()
        repository.saveNotifyCandleLighting(false)
        repository.saveNotifyOmer(true)
        repository.saveNotifyOmerTzait(false)
        repository.saveNotifyOmerMorning(true)
        repository.saveNotifyOmerMorningTime(7 * 60)
        try {
            val singleton = koin.get<NotificationAlarmRebuilder>()
            singleton.rebuild()
            assertEquals(sevenAm(friday, nyZone), morningTriggers()[friday])

            setDeviceZone(moscowZone)
            assertSame(singleton, koin.get<NotificationAlarmRebuilder>())
            singleton.rebuild()

            val moved = morningTriggers()
            assertEquals((0..7L).map { friday.plusDays(it) }.toSet(), moved.keys)
            moved.forEach { (date, trigger) -> assertEquals(sevenAm(date, moscowZone), trigger) }
        } finally {
            repository.saveNotifyOmer(false)
        }
    }

    @Test
    fun timeZoneBroadcastMovesDeviceZoneAlarmsAndKeepsNightfall() {
        startKoinWithRebuilder()
        initializeWorkManager()
        // Thursday 22:00 in New York is Friday 05:00 in Moscow
        instant = ZonedDateTime.of(friday.minusDays(1), LocalTime.of(22, 0), nyZone).toInstant()
        runBlocking { rebuilder.rebuild() }
        assertEquals(sevenAm(friday, nyZone), morningTriggers()[friday])
        val tzait = triggers(NotificationAlarmScheduler.ACTION_OMER)
        val candles = triggers(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)

        setDeviceZone(moscowZone)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)
        awaitUntil { morningTriggers()[friday] == sevenAm(friday, moscowZone) }

        val window = (0..7L).map { friday.plusDays(it) }
        val morning = alarms(NotificationAlarmScheduler.ACTION_OMER_MORNING)
        assertEquals(window.toSet(), morning.keys)
        for (date in window) {
            assertEquals("one alarm for $date", 1, morning.getValue(date).size)
            assertEquals(sevenAm(date, moscowZone), morning.getValue(date).single().triggerAtMs)
        }
        val tzaitAfter = triggers(NotificationAlarmScheduler.ACTION_OMER)
        assertEquals(window.toSet(), tzaitAfter.keys)
        // Dates planned in both zones keep their nightfall instants
        window.dropLast(1).forEach {
            assertEquals("nightfall $it", tzait.getValue(it), tzaitAfter.getValue(it))
        }
        // TASK-5: the morning candle-lighting notice is in the device zone, so it moves too
        // (it previously stayed at 09:30 in the selected city's zone)
        val candlesAfter = triggers(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)
        assertEquals(
            ZonedDateTime.of(friday, LocalTime.of(9, 30), nyZone).toInstant().toEpochMilli(),
            candles.getValue(friday)
        )
        assertEquals(
            ZonedDateTime.of(friday, LocalTime.of(9, 30), moscowZone).toInstant().toEpochMilli(),
            candlesAfter.getValue(friday)
        )

        assertEquals("a time-zone change posts nothing", 0, postedCount())
        assertTrue(
            "no daily category is re-posted",
            WorkManager.getInstance(app)
                .getWorkInfosByTag(DailyNotificationWorker::class.java.name).get().isEmpty()
        )
    }

    @Test
    fun timeSetBroadcastAlsoRebuilds() {
        startKoinWithRebuilder()
        instant = ZonedDateTime.of(friday.minusDays(1), LocalTime.of(22, 0), nyZone).toInstant()
        runBlocking { rebuilder.rebuild() }

        setDeviceZone(moscowZone)
        broadcast(Intent.ACTION_TIME_CHANGED)

        awaitUntil { morningTriggers()[friday] == sevenAm(friday, moscowZone) }
    }

    @Test
    fun disabledOmerRemindersAreCancelledNotRescheduled() {
        startKoinWithRebuilder()
        instant = ZonedDateTime.of(friday.minusDays(1), LocalTime.of(22, 0), nyZone).toInstant()
        runBlocking { rebuilder.rebuild() }
        assertTrue(morningTriggers().isNotEmpty())

        prefs.value = prefs.value.copy(notifyOmer = false)
        setDeviceZone(moscowZone)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)

        awaitUntil {
            alarms(NotificationAlarmScheduler.ACTION_OMER_MORNING).isEmpty() &&
                alarms(NotificationAlarmScheduler.ACTION_OMER).isEmpty()
        }
        assertTrue(alarms(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING).containsKey(friday))
    }

    @Test
    fun overdueCandleAlarmSurvivesAndNothingIsPosted() {
        startKoinWithRebuilder()
        instant = ZonedDateTime.of(friday, LocalTime.of(8, 0), nyZone).toInstant()
        runBlocking { rebuilder.rebuild() }
        val candle = alarms(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING).getValue(friday).single()

        // 09:35 in New York, past the 09:30 notice Android has not delivered yet
        instant = ZonedDateTime.of(friday, LocalTime.of(9, 35), nyZone).toInstant()
        setDeviceZone(moscowZone)
        broadcast(Intent.ACTION_TIMEZONE_CHANGED)
        val saturday = friday.plusDays(1)
        awaitUntil { morningTriggers()[saturday] == sevenAm(saturday, moscowZone) }

        val kept = alarms(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING).getValue(friday).single()
        assertSame("same pending alarm, untouched", candle.operation, kept.operation)
        assertEquals(candle.triggerAtMs, kept.triggerAtMs)
        assertEquals("not posted by the rebuild", 0, postedCount())
        assertEquals(LocalDate.of(2000, 1, 1), runBlocking { deliveryLog.lastCandleLightingDate() })
    }
}
