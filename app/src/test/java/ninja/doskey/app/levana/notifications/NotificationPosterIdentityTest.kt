package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.Application
import android.app.LocaleManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.LocaleList
import androidx.core.app.NotificationCompat
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.HolidayMapper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
class NotificationPosterIdentityTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()

    private val manager: NotificationManager
        get() = app.getSystemService(NotificationManager::class.java)

    private val day = LocalDate.of(2026, 4, 20)

    @Before
    fun grantPermission() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(app)
    }

    @After
    fun resetAppLanguage() {
        app.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.getEmptyLocaleList()
    }

    private fun hebrew(): Context {
        app.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags("iw")
        return app.withAppLocale()
    }

    private val active get() = manager.activeNotifications.toList()

    private fun titles() = active.map {
        it.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
    }

    @Test
    fun `morning omer reminder survives the next evening's reminder`() {
        NotificationPoster.postOmerMorning(app, day, omerDay = 5)
        NotificationPoster.postOmer(app, day.plusDays(1), omerDay = 7)
        NotificationPoster.postOmer(app, day, omerDay = 6)

        assertEquals(3, active.size)
    }

    @Test
    fun `same name contacts and same title events each keep a notification`() {
        NotificationPoster.postBirthday(app, day, "key-1", "David Cohen", null)
        NotificationPoster.postBirthday(app, day, "key-2", "David Cohen", null)
        NotificationPoster.postPersonalEvent(app, day, 1, "Yahrzeit", null)
        NotificationPoster.postPersonalEvent(app, day, 2, "Yahrzeit", null)

        assertEquals(4, active.size)
    }

    @Test
    fun `events on different days and years do not replace each other`() {
        // Previously day ids plus a signed title hash overlapped neighbouring days
        for (offset in 0L..5L) {
            NotificationPoster.postPersonalEvent(app, day.plusDays(offset), 1, "Event", null)
        }
        NotificationPoster.postPersonalEvent(app, day.plusYears(1), 1, "Event", null)
        NotificationPoster.postCandleLighting(app, LocalDate.of(2026, 12, 31), LT, null)
        NotificationPoster.postCandleLighting(app, LocalDate.of(2027, 12, 31), LT, null)

        assertEquals(9, active.size)
    }

    @Test
    fun `holiday observances on one date are posted separately`() {
        val chanukah = HolidayMapper.mapHoliday(JewishCalendar.CHANUKAH)!!
        val roshChodesh = HolidayMapper.mapHoliday(JewishCalendar.ROSH_CHODESH)!!
        NotificationPoster.postHoliday(app, day, chanukah, daysUntil = 0)
        NotificationPoster.postHoliday(app, day, roshChodesh, daysUntil = 0)

        assertEquals(2, active.size)
    }

    @Test
    fun `reposting the same reminder replaces it, also after a language change`() {
        val pesach = HolidayMapper.mapHoliday(JewishCalendar.PESACH)!!
        // Days-before reminder reposted each day until the holiday
        NotificationPoster.postHoliday(app, day, pesach, daysUntil = 2)
        NotificationPoster.postHoliday(app, day, pesach, daysUntil = 1)
        NotificationPoster.postPersonalEvent(app, day, 3, "Old title", null)
        NotificationPoster.postPersonalEvent(app, day, 3, "New title", null)
        NotificationPoster.postBirthday(app, day, "key-1", "David", null)
        NotificationPoster.postOmerMorning(app, day, 5)
        NotificationPoster.postOmer(app, day, 6)
        val englishTitles = titles()

        val hebrew = hebrew()
        NotificationPoster.postHoliday(hebrew, day, pesach, daysUntil = 1)
        NotificationPoster.postPersonalEvent(hebrew, day, 3, "New title", null)
        NotificationPoster.postBirthday(hebrew, day, "key-1", "David", null)
        NotificationPoster.postOmerMorning(hebrew, day, 5)
        NotificationPoster.postOmer(hebrew, day, 6)

        assertEquals(5, active.size)
        // The Hebrew posts replaced the English ones rather than being dropped
        assertNotEquals(englishTitles.toSet(), titles().toSet())
        assertEquals(
            omerNotificationText(hebrew, 6, tonight = true).title,
            active.single { it.tag == NotificationKeys.omerTzait(day).tag }
                .notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
        )
    }

    @Test
    fun `posting a reminder removes the untagged copy from the previous version`() {
        val key = NotificationKeys.omerTzait(day)
        val legacy = NotificationCompat.Builder(app, NotificationChannels.OMER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("old")
            .build()
        manager.notify(key.legacyId, legacy)
        manager.notify(UPDATE_ID, legacy)

        NotificationPoster.postOmer(app, day, 6)

        assertEquals(
            setOf(key.tag to key.id, null to UPDATE_ID),
            active.map { it.tag to it.id }.toSet()
        )
    }

    private companion object {
        val LT: java.time.LocalTime = java.time.LocalTime.of(18, 0)
        const val UPDATE_ID = 9001
    }
}
