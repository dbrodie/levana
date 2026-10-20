package ninja.doskey.app.levana.notifications

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import ninja.doskey.app.levana.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NotificationAlarmSchedulerTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)
    private val scheduler get() = NotificationAlarmScheduler(app)
    private val location = Location.JERUSALEM

    // Far enough ahead that every trigger is in the future
    private val day = LocalDate.of(2040, 4, 20)
    private val tzait = LocalTime.of(19, 45)
    private val morning = LocalTime.of(7, 30)

    private val alarms get() = shadowOf(alarmManager).scheduledAlarms

    private fun savedIntents() = alarms.map { shadowOf(it.operation).savedIntent }

    @Test
    fun `both omer reminders and adjacent days get their own alarms`() {
        scheduler.scheduleOmerReminder(day, tzait, location, omerDay = 6)
        scheduler.scheduleOmerMorningReminder(day, morning, omerDay = 5)
        scheduler.scheduleOmerReminder(day.plusDays(1), tzait, location, omerDay = 7)
        scheduler.scheduleOmerMorningReminder(day.plusDays(1), morning, omerDay = 6)

        assertEquals(4, alarms.size)
        val byAction = savedIntents().groupBy { it.action }
        assertEquals(
            setOf(day.toEpochDay(), day.plusDays(1).toEpochDay()),
            byAction.getValue(NotificationAlarmScheduler.ACTION_OMER)
                .map { it.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0) }
                .toSet()
        )
        assertEquals(
            listOf(5, 6),
            byAction.getValue(NotificationAlarmScheduler.ACTION_OMER_MORNING)
                .map { it.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0) }
                .sorted()
        )
    }

    @Test
    fun `same day of year in different years does not replace the alarm`() {
        val dec31 = LocalDate.of(2041, 12, 31)
        val sameDayOfYearNextYear = LocalDate.of(2042, 1, 1).withDayOfYear(dec31.dayOfYear)
        scheduler.scheduleCandleLighting(dec31, LocalTime.of(10, 0), location, tzait, null)
        scheduler.scheduleCandleLighting(
            sameDayOfYearNextYear, LocalTime.of(10, 0), location, tzait, null
        )

        assertEquals(2, alarms.size)
    }

    @Test
    fun `rescheduling the same reminder replaces its alarm`() {
        scheduler.scheduleOmerReminder(day, tzait, location, omerDay = 6)
        scheduler.scheduleOmerReminder(day, tzait.plusMinutes(3), location, omerDay = 6)
        scheduler.scheduleCandleLighting(day, LocalTime.of(9, 0), location, tzait, null)
        scheduler.scheduleCandleLighting(day, LocalTime.of(14, 0), location, tzait, null)

        assertEquals(2, alarms.size)
        val omer = savedIntents().single { it.action == NotificationAlarmScheduler.ACTION_OMER }
        assertEquals(day.toEpochDay(), omer.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0))
        assertEquals(
            NotificationAlarmScheduler.alarmUri(NotificationAlarmScheduler.ACTION_OMER, day),
            omer.data
        )
    }

    @Test
    fun `scheduling cancels the alarm a previous version set for the same reminder`() {
        val legacyToday = legacyAlarm(NotificationAlarmScheduler.ACTION_OMER, 200000 + day.dayOfYear)
        val legacyOtherAction = legacyAlarm(
            NotificationAlarmScheduler.ACTION_OMER_MORNING, 201000 + day.dayOfYear
        )
        assertEquals(2, alarms.size)

        scheduler.scheduleOmerReminder(day, tzait, location, omerDay = 6)

        val operations = alarms.map { it.operation }
        assertTrue(legacyToday !in operations)
        assertTrue(legacyOtherAction in operations)
        assertEquals(2, alarms.size)
    }

    @Test
    fun `cancelAll removes new and legacy alarms`() {
        legacyAlarm(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, 100000 + 77)
        scheduler.scheduleOmerReminder(day, tzait, location, omerDay = 6)
        scheduler.scheduleOmerMorningReminder(day.plusDays(1), morning, omerDay = 6)
        scheduler.scheduleCandleLighting(day, LocalTime.of(9, 0), location, tzait, null)
        assertEquals(4, alarms.size)

        scheduler.cancelAll(around = day)

        assertEquals(0, alarms.size)
    }

    /** An alarm as the previous version set it: day-of-year request code, no data URI. */
    private fun legacyAlarm(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(app, NotificationAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, day.toEpochDay())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            app,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 3_600_000,
            pendingIntent
        )
        return pendingIntent
    }
}
