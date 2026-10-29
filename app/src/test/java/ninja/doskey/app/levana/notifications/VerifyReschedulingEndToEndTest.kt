package ninja.doskey.app.levana.notifications

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.fixedClock
import ninja.doskey.app.levana.domain.model.Location
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
import org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Verifier: drives real persisted preferences (DataStore via PreferencesRepository) through the
 * production NotificationRescheduling observer and checks the AlarmManager state it leaves.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class VerifyReschedulingEndToEndTest {

    private val app: Context get() = RuntimeEnvironment.getApplication()
    private val zmanim = ZmanimRepository()
    private val calendar = CalendarRepository()
    private lateinit var repo: PreferencesRepository
    private lateinit var scheduler: NotificationAlarmScheduler
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val newYork = Location(40.7128, -74.0060, 0.0, "America/New_York", "New York")
    private val london = Location(51.5074, -0.1278, 0.0, "Europe/London", "London")

    // A future Friday inside the Omer, so candle-lighting and both Omer alarms are all due
    private val date: LocalDate = generateSequence(LocalDate.of(2030, 3, 1)) { it.plusDays(1) }
        .first {
            it.dayOfWeek == DayOfWeek.FRIDAY && zmanim.hasCandles(it) &&
                calendar.getDayInfo(it, inIsrael = false).omerDay != null
        }

    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)
    private val alarms: List<ScheduledAlarm> get() = shadowOf(alarmManager).scheduledAlarms

    private fun actions() = alarms.map { it.operation }.map { shadowOf(it).savedIntent.action }

    /** Alarms for [date]; the rebuilder also plans the days after it. */
    private val alarmsOnDate: List<ScheduledAlarm>
        get() = alarms.filter {
            shadowOf(it.operation).savedIntent
                .getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0) == date.toEpochDay()
        }

    private fun alarmFor(action: String): ScheduledAlarm? =
        alarmsOnDate.firstOrNull { shadowOf(it.operation).savedIntent.action == action }

    private fun millis(time: LocalTime, zone: String) =
        ZonedDateTime.of(date, time, ZoneId.of(zone)).toInstant().toEpochMilli()

    private fun awaitCondition(what: String, check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (runCatching(check).getOrDefault(false)) return
            Thread.sleep(25)
        }
        fail("Timed out waiting for: $what; alarms=${actions()}")
    }

    /** Waits long enough that a rebuild (if any were triggered) would have happened. */
    private fun settle() = Thread.sleep(600)

    private fun save(block: suspend PreferencesRepository.() -> Unit) = runBlocking { repo.block() }

    @Before
    fun setUp() {
        repo = PreferencesRepository(app, CityRepository(app))
        scheduler = NotificationAlarmScheduler(app)
        // The device is in New York; its clock reads the start of [date]
        val rebuilder = NotificationAlarmRebuilder(repo.preferences, zmanim, calendar, scheduler) {
            fixedClock(date, LocalTime.MIDNIGHT, ZoneId.of(newYork.timezoneId))
        }
        startKoin {
            modules(module {
                single { repo }
                single { rebuilder }
            })
        }
        // Baseline persisted state before observation begins
        save {
            // DataStore is a per-JVM singleton, so clear state left by earlier tests
            preferences.first().savedLocations.forEach { removeSavedLocation(it.id) }
            saveCandleLightingOffset(18.0)
            updateCurrentLocation(newYork)
            setActiveLocation(null)
            saveIsInIsrael(false)
            saveDevDayShift(0)
            saveNotifyCandleLighting(false)
            saveNotifyOmer(false)
            saveNotifyOmerTzait(true)
            saveNotifyOmerMorning(false)
            saveCandleLightingNotifyMode("morning")
            saveCandleLightingMorningTime(480)
            saveCandleLightingHoursBefore(2)
            saveNotifyOmerMorningTime(420)
        }
        shadowOf(app as Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        // Same observer the production object runs, wired to this test's instances (the
        // production object caches its Koin injections for the whole JVM; it is exercised in
        // VerifyProductionObserverTest)
        scope.launch { rebuildOnAlarmSettingsChange(repo.preferences) { rebuilder.rebuild() } }
        settle()
    }

    @After
    fun tearDown() {
        scope.cancel()
        stopKoin()
        // The DataStore outlives this test; restore what other suites may read
        save {
            saveIsInIsrael(false)
            saveDevDayShift(0)
            saveCandleLightingOffset(18.0)
            saveNotifyCandleLighting(false)
            saveNotifyOmer(false)
            saveSelectedZmanim(setOf("Sunrise", "Sunset", "Nightfall"))
            saveCalendarHebrewMode(false)
            saveDynamicHolidayTheme(true)
            saveShowModernIsraeliHolidays(true)
            saveDevForceHolidayTheme(null)
        }
    }

    @Test
    fun persistedSettingChangesRebuildAndCancelAlarms() {
        assertTrue("nothing enabled yet", alarms.isEmpty())
        val candle = zmanim.getShabbatInfo(date, newYork, 18.0).candleLightingTime!!

        // Enable candle lighting (morning mode, 08:00 NY)
        save { saveNotifyCandleLighting(true) }
        awaitCondition("candle alarm at 08:00 NY") {
            alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)?.triggerAtTime ==
                millis(LocalTime.of(8, 0), newYork.timezoneId)
        }

        // Change morning time
        save { saveCandleLightingMorningTime(9 * 60 + 30) }
        awaitCondition("candle alarm moved to 09:30") {
            alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)?.triggerAtTime ==
                millis(LocalTime.of(9, 30), newYork.timezoneId)
        }
        assertEquals(1, alarmsOnDate.size)

        // Switch reminder type to hours_before (2h)
        save { saveCandleLightingNotifyMode("hours_before") }
        awaitCondition("candle alarm at candle-2h") {
            alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)?.triggerAtTime ==
                millis(candle.minusHours(2), newYork.timezoneId)
        }

        // Change hours before
        save { saveCandleLightingHoursBefore(4) }
        awaitCondition("candle alarm at candle-4h") {
            alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)?.triggerAtTime ==
                millis(candle.minusHours(4), newYork.timezoneId)
        }

        // Candle-lighting offset (minutes before sunset) changes candle time and thus trigger
        save { saveCandleLightingOffset(40.0) }
        val candle40 = zmanim.getShabbatInfo(date, newYork, 40.0).candleLightingTime!!
        awaitCondition("candle alarm follows offset") {
            alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)?.triggerAtTime ==
                millis(candle40.minusHours(4), newYork.timezoneId)
        }

        // TASK-5: reminders follow the device; selecting a city (previously it moved the
        // alarm to that city's candle lighting) changes nothing
        val candleNy40 = alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)!!.triggerAtTime
        save { addSavedLocation(london) }
        settle()
        assertEquals(candleNy40, alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)!!.triggerAtTime)
        assertEquals(1, alarmsOnDate.size)

        // A device-position update moves it, even while a saved city is selected
        save { updateCurrentLocation(newYork.copy(latitude = 40.8)) }
        val nyMoved = newYork.copy(latitude = 40.8)
        val candleNyMoved = zmanim.getShabbatInfo(date, nyMoved, 40.0).candleLightingTime!!
        awaitCondition("candle alarm follows the device position") {
            val a = alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)
            a?.triggerAtTime == millis(candleNyMoved.minusHours(4), newYork.timezoneId) &&
                shadowOf(a.operation).savedIntent.getStringExtra(
                    NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME
                ) == candleNyMoved.toString()
        }

        // Switching the selected city back to the current location changes nothing
        val before = alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)!!.triggerAtTime
        save { setActiveLocation(null) }
        settle()
        assertEquals(before, alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)!!.triggerAtTime)

        // The device moves to London
        save { updateCurrentLocation(london.copy(name = "GPS")) }
        val candleLondon = zmanim.getShabbatInfo(date, london, 40.0).candleLightingTime!!
        awaitCondition("candle alarm follows a GPS update of the device position") {
            alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)?.triggerAtTime ==
                millis(candleLondon.minusHours(4), london.timezoneId)
        }

        // Adding, selecting and removing a saved city never moves it
        save { addSavedLocation(newYork) }
        settle()
        val nyId = runBlocking { repo.preferences.first().savedLocations.last().id }
        save { setActiveLocation(nyId) }
        settle()
        save { removeSavedLocation(nyId) }
        settle()
        assertEquals(
            millis(candleLondon.minusHours(4), london.timezoneId),
            alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)!!.triggerAtTime
        )

        // Omer: enable category (tzait sub-option defaults on)
        save { saveNotifyOmer(true) }
        awaitCondition("omer tzait alarm") {
            alarmFor(NotificationAlarmScheduler.ACTION_OMER) != null
        }
        val tzait = zmanim.getTzaitTime(date, london.copy(name = "GPS"))!!
        assertEquals(
            millis(tzait, london.timezoneId),
            alarmFor(NotificationAlarmScheduler.ACTION_OMER)!!.triggerAtTime
        )
        assertTrue(alarmFor(NotificationAlarmScheduler.ACTION_OMER_MORNING) == null)

        // Omer morning sub-option on (the device clock is in New York)
        save { saveNotifyOmerMorning(true) }
        awaitCondition("omer morning alarm at 07:00") {
            alarmFor(NotificationAlarmScheduler.ACTION_OMER_MORNING)?.triggerAtTime ==
                millis(LocalTime.of(7, 0), newYork.timezoneId)
        }
        // Omer morning time change
        save { saveNotifyOmerMorningTime(6 * 60 + 15) }
        awaitCondition("omer morning alarm moved to 06:15") {
            alarmFor(NotificationAlarmScheduler.ACTION_OMER_MORNING)?.triggerAtTime ==
                millis(LocalTime.of(6, 15), newYork.timezoneId)
        }
        assertEquals(3, alarmsOnDate.size)

        // Omer tzait sub-option off cancels only that alarm
        save { saveNotifyOmerTzait(false) }
        awaitCondition("omer tzait cancelled") { alarmFor(NotificationAlarmScheduler.ACTION_OMER) == null }
        assertTrue(alarmFor(NotificationAlarmScheduler.ACTION_OMER_MORNING) != null)
        assertTrue(alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING) != null)

        // Omer master off cancels the morning one too, keeps candle lighting
        save { saveNotifyOmer(false) }
        awaitCondition("omer morning cancelled") {
            alarmFor(NotificationAlarmScheduler.ACTION_OMER_MORNING) == null
        }
        assertEquals(setOf(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING), actions().toSet())

        // Candle lighting off cancels everything
        save { saveNotifyCandleLighting(false) }
        awaitCondition("all cancelled") { alarms.isEmpty() }

        // Rapid toggles settle on the final persisted value
        save {
            saveNotifyCandleLighting(true)
            saveNotifyCandleLighting(false)
            saveNotifyOmer(true)
            saveNotifyOmer(false)
            saveNotifyCandleLighting(true)
        }
        awaitCondition("final state: only candle") {
            actions().toSet() == setOf(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)
        }
        settle()
        assertEquals(setOf(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING), actions().toSet())
    }

    @Test
    fun unrelatedPreferenceChangesNeitherRebuildNorPost() {
        save { saveNotifyCandleLighting(true) }
        awaitCondition("candle alarm") {
            alarmFor(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING) != null
        }
        // Sentinel Omer alarm for another date: any rebuild would cancel it, Omer being off
        val other = date.plusDays(7)
        val planned = alarms.size
        scheduler.scheduleOmerReminder(other, LocalTime.NOON, newYork, 5)
        assertEquals(planned + 1, alarms.size)

        save {
            saveNotifyHolidays(true)
            saveHolidayNotifyDaysBefore(3)
            saveNotifyFasts(true)
            saveNotifyPersonalEvents(true)
            saveSelectedZmanim(setOf("Sunrise"))
            saveCalendarHebrewMode(true)
            saveDynamicHolidayTheme(false)
            saveShowModernIsraeliHolidays(false)
            saveDevForceHolidayTheme("CHANUKAH")
            saveNotifyHolidays(false)
            saveNotifyPersonalEvents(false)
        }
        settle()
        assertEquals("no rebuild on unrelated prefs", planned + 1, alarms.size)
        assertTrue(NotificationAlarmScheduler.ACTION_OMER in actions())

        // Israel/Diaspora is alarm-relevant: it must rebuild (sentinel cancelled)
        save { saveIsInIsrael(true) }
        awaitCondition("rebuild on Israel change") {
            NotificationAlarmScheduler.ACTION_OMER !in actions()
        }

        val nm = app.getSystemService(NotificationManager::class.java)
        assertTrue(
            "no notification posted by unrelated pref changes",
            shadowOf(nm).allNotifications.isEmpty()
        )
    }

    @Test
    fun receiverDropsStaleAlarmAfterCategoryDisabled() {
        save {
            saveNotifyCandleLighting(true)
            saveNotifyOmer(true)
            saveNotifyOmerMorning(true)
        }
        awaitCondition("three alarms") { alarmsOnDate.size == 3 }
        val intents = alarmsOnDate.map { Intent(shadowOf(it.operation).savedIntent) }

        // Capture the pending intents as they'd be delivered, then disable everything
        save {
            saveNotifyCandleLighting(false)
            saveNotifyOmer(false)
        }
        awaitCondition("cancelled") { alarms.isEmpty() }

        val prefs = runBlocking { repo.preferences.first() }
        val nm = app.getSystemService(NotificationManager::class.java)
        NotificationChannels.createAll(app)
        intents.forEach { deliverIfEnabled(app, it, prefs) }
        assertTrue(shadowOf(nm).allNotifications.isEmpty())

        // Sanity: with categories on, the same intents do post
        save {
            saveNotifyCandleLighting(true)
            saveNotifyOmer(true)
        }
        val enabled = runBlocking { repo.preferences.first() }
        intents.forEach { deliverIfEnabled(app, it, enabled) }
        assertEquals(3, shadowOf(nm).allNotifications.size)
    }

    @Test
    fun receiverOnReceiveReadsPersistedPrefs() {
        save { saveNotifyOmer(true); saveNotifyOmerTzait(false); saveNotifyOmerMorning(true) }
        NotificationChannels.createAll(app)
        val nm = app.getSystemService(NotificationManager::class.java)
        val tzaitIntent = Intent(app, NotificationAlarmReceiver::class.java).apply {
            action = NotificationAlarmScheduler.ACTION_OMER
            putExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
            putExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 10)
        }
        val morningIntent = Intent(tzaitIntent).apply {
            action = NotificationAlarmScheduler.ACTION_OMER_MORNING
        }
        NotificationAlarmReceiver().onReceive(app, tzaitIntent)
        settle()
        assertTrue("tzait sub-option off → dropped", shadowOf(nm).allNotifications.isEmpty())
        NotificationAlarmReceiver().onReceive(app, morningIntent)
        awaitCondition("morning omer posted") { shadowOf(nm).allNotifications.size == 1 }
    }
}
