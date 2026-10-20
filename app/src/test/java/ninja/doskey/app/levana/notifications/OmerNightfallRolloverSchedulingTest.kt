package ninja.doskey.app.levana.notifications

import android.app.AlarmManager
import android.content.Context
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
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
import java.time.ZoneId
import java.time.ZonedDateTime

/** Post-midnight Omer evening alarms through the production apply path. */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class OmerNightfallRolloverSchedulingTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    private val stPetersburg = Location(59.9343, 30.3351, timezoneId = "Europe/Moscow", name = "St. Petersburg")
    private val zone = ZoneId.of(stPetersburg.timezoneId)
    private val zmanim = ZmanimRepository()
    private val prefs = UserPreferences(
        notifyCandleLighting = false,
        notifyOmer = true,
        notifyOmerTzait = true,
        notifyOmerMorning = false
    )

    private val may24 = LocalDate.of(2027, 5, 24)
    private val may25 = LocalDate.of(2027, 5, 25)

    private fun run(today: LocalDate, time: LocalTime) {
        val now = ZonedDateTime.of(today, time, zone).toInstant()
        val plan = planReminderAlarms(today, now, zone, stPetersburg, prefs, zmanim, CalendarRepository())
        runBlocking {
            applyReminderAlarmPlan(
                plan,
                NotificationAlarmScheduler(context) { now.toEpochMilli() },
                NotificationDeliveryLog(context)
            ) { error("no candle-lighting notice expected") }
        }
    }

    private fun eveningAlarms(date: LocalDate) = shadowOf(alarmManager).scheduledAlarms.filter {
        val intent = shadowOf(it.operation).savedIntent
        intent.action == NotificationAlarmScheduler.ACTION_OMER &&
            intent.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0) == date.toEpochDay()
    }

    @Suppress("DEPRECATION")
    private fun triggerMillis(alarm: org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm) =
        alarm.triggerAtTime

    private fun omerDay(alarm: org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm) =
        shadowOf(alarm.operation).savedIntent.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0)

    @Test
    fun `evening alarm is set at the next-day nightfall instant and survives a run after midnight`() {
        val nightfall = zmanim.getTzaitInstant(may24, stPetersburg)!!.toEpochMilli()

        run(may24, LocalTime.NOON)
        val first = eveningAlarms(may24).single()
        assertEquals(nightfall, triggerMillis(first))
        assertEquals(33, omerDay(first))

        // A run at 00:01 on 25 May, before that nightfall, keeps exactly one alarm for it
        run(may25, LocalTime.of(0, 1))
        val kept = eveningAlarms(may24).single()
        assertEquals(nightfall, triggerMillis(kept))
        assertEquals(33, omerDay(kept))
        val next = eveningAlarms(may25).single()
        assertEquals(34, omerDay(next))
        assertEquals(zmanim.getTzaitInstant(may25, stPetersburg)!!.toEpochMilli(), triggerMillis(next))
    }

    @Test
    fun `a first run between midnight and a post-midnight nightfall schedules that night`() {
        run(may25, LocalTime.of(0, 1))

        val alarm = eveningAlarms(may24).single()
        assertEquals(zmanim.getTzaitInstant(may24, stPetersburg)!!.toEpochMilli(), triggerMillis(alarm))
        assertEquals(33, omerDay(alarm))
        assertTrue(shadowOf(alarmManager).scheduledAlarms.none {
            triggerMillis(it) <= ZonedDateTime.of(may25, LocalTime.of(0, 1), zone).toInstant().toEpochMilli()
        })
    }
}
