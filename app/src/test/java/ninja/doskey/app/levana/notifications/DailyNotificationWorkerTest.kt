package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.work.Data
import androidx.work.ForegroundUpdater
import androidx.work.ProgressUpdater
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.device.fixedClock
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.domain.model.IsraelMode
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.notifications.DailyCategory.FASTS
import ninja.doskey.app.levana.notifications.DailyCategory.HOLIDAYS
import ninja.doskey.app.levana.notifications.DailyCategory.PERSONAL_EVENTS
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
import java.time.LocalDate
import java.time.LocalTime
import java.util.GregorianCalendar
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Saved preferences (real DataStore) drive the production change detection, which runs the real
 * [DailyNotificationWorker] for the categories it reports.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DailyNotificationWorkerTest {

    private val app: Context get() = RuntimeEnvironment.getApplication()
    private val calendar = CalendarRepository()
    private lateinit var repo: PreferencesRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshes = CopyOnWriteArrayList<Set<DailyCategory>>()

    private val newYork = Location(40.7128, -74.0060, 0.0, "America/New_York", "New York")
    private val london = Location(51.5074, -0.1278, 0.0, "Europe/London", "London")

    // A holiday followed by a fast (Rosh Hashana → Tzom Gedaliah), in the future
    private val date: LocalDate = generateSequence(LocalDate.of(2030, 1, 1)) { it.plusDays(1) }
        .first { day ->
            calendar.getDayInfo(day, inIsrael = false).holidays.any { it.category == HolidayCategory.TORAH } &&
                calendar.getDayInfo(day.plusDays(1), inIsrael = false).holidays.any { it.category == HolidayCategory.FAST }
        }

    private val anniversary = JewishDate(GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth))
        .let { PersonalEvent(1, "Anniversary", it.jewishDayOfMonth, it.jewishMonth, it.jewishYear - 10) }

    private val dao = object : PersonalEventDao {
        override fun getAll(): Flow<List<PersonalEvent>> = flowOf(listOf(anniversary))
        override suspend fun getById(id: Long) = anniversary.takeIf { it.id == id }
        override suspend fun getByHebrewDate(month: Int, day: Int) = listOf(anniversary)
        override suspend fun getAllOnce() = listOf(anniversary)
        override suspend fun insert(event: PersonalEvent) = error("read-only")
        override suspend fun update(event: PersonalEvent) = error("read-only")
        override suspend fun delete(event: PersonalEvent) = error("read-only")
    }

    private val notificationManager get() = app.getSystemService(NotificationManager::class.java)

    private fun notification(channel: String): Notification? =
        shadowOf(notificationManager).allNotifications.singleOrNull { it.channelId == channel }

    private val holidayId = NotificationChannels.HOLIDAYS
    private val fastId = NotificationChannels.FASTS
    private val personalId = NotificationChannels.PERSONAL_EVENTS

    private fun save(block: suspend PreferencesRepository.() -> Unit) = runBlocking { repo.block() }

    private fun runWorker(categories: Set<DailyCategory>?) = runBlocking {
        val direct = Executor { it.run() }
        val params = WorkerParameters(
            UUID.randomUUID(),
            categories?.let { DailyCategory.toInputData(it) } ?: Data.EMPTY,
            emptyList(),
            WorkerParameters.RuntimeExtras(),
            0,
            0,
            direct,
            Dispatchers.Unconfined,
            WorkManagerTaskExecutor(direct),
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ) = null
            },
            ProgressUpdater { _, _, _ -> error("unused") },
            ForegroundUpdater { _, _, _ -> error("unused") }
        )
        DailyNotificationWorker(app, params).doWork()
    }

    private fun awaitCondition(what: String, check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (check()) return
            Thread.sleep(20)
        }
        fail("Timed out waiting for: $what; refreshes=$refreshes")
    }

    /** Long enough for a refresh to have run had one been triggered. */
    private fun settle() = Thread.sleep(500)

    @Before
    fun setUp() {
        repo = PreferencesRepository(app, CityRepository(app))
        val zmanim = ZmanimRepository()
        // The device clock reads the start of [date]
        val device = DeviceContext(repo.preferences) { fixedClock(date, LocalTime.MIDNIGHT) }
        val rebuilder = NotificationAlarmRebuilder(
            repo.preferences, device, zmanim, calendar, NotificationAlarmScheduler(app)
        )
        startKoin {
            modules(module {
                single { repo }
                single { device }
                single { zmanim }
                single { calendar }
                single { PersonalEventRepository(dao) }
                single { ContactBirthdayRepository(app) }
                single { rebuilder }
                single { NotificationDeliveryLog(app) }
            })
        }
        save {
            // DataStore is shared by the whole JVM, so reset what earlier tests may have saved
            preferences.first().savedLocations.forEach { removeSavedLocation(it.id) }
            updateCurrentLocation(newYork)
            setActiveLocation(null)
            saveIsraelMode(IsraelMode.DIASPORA)
            saveDevDayShift(0)
            saveNotifyHolidays(false)
            saveHolidayNotifyDaysBefore(1)
            saveNotifyFasts(false)
            saveNotifyPersonalEvents(false)
            saveNotifyCandleLighting(false)
            saveNotifyOmer(false)
        }
        shadowOf(app as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(app)
    }

    @After
    fun tearDown() {
        scope.cancel()
        stopKoin()
    }

    private fun observe() {
        scope.launch {
            refreshOnDailySettingsChange(repo.preferences, refresh = { categories ->
                runWorker(categories)
                refreshes += categories
            })
        }
        settle()
    }

    @Test
    fun savedSettingChangesRepostOnlyTheAffectedCategory() {
        observe()
        assertEquals(emptyList<Notification>(), shadowOf(notificationManager).allNotifications)

        save { saveNotifyHolidays(true) }
        awaitCondition("holiday posted") { notification(holidayId) != null }
        assertEquals(listOf(setOf(HOLIDAYS)), refreshes)
        assertNull("fasts are still off", notification(fastId))
        assertNull("personal events are still off", notification(personalId))
        val holiday = notification(holidayId)

        save { saveNotifyFasts(true) }
        awaitCondition("fast posted") { notification(fastId) != null }
        save { saveNotifyPersonalEvents(true) }
        awaitCondition("personal event posted") { notification(personalId) != null }
        assertSame("holiday not re-posted", holiday, notification(holidayId))
        val fast = notification(fastId)
        val personal = notification(personalId)

        // Unrelated settings post nothing
        save {
            saveShowDeveloperSettings(false)
            saveDynamicHolidayTheme(false)
            saveCalendarHebrewMode(true)
            saveSelectedZmanim(setOf("Sunrise"))
            saveNotifyCandleLighting(true)
            saveNotifyCandleLighting(false)
        }
        settle()
        assertEquals(3, refreshes.size)
        assertSame(holiday, notification(holidayId))
        assertSame(fast, notification(fastId))
        assertSame(personal, notification(personalId))

        save { saveHolidayNotifyDaysBefore(3) }
        awaitCondition("holidays refreshed") { notification(holidayId) !== holiday }
        assertEquals(setOf(HOLIDAYS), refreshes.last())
        assertSame(fast, notification(fastId))
        assertSame(personal, notification(personalId))
        val holiday2 = notification(holidayId)

        // TASK-5: fast times follow the device position; selecting a city changes nothing
        save { addSavedLocation(london) }
        settle()
        assertSame(fast, notification(fastId))
        save { updateCurrentLocation(london) }
        awaitCondition("fasts follow the device position") { notification(fastId) !== fast }
        assertEquals(setOf(FASTS), refreshes.last())
        assertSame(holiday2, notification(holidayId))
        assertSame(personal, notification(personalId))
        val fast2 = notification(fastId)

        save { saveIsraelMode(IsraelMode.ISRAEL) }
        awaitCondition("Israel refreshes holidays and fasts") {
            notification(holidayId) !== holiday2 && notification(fastId) !== fast2
        }
        assertEquals(setOf(HOLIDAYS, FASTS), refreshes.last())
        assertSame(personal, notification(personalId))

        // Switching a category off posts nothing
        val count = refreshes.size
        save { saveNotifyPersonalEvents(false) }
        settle()
        assertEquals(count, refreshes.size)
    }

    // TASK-5 case 7: the fast notice uses the device date and the device position's times
    @Test
    fun fastNoticeTimesAreTheDevicePositionsNotTheSelectedCitys() {
        fun body() = notification(fastId)!!.let {
            (it.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: it.extras.getCharSequence(Notification.EXTRA_TEXT)).toString()
        }
        save { saveNotifyFasts(true) }
        runWorker(setOf(FASTS))
        val inNewYork = body()

        save { addSavedLocation(Location.JERUSALEM) }
        runWorker(setOf(FASTS))
        assertEquals("selecting Jerusalem changes nothing", inNewYork, body())

        save { updateCurrentLocation(london) }
        runWorker(setOf(FASTS))
        assertNotEquals("the device moved", inNewYork, body())
    }

    @Test
    fun aStaleRunDoesNotPostADisabledCategory() {
        save {
            saveNotifyHolidays(true)
            saveNotifyFasts(true)
            saveNotifyPersonalEvents(true)
        }
        runWorker(null)
        assertEquals(3, shadowOf(notificationManager).allNotifications.size)

        save { saveNotifyFasts(false) }
        notificationManager.cancelAll()

        // A fasts refresh enqueued before fasts were switched off
        runWorker(setOf(FASTS))
        assertEquals(emptyList<Notification>(), shadowOf(notificationManager).allNotifications)

        // The daily run still posts the categories that remain on
        runWorker(null)
        assertNull(notification(fastId))
        assertNotNull(notification(holidayId))
        assertNotNull(notification(personalId))
    }

    @Test
    fun aRefreshRunPostsOnlyItsCategories() {
        save {
            saveNotifyHolidays(true)
            saveNotifyFasts(true)
            saveNotifyPersonalEvents(true)
        }
        runWorker(setOf(PERSONAL_EVENTS))
        assertEquals(1, shadowOf(notificationManager).allNotifications.size)
        assertNotNull(notification(personalId))
    }

    @Test
    fun inputDataRoundTrips() {
        assertNull(DailyCategory.fromInputData(Data.EMPTY))
        assertEquals(
            setOf(HOLIDAYS, FASTS),
            DailyCategory.fromInputData(DailyCategory.toInputData(setOf(HOLIDAYS, FASTS)))
        )
    }
}
