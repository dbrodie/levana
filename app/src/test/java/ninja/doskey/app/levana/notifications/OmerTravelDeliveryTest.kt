package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * An Omer notice is posted once per (evening/morning, Omer year, count), across westward
 * time-zone changes, rebuilds and process restarts (levana-1bl). Each test uses its own
 * delivery log file; the app's singleton DataStore outlives one Robolectric test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class OmerTravelDeliveryTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val notificationManager get() = app.getSystemService(NotificationManager::class.java)
    private var storage = TestDataStore()
    private val log get() = NotificationDeliveryLog(storage.store)

    private val prefs = UserPreferences(
        notifyCandleLighting = false,
        notifyOmer = true,
        notifyOmerTzait = true,
        notifyOmerMorning = true,
        notifyOmerMorningTime = 7 * 60
    )

    /** Lag BaOmer 5787 is Tue 25 May 2027: its evening reminder is dated 24 May. */
    private val may24 = LocalDate.of(2027, 5, 24)
    private val may25 = LocalDate.of(2027, 5, 25)

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(app)
        // Counts used below, checked against the Hebrew calendar rather than assumed
        val calendar = CalendarRepository()
        assertEquals(33, calendar.getDayInfo(may25, inIsrael = false).omerDay)
        assertEquals(33, calendar.getDayInfo(LocalDate.of(2028, 5, 14), inIsrael = false).omerDay)
        assertEquals(33, calendar.getDayInfo(LocalDate.of(2026, 5, 5), inIsrael = false).omerDay)
    }

    private fun intent(action: String, date: LocalDate, omerDay: Int) =
        Intent(app, NotificationAlarmReceiver::class.java).apply {
            this.action = action
            data = NotificationAlarmScheduler.alarmUri(action, date)
            putExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
            putExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, omerDay)
        }

    private fun evening(date: LocalDate, omerDay: Int) = intent(NotificationAlarmScheduler.ACTION_OMER, date, omerDay)
    private fun morning(date: LocalDate, omerDay: Int) = intent(NotificationAlarmScheduler.ACTION_OMER_MORNING, date, omerDay)

    private fun deliver(intent: Intent, settings: UserPreferences = prefs) =
        runBlocking { deliverAlarm(app, intent, settings, log) }

    private fun shown() = shadowOf(notificationManager).allNotifications.size

    /** The user dismisses everything, so a repost would be visible again. */
    private fun dismissAll() = notificationManager.cancelAll()

    @Test
    fun `a delivered notice is not reposted, distinct notices still are`() {
        assertTrue(deliver(evening(may24, 33)))
        dismissAll()

        // The same evening notice planned again (e.g. after a westward location change)
        assertFalse(deliver(evening(may24, 33)))
        assertEquals(0, shown())

        // The morning notice of the same count is intentionally separate
        assertTrue(deliver(morning(may25, 33)))
        assertFalse(deliver(morning(may25, 33)))
        // The next count, evening and morning
        assertTrue(deliver(evening(may25, 34)))
        assertTrue(deliver(morning(LocalDate.of(2027, 5, 26), 34)))
        // Next year's count 33 (5788) is a new notice
        assertTrue(deliver(evening(LocalDate.of(2028, 5, 13), 33)))
        assertTrue(deliver(morning(LocalDate.of(2028, 5, 14), 33)))
        assertEquals(5, shown())
    }

    @Test
    fun `nothing is recorded when nothing could be posted`() {
        // Category off
        assertFalse(deliver(evening(may24, 33), prefs.copy(notifyOmerTzait = false)))
        assertFalse(deliver(morning(may25, 33), prefs.copy(notifyOmer = false)))
        // No notification permission
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(deliver(evening(may24, 33)))
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        // All notifications turned off for the app
        shadowOf(notificationManager).setNotificationsEnabled(false)
        assertFalse(deliver(morning(may25, 33)))
        shadowOf(notificationManager).setNotificationsEnabled(true)
        dismissAll()

        // Once posting is possible, both notices are delivered
        assertTrue(deliver(evening(may24, 33)))
        assertTrue(deliver(morning(may25, 33)))
        assertEquals(2, shown())

        // Omer channel blocked by the user: not recorded as delivered either
        val channel = notificationManager.getNotificationChannel(NotificationChannels.OMER)
        notificationManager.deleteNotificationChannel(channel.id)
        notificationManager.createNotificationChannel(
            NotificationChannel(channel.id, channel.name, NotificationManager.IMPORTANCE_NONE)
        )
        val next = OmerDelivery.of(evening = true, date = may25, omerDay = 34)
        assertFalse(deliver(evening(may25, 34)))
        var postedLater = false
        runBlocking { log.deliverOmerOnce(next) { postedLater = true; true } }
        assertTrue(postedLater)
    }

    @Test
    fun `delivery is remembered across a process restart`() {
        assertTrue(deliver(morning(may25, 33)))
        storage = storage.reopen()
        dismissAll()
        assertFalse(deliver(morning(may25, 33)))
        assertEquals(0, shown())
    }

    @Test
    fun `concurrent deliveries of one notice post it once`() {
        val posts = AtomicInteger()
        val notice = OmerDelivery.of(evening = true, date = may24, omerDay = 33)
        val results = runBlocking(Dispatchers.Default) {
            (1..32).map {
                async { log.deliverOmerOnce(notice) { Thread.sleep(5); posts.incrementAndGet(); true } }
            }.awaitAll()
        }
        assertEquals(1, posts.get())
        assertEquals(1, results.count { it })
    }

    @Test
    fun `old years are pruned, the previous year is kept`() {
        val y5786 = morning(LocalDate.of(2026, 5, 5), 33)
        val y5787 = morning(may25, 33)
        assertTrue(deliver(y5786))
        assertTrue(deliver(y5787))
        assertTrue(deliver(morning(LocalDate.of(2028, 5, 14), 33))) // 5788 prunes 5786
        dismissAll()
        assertFalse(deliver(y5787))
        assertTrue(deliver(y5786))
    }

    @Test
    fun `westward travel replans the delivered morning but does not repost it`() {
        val tokyo = Location(35.6762, 139.6503, timezoneId = "Asia/Tokyo", name = "Tokyo")
        val tokyoZone = ZoneId.of(tokyo.timezoneId)
        val honoluluZone = ZoneId.of("Pacific/Honolulu")
        val alarmManager = app.getSystemService(AlarmManager::class.java)
        val scheduler = NotificationAlarmScheduler(app, ScheduledAlarmRegistry(TestDataStore().store)) { now.toEpochMilli() }

        fun plan(at: ZonedDateTime) = planReminderAlarms(
            at.toLocalDate(), at.toInstant(), at.zone, tokyo, prefs, ZmanimRepository(), CalendarRepository()
        )
        fun pendingMorning(date: LocalDate) = shadowOf(alarmManager).scheduledAlarms.singleOrNull {
            val i = shadowOf(it.operation).savedIntent
            i.action == NotificationAlarmScheduler.ACTION_OMER_MORNING &&
                i.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0) == date.toEpochDay()
        }

        // 25 May 07:30 in Tokyo; the 07:00 morning notice (count 33) has been delivered
        val tokyoMorning = ZonedDateTime.of(may25, LocalTime.of(7, 30), tokyoZone)
        now = tokyoMorning.toInstant()
        runBlocking { scheduler.scheduleAll(plan(tokyoMorning)) }
        assertTrue(deliver(morning(may25, 33)))
        dismissAll()

        // Flying to Honolulu: it is 24 May 12:30 there, so 25 May's morning is ahead again
        val honolulu = tokyoMorning.withZoneSameInstant(honoluluZone)
        runBlocking { scheduler.scheduleAll(plan(honolulu)) }
        val replanned = pendingMorning(may25)!!
        assertEquals(ZonedDateTime.of(may25, LocalTime.of(7, 0), honoluluZone).toInstant().toEpochMilli(), replanned.triggerAtMs)

        // When it fires the same notice is not posted again; the next count still is
        assertFalse(deliver(shadowOf(replanned.operation).savedIntent))
        assertEquals(0, shown())
        assertTrue(deliver(morning(LocalDate.of(2027, 5, 26), 34)))
    }

    private var now: Instant = Instant.EPOCH
}
