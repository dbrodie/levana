package ninja.doskey.app.levana.notifications

import android.app.AlarmManager
import android.content.Context
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

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

    @Test
    fun `every planned alarm is registered at its instant with its extras`() {
        scheduler.scheduleAll(
            ReminderAlarmPlan(window, listOf(newYearCandles, omerEvening, omerMorning), null)
        )

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
        scheduler.scheduleAll(ReminderAlarmPlan(window, listOf(past), null))
        assertTrue(scheduled().isEmpty())
    }

    @Test
    fun `rerunning a plan replaces alarms instead of duplicating them`() {
        val plan = ReminderAlarmPlan(window, listOf(newYearCandles, omerEvening), null)
        scheduler.scheduleAll(plan)
        scheduler.scheduleAll(plan)
        assertEquals(2, scheduled().size)
    }

    @Test
    fun `alarms in the window that are no longer planned are cancelled`() {
        scheduler.scheduleAll(ReminderAlarmPlan(window, listOf(newYearCandles, omerEvening), null))
        scheduler.scheduleAll(ReminderAlarmPlan(window, listOf(newYearCandles), null))

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
