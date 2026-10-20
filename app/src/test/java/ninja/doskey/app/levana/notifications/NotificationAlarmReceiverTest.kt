package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NotificationAlarmReceiverTest {

    private val app: Context get() = RuntimeEnvironment.getApplication()
    private val date = LocalDate.of(2030, 4, 26)

    @Before
    fun setUp() {
        shadowOf(app as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(app)
    }

    private fun posted() =
        shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.size

    private fun candleIntent() = Intent(app, NotificationAlarmReceiver::class.java).apply {
        action = NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING
        putExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
        putExtra(NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME, "18:30")
        putExtra(NotificationAlarmScheduler.EXTRA_END_TIME, "19:40")
    }

    private fun omerIntent(action: String) = Intent(app, NotificationAlarmReceiver::class.java).apply {
        this.action = action
        putExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
        putExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 12)
    }

    @Test
    fun staleCandleLightingAlarmIsDroppedAfterDisabling() {
        deliverIfEnabled(app, candleIntent(), UserPreferences(notifyCandleLighting = false))
        assertEquals(0, posted())

        deliverIfEnabled(app, candleIntent(), UserPreferences(notifyCandleLighting = true))
        assertEquals(1, posted())
    }

    @Test
    fun staleOmerAlarmsAreDroppedAfterDisabling() {
        val omerOff = UserPreferences(notifyOmer = false, notifyOmerMorning = true)
        deliverIfEnabled(app, omerIntent(NotificationAlarmScheduler.ACTION_OMER), omerOff)
        deliverIfEnabled(app, omerIntent(NotificationAlarmScheduler.ACTION_OMER_MORNING), omerOff)
        val morningOff = UserPreferences(notifyOmer = true, notifyOmerMorning = false)
        deliverIfEnabled(app, omerIntent(NotificationAlarmScheduler.ACTION_OMER_MORNING), morningOff)
        assertEquals(0, posted())

        deliverIfEnabled(app, omerIntent(NotificationAlarmScheduler.ACTION_OMER), morningOff)
        assertEquals(1, posted())
    }
}
