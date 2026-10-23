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
import org.junit.Assert.assertTrue
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
import java.util.TimeZone

/**
 * Alarms left outside the planning window by date-changing time-zone moves and clock changes
 * are reconciled through the scheduled-alarm registry (levana-clp). Runs the production
 * rebuilder and scheduler against Robolectric's AlarmManager, which keeps an alarm until it is
 * cancelled (it never fires them), so a pending past alarm stands for an undelivered one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ShiftedWindowReconciliationTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)
    private val zmanim = ZmanimRepository()
    private val savedZone = TimeZone.getDefault()

    private val tokyoZone = ZoneId.of("Asia/Tokyo")
    private val honoluluZone = ZoneId.of("Pacific/Honolulu")
    private val nyZone = ZoneId.of("America/New_York")
    private val tokyo = Location(35.6762, 139.6503, timezoneId = tokyoZone.id, name = "Tokyo")
    private val honolulu = Location(21.3069, -157.8583, timezoneId = honoluluZone.id, name = "Honolulu")
    private val newYork = Location(40.7128, -74.0060, timezoneId = nyZone.id, name = "New York")

    private val omer = UserPreferences(
        isInIsrael = false,
        notifyCandleLighting = false,
        notifyOmer = true,
        notifyOmerTzait = true,
        notifyOmerMorning = true,
        notifyOmerMorningTime = 7 * 60
    )
    private val omerAndCandles = omer.copy(
        notifyCandleLighting = true,
        candleLightingNotifyMode = "hours_before",
        candleLightingHoursBefore = 2
    )

    private var instant: Instant = Instant.EPOCH
    private var deviceZone: ZoneId = tokyoZone
    private val prefs = MutableStateFlow(omer)
    private var storage = TestDataStore()

    @After
    fun tearDown() {
        TimeZone.setDefault(savedZone)
    }

    private fun scheduler(registry: ScheduledAlarmRegistry = ScheduledAlarmRegistry(storage.store)) =
        NotificationAlarmScheduler(app, registry) { instant.toEpochMilli() }

    /** A settings / time-change rebuild at [at] in [zone], the device's zone, with [location]. */
    private fun rebuild(
        at: ZonedDateTime,
        location: Location,
        settings: UserPreferences = prefs.value,
        alarmScheduler: NotificationAlarmScheduler = scheduler()
    ): NotificationAlarmScheduler {
        instant = at.toInstant()
        deviceZone = at.zone
        TimeZone.setDefault(TimeZone.getTimeZone(at.zone))
        prefs.value = settings.copy(currentLocation = location)
        val rebuilder = NotificationAlarmRebuilder(
            prefs, zmanim, CalendarRepository(), alarmScheduler
        ) { Clock.fixed(instant, deviceZone) }
        runBlocking { rebuilder.rebuild() }
        return alarmScheduler
    }

    private data class Pending(val action: String, val date: LocalDate, val triggerAt: Instant)

    private fun pending(): List<Pending> = shadowOf(alarmManager).scheduledAlarms.map {
        val intent = shadowOf(it.operation).savedIntent
        Pending(
            intent.action!!,
            LocalDate.ofEpochDay(intent.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0)),
            Instant.ofEpochMilli(it.triggerAtMs)
        )
    }

    private fun pending(action: String) = pending().filter { it.action == action }

    private fun assertNoDuplicates() {
        val keys = pending().map { it.action to it.date }
        assertEquals(keys.size, keys.toSet().size)
    }

    private fun may(day: Int) = LocalDate.of(2027, 5, day)
    private fun sevenAm(date: LocalDate, zone: ZoneId) =
        ZonedDateTime.of(date, LocalTime.of(7, 0), zone).toInstant()

    private val morning = NotificationAlarmScheduler.ACTION_OMER_MORNING
    private val evening = NotificationAlarmScheduler.ACTION_OMER
    private val candles = NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING

    @Test
    fun `westward date shift cancels the morning alarm left after the new window`() {
        // Mon 3 May 2027 12:00 in Tokyo: mornings 4..10 May at 07:00 Tokyo
        rebuild(ZonedDateTime.of(may(3), LocalTime.NOON, tokyoZone), tokyo)
        assertEquals(sevenAm(may(10), tokyoZone), pending(morning).single { it.date == may(10) }.triggerAt)

        // Same instant in Honolulu is 2 May 17:00: the window ends on 9 May
        rebuild(ZonedDateTime.of(may(3), LocalTime.NOON, tokyoZone).withZoneSameInstant(honoluluZone), honolulu)

        // 07:00 Tokyo on 10 May would have fired at 12:00 on 9 May in Honolulu
        assertTrue(pending().none { it.date == may(10) })
        val mornings = pending(morning).filter { it.triggerAt.isAfter(instant) }
        assertEquals((3..9).map(::may), mornings.map { it.date }.sorted())
        mornings.forEach { assertEquals(sevenAm(it.date, honoluluZone), it.triggerAt) }
        pending(evening).filter { it.triggerAt.isAfter(instant) }.forEach {
            assertEquals(zmanim.getTzaitInstant(it.date, honolulu), it.triggerAt)
        }
        assertNoDuplicates()
    }

    @Test
    fun `eastward date shift cancels alarms left before the new window`() {
        // Sat 1 May 2027 06:00 in Honolulu: the 1 May morning alarm is still ahead (07:00)
        val start = ZonedDateTime.of(may(1), LocalTime.of(6, 0), honoluluZone)
        rebuild(start, honolulu)
        assertEquals(sevenAm(may(1), honoluluZone), pending(morning).single { it.date == may(1) }.triggerAt)

        // Same instant in Tokyo is 2 May 01:00: 1 May is before the window, and its morning
        // alarm would have said "today is" 1 May's count at 02:00 on 2 May
        rebuild(start.withZoneSameInstant(tokyoZone), tokyo)
        assertTrue(pending(morning).none { it.date == may(1) })
        val mornings = pending(morning)
        assertEquals((2..9).map(::may), mornings.map { it.date }.sorted())
        mornings.forEach { assertEquals(sevenAm(it.date, tokyoZone), it.triggerAt) }
        assertNoDuplicates()
    }

    @Test
    fun `manual clock jumps reconcile alarms far outside any fixed window`() {
        prefs.value = omerAndCandles
        rebuild(ZonedDateTime.of(LocalDate.of(2027, 4, 25), LocalTime.NOON, nyZone), newYork)
        assertTrue(pending().any { it.date == may(2) })

        // Forward 30 days (TIME_SET): nothing from April or early May survives
        rebuild(ZonedDateTime.of(may(25), LocalTime.NOON, nyZone), newYork)
        assertTrue(pending().none { it.date.isBefore(may(24)) })
        assertTrue(pending(morning).any { it.date == may(26) })

        // Back 30 days: the late-May alarms are now 4 weeks ahead and must go too
        rebuild(ZonedDateTime.of(LocalDate.of(2027, 4, 25), LocalTime.NOON, nyZone), newYork)
        assertTrue(pending().none { it.date.isAfter(may(2)) })
        assertTrue(pending(morning).any { it.date == may(2) })
        assertNoDuplicates()
    }

    @Test
    fun `disabling a category after a shift removes its out-of-window alarms only`() {
        prefs.value = omerAndCandles
        // Thu 6 May 2027 in Tokyo: Friday 7 May candle lighting and Omer up to 13 May
        rebuild(ZonedDateTime.of(may(6), LocalTime.of(8, 0), tokyoZone), tokyo)
        assertTrue(pending(morning).any { it.date == may(13) })
        assertTrue(pending(candles).any { it.date == may(7) })

        // Move west and turn the Omer off in the same settings change
        rebuild(
            ZonedDateTime.of(may(6), LocalTime.of(8, 0), tokyoZone).withZoneSameInstant(honoluluZone),
            honolulu,
            omerAndCandles.copy(notifyOmer = false)
        )
        assertTrue(pending(morning).isEmpty())
        assertTrue(pending(evening).isEmpty())
        val friday = pending(candles).single { it.date == may(7) }
        val expected = zmanim.getCandleLightingInstant(may(7), honolulu, 18.0, false)!!.minusSeconds(2 * 3600)
        assertEquals(expected, friday.triggerAt)
    }

    @Test
    fun `still-wanted alarms survive a shift`() {
        // Tokyo location; nightfall on 3 May has just passed, its alarm not yet delivered
        val tzait = zmanim.getTzaitInstant(may(3), tokyo)!!
        rebuild(tzait.minusSeconds(60).atZone(tokyoZone), tokyo)
        val undelivered = pending(evening).single { it.date == may(3) }

        // The device moves to Honolulu (date back to 3 May → 2 May), location unchanged
        rebuild(tzait.plusSeconds(300).atZone(honoluluZone), tokyo)
        assertEquals(undelivered, pending(evening).single { it.date == may(3) })

        // Device ahead of the location: Sat 8 May 10:00 Tokyo is Fri 7 May 15:00 Honolulu;
        // Honolulu's Friday candle lighting is still ahead and must stay wanted
        rebuild(
            ZonedDateTime.of(may(8), LocalTime.of(10, 0), tokyoZone), honolulu,
            omerAndCandles
        )
        val friday = pending(candles).single { it.date == may(7) }
        assertEquals(
            zmanim.getCandleLightingInstant(may(7), honolulu, 18.0, false)!!.minusSeconds(2 * 3600),
            friday.triggerAt
        )
        assertTrue(friday.triggerAt.isAfter(instant))
    }

    @Test
    fun `upgrade finds alarms the previous scheduler left outside the window`() {
        // A previous version scheduled these with the same identities but kept no registry
        val legacy = scheduler(ScheduledAlarmRegistry(TestDataStore().store))
        instant = ZonedDateTime.of(may(3), LocalTime.NOON, tokyoZone).toInstant()
        legacy.scheduleOmerMorningReminder(may(10), LocalTime.of(7, 0), omerDay = 19)
        legacy.scheduleOmerMorningReminder(may(30), LocalTime.of(7, 0), omerDay = 39)

        rebuild(ZonedDateTime.of(may(3), LocalTime.NOON, tokyoZone).withZoneSameInstant(honoluluZone), honolulu)

        // 10 May is within the one-time search around the window and is cancelled
        assertTrue(pending(morning).none { it.date == may(10) })
        // 30 May is beyond it: AlarmManager cannot list alarms, so it stays until that date
        // enters a window (documented limitation)
        assertTrue(pending(morning).any { it.date == may(30) })
        assertTrue(runBlocking { ScheduledAlarmRegistry(storage.store).isSwept() })
    }

    @Test
    fun `registry survives a process restart and stays bounded`() {
        rebuild(ZonedDateTime.of(may(3), LocalTime.NOON, nyZone), newYork)
        val before = pending().map { it.action to it.date }.toSet()
        assertTrue(before.isNotEmpty())

        // Process restart: a new DataStore and scheduler over the same file
        storage = storage.reopen()
        // Clock back 30 days: the old alarms are 4 weeks ahead, beyond the one-time search, so
        // only the persisted registry can find them
        rebuild(ZonedDateTime.of(LocalDate.of(2027, 4, 3), LocalTime.NOON, nyZone), newYork,
            omer.copy(notifyOmer = false))
        assertTrue(pending().isEmpty())
        assertTrue(runBlocking { ScheduledAlarmRegistry(storage.store).ids() }.isEmpty())

        // Bounded: after several shifts the registry holds exactly the last plan's alarms
        rebuild(ZonedDateTime.of(may(3), LocalTime.NOON, nyZone), newYork, omer)
        rebuild(ZonedDateTime.of(may(5), LocalTime.NOON, tokyoZone), tokyo, omer)
        val ids = runBlocking { ScheduledAlarmRegistry(storage.store).ids() }
        assertTrue(ids.containsAll(pending().map { AlarmId(it.action, it.date) }))
        // Tokyo 5 May: previous evening 4 May, window 5..12 May
        assertTrue(ids.toString(), ids.all { !it.date.isBefore(may(4)) && !it.date.isAfter(may(12)) })
    }
}
