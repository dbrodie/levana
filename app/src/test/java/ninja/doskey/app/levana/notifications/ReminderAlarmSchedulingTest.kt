package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
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

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ReminderAlarmSchedulingTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    private val now = Instant.parse("2026-12-28T12:00:00Z")
    private val scheduler = NotificationAlarmScheduler(context) { now.toEpochMilli() }

    private val window = (0..7L).map { LocalDate.of(2026, 12, 28).plusDays(it) }
    private val newYearCandles = ReminderAlarm.CandleLighting(
        LocalDate.of(2027, 1, 1),
        Instant.parse("2027-01-01T13:00:00Z"),
        LocalTime.of(16, 20),
        ObservanceEnd(LocalDate.of(2027, 1, 2), LocalTime.of(17, 25), includesYomTov = false)
    )
    private val omerEvening = ReminderAlarm.OmerEvening(
        LocalDate.of(2026, 12, 31),
        Instant.parse("2026-12-31T22:00:00Z"),
        omerDay = 5
    )
    private val omerMorning = ReminderAlarm.OmerMorning(
        LocalDate.of(2026, 12, 29),
        Instant.parse("2026-12-29T12:00:00Z"),
        omerDay = 4
    )

    private fun scheduled() = shadowOf(alarmManager).scheduledAlarms

    private val newYork = Location(40.7128, -74.006, timezoneId = "America/New_York", name = "New York")
    private val nyZone = ZoneId.of(newYork.timezoneId)
    private val calendar = CalendarRepository()
    private val omerPrefs = UserPreferences(
        notifyCandleLighting = false,
        notifyOmer = true,
        notifyOmerTzait = true,
        notifyOmerMorning = true,
        notifyOmerMorningTime = 7 * 60
    )

    @Before
    fun grantNotifications() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(context)
    }

    /** One worker run at [time] on [today] in New York, through the production apply path. */
    private fun workerRun(today: LocalDate, time: LocalTime, prefs: UserPreferences = omerPrefs) {
        val now = ZonedDateTime.of(today, time, nyZone).toInstant()
        val plan = planReminderAlarms(today, now, nyZone, newYork, prefs, ZmanimRepository(), calendar)
        runBlocking {
            applyReminderAlarmPlan(
                plan,
                NotificationAlarmScheduler(context) { now.toEpochMilli() },
                NotificationDeliveryLog(context)
            ) { error("no candle-lighting notice expected") }
        }
    }

    private fun alarmsFor(action: String, date: LocalDate) = scheduled().filter {
        val intent = shadowOf(it.operation).savedIntent
        intent.action == action &&
            intent.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0) == date.toEpochDay()
    }

    @Test
    fun `undelivered overdue Omer alarms survive a later run and still post once`() {
        val day = LocalDate.of(2027, 5, 3) // during the Omer of 5787
        workerRun(day, LocalTime.of(5, 0))
        val evening = alarmsFor(NotificationAlarmScheduler.ACTION_OMER, day).single()
        val morning = alarmsFor(NotificationAlarmScheduler.ACTION_OMER_MORNING, day).single()

        // Doze held both alarms past their triggers; later runs must neither drop nor duplicate them
        workerRun(day, LocalTime.of(23, 50))
        workerRun(day, LocalTime.of(23, 55))
        assertEquals(listOf(evening), alarmsFor(NotificationAlarmScheduler.ACTION_OMER, day))
        assertEquals(listOf(morning), alarmsFor(NotificationAlarmScheduler.ACTION_OMER_MORNING, day))
        assertEquals(
            calendar.getDayInfo(day.plusDays(1), inIsrael = false).omerDay,
            shadowOf(evening.operation).savedIntent.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0)
        )
        assertEquals(
            calendar.getDayInfo(day, inIsrael = false).omerDay,
            shadowOf(morning.operation).savedIntent.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0)
        )

        // Android finally delivers them: each posts exactly one notification
        val notifications = shadowOf(context.getSystemService(NotificationManager::class.java))
        val before = notifications.allNotifications.size
        listOf(evening, morning).forEach {
            deliverIfEnabled(context, shadowOf(it.operation).savedIntent, omerPrefs)
        }
        assertEquals(before + 2, notifications.allNotifications.size)

        // The next day's run leaves them alone too and plans tomorrow without duplicates
        workerRun(day.plusDays(1), LocalTime.of(5, 0))
        val keys = scheduled().map {
            val intent = shadowOf(it.operation).savedIntent
            intent.action to intent.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0)
        }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `overdue Omer alarms are still cancelled once the reminder is turned off`() {
        val day = LocalDate.of(2027, 5, 3)
        workerRun(day, LocalTime.of(5, 0))
        workerRun(day, LocalTime.of(23, 50), omerPrefs.copy(notifyOmer = false))
        assertTrue(scheduled().isEmpty())
    }

    @Test
    fun `overdue alarms in the plan are kept while unplanned ones are cancelled`() {
        runBlocking { scheduler.scheduleAll(ReminderAlarmPlan(window, listOf(newYearCandles, omerEvening, omerMorning), null)) }
        val overdueEvening = omerEvening.copy(triggerAt = now.minusSeconds(60))
        runBlocking { scheduler.scheduleAll(ReminderAlarmPlan(window, listOf(newYearCandles), null, overdue = listOf(overdueEvening))) }

        val actions = scheduled().map { shadowOf(it.operation).savedIntent.action }.toSet()
        assertEquals(
            setOf(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, NotificationAlarmScheduler.ACTION_OMER),
            actions
        )
        // Kept as originally scheduled, not re-registered at the passed time
        assertEquals(
            omerEvening.triggerAt.toEpochMilli(),
            alarmsFor(NotificationAlarmScheduler.ACTION_OMER, omerEvening.date).single().triggerAtTime
        )
    }

    @Test
    fun `every planned alarm is registered at its instant with its extras`() {
        runBlocking {
            scheduler.scheduleAll(
                ReminderAlarmPlan(window, listOf(newYearCandles, omerEvening, omerMorning), null)
            )
        }

        val alarms = scheduled().associateBy { shadowOf(it.operation).savedIntent.action }
        assertEquals(3, alarms.size)

        val candles = alarms.getValue(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING)
        assertEquals(newYearCandles.triggerAt.toEpochMilli(), candles.triggerAtTime)
        val candleIntent = shadowOf(candles.operation).savedIntent
        assertEquals(
            newYearCandles.date.toEpochDay(),
            candleIntent.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0)
        )
        assertEquals("16:20", candleIntent.getStringExtra(NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME))
        assertEquals("17:25", candleIntent.getStringExtra(NotificationAlarmScheduler.EXTRA_END_TIME))

        val evening = alarms.getValue(NotificationAlarmScheduler.ACTION_OMER)
        assertEquals(omerEvening.triggerAt.toEpochMilli(), evening.triggerAtTime)
        assertEquals(5, shadowOf(evening.operation).savedIntent.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0))

        val morning = alarms.getValue(NotificationAlarmScheduler.ACTION_OMER_MORNING)
        assertEquals(omerMorning.triggerAt.toEpochMilli(), morning.triggerAtTime)
        assertEquals(4, shadowOf(morning.operation).savedIntent.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0))
    }

    @Test
    fun `passed triggers are not registered`() {
        val past = omerEvening.copy(date = window[0], triggerAt = now.minusSeconds(60))
        runBlocking { scheduler.scheduleAll(ReminderAlarmPlan(window, listOf(past), null)) }
        assertTrue(scheduled().isEmpty())
    }

    @Test
    fun `rerunning a plan replaces alarms instead of duplicating them`() {
        val plan = ReminderAlarmPlan(window, listOf(newYearCandles, omerEvening), null)
        runBlocking { scheduler.scheduleAll(plan) }
        runBlocking { scheduler.scheduleAll(plan) }
        assertEquals(2, scheduled().size)
    }

    @Test
    fun `alarms in the window that are no longer planned are cancelled`() {
        runBlocking { scheduler.scheduleAll(ReminderAlarmPlan(window, listOf(newYearCandles, omerEvening), null)) }
        runBlocking { scheduler.scheduleAll(ReminderAlarmPlan(window, listOf(newYearCandles), null)) }

        val actions = scheduled().map { shadowOf(it.operation).savedIntent.action }
        assertEquals(listOf(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING), actions)
    }

    @Test
    fun `due morning candle lighting is posted once per date`() = runBlocking {
        val log = NotificationDeliveryLog(context)
        val posted = mutableListOf<LocalDate>()
        val today = newYearCandles.copy(date = LocalDate.of(2027, 1, 8))
        val plan = ReminderAlarmPlan(window, emptyList(), today)

        applyReminderAlarmPlan(plan, scheduler, log) { posted += it.date }
        applyReminderAlarmPlan(plan, scheduler, log) { posted += it.date }
        assertEquals(listOf(today.date), posted)

        // Already delivered by the alarm a previous run scheduled
        val delivered = today.copy(date = LocalDate.of(2027, 1, 15))
        log.recordCandleLighting(delivered.date)
        applyReminderAlarmPlan(plan.copy(dueCandleLighting = delivered), scheduler, log) { posted += it.date }
        assertEquals(listOf(today.date), posted)
        assertEquals(delivered.date, log.lastCandleLightingDate())
    }
}
