package ninja.doskey.app.levana.icon

import android.app.AlarmManager
import android.app.Application
import ninja.doskey.app.levana.device.DeviceNow
import ninja.doskey.app.levana.domain.model.Location
import org.junit.Assert.assertEquals
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

/** TASK-5 case 13: the icon turns over at the device's midnight even when the app isn't running. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DateRolloverTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val zone = ZoneId.of("Asia/Jerusalem")
    private val day = LocalDate.of(2026, 10, 29)

    private fun scheduled() = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms
        .filter { shadowOf(it.operation).savedIntent.action == DateRollover.ACTION }

    @Test
    fun `an explicit alarm wakes the receiver at the next device midnight`() {
        val now = ZonedDateTime.of(day, LocalTime.of(21, 40), zone).toInstant()
        DateRollover.schedule(app, DeviceNow(now, zone, Location.JERUSALEM))
        val alarm = scheduled().single()
        assertEquals(ZonedDateTime.of(day.plusDays(1), LocalTime.MIDNIGHT, zone).toInstant().toEpochMilli(), alarm.triggerAtMs)
        assertEquals(DateChangeReceiver::class.java.name, shadowOf(alarm.operation).savedIntent.component?.className)
        // Rescheduling replaces it
        DateRollover.schedule(app, DeviceNow(now.plusSeconds(60), zone, Location.JERUSALEM))
        assertEquals(1, scheduled().size)
    }

    @Test
    fun `with the developer override it is still the real midnight`() {
        // As-if three days ahead; whole-day shifts keep midnight at the real midnight
        val asIf = ZonedDateTime.of(day.plusDays(3), LocalTime.of(21, 40), zone).toInstant()
        DateRollover.schedule(app, DeviceNow(asIf, zone, Location.JERUSALEM, devDayShift = 3))
        assertEquals(
            ZonedDateTime.of(day.plusDays(1), LocalTime.MIDNIGHT, zone).toInstant().toEpochMilli(),
            scheduled().single().triggerAtMs
        )
    }
}
