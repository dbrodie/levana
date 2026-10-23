package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.LocaleManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.LocaleList
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.After
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Havdalah after civil midnight keeps its actual civil date through the observance end, the
 * alarm and the notification wording (levana-jh0). Expected dates are written out.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PostMidnightHavdalahTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val zmanim = ZmanimRepository()

    private val stPetersburg = Location(59.9343, 30.3351, timezoneId = "Europe/Moscow", name = "St. Petersburg")
    private val riga = Location(56.9496, 24.1052, timezoneId = "Europe/Riga", name = "Riga")
    private val newYork = Location(40.7128, -74.0060, timezoneId = "America/New_York", name = "New York")

    /** Fri 17 Jul 2026; Shabbat ends at nightfall, Sun 19 Jul 00:13:59 in St. Petersburg. */
    private val friday = LocalDate.of(2026, 7, 17)

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(app)
        // App language English; Robolectric keeps an app locale across tests
        app.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("en-US")
    }

    @After
    fun resetAppLanguage() {
        app.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.getEmptyLocaleList()
    }

    private fun hebrew(): Context {
        app.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("iw")
        return app.withAppLocale()
    }

    private fun hm(time: LocalTime?) = time?.withSecond(0)?.withNano(0)

    @Test
    fun `post-midnight havdalah keeps its civil date`() {
        val end = zmanim.getObservanceEnd(friday, stPetersburg, inIsrael = false)!!
        assertEquals(LocalDate.of(2026, 7, 19), end.date)
        assertEquals(LocalTime.of(0, 13), hm(end.time))
        assertEquals(LocalDate.of(2026, 7, 18), end.lastDay)
        assertFalse(end.includesYomTov)
    }

    @Test
    fun `post-midnight end of Yom Tov followed by Shabbat`() {
        // Shavuot Fri 11 + Shabbat 12 Jun 2027 (Diaspora); ends Sun 13 Jun 00:12:48 in Riga
        for (source in listOf(LocalDate.of(2027, 6, 10), LocalDate.of(2027, 6, 11))) {
            val end = zmanim.getObservanceEnd(source, riga, inIsrael = false)!!
            assertEquals(LocalDate.of(2027, 6, 13), end.date)
            assertEquals(LocalTime.of(0, 12), hm(end.time))
            assertEquals(LocalDate.of(2027, 6, 12), end.lastDay)
            assertTrue(end.includesYomTov)
        }
    }

    @Test
    fun `ordinary and consecutive-day ends are unchanged`() {
        val nyFriday = LocalDate.of(2026, 11, 13)
        val ordinary = zmanim.getObservanceEnd(nyFriday, newYork, inIsrael = false)!!
        assertEquals(nyFriday.plusDays(1), ordinary.date)
        assertEquals(ordinary.date, ordinary.lastDay)
        assertEquals(zmanim.getTzaitTime(nyFriday.plusDays(1), newYork), ordinary.time)

        // Rosh Hashanah 5787: Sat 12 + Sun 13 Sep 2026, from Friday's candles; ends Sunday night
        val rh = zmanim.getObservanceEnd(LocalDate.of(2026, 9, 11), newYork, inIsrael = false)!!
        assertEquals(LocalDate.of(2026, 9, 13), rh.date)
        assertEquals(rh.date, rh.lastDay)
        assertTrue(rh.includesYomTov)
    }

    @Test
    fun `wording names the civil day of a post-midnight end`() {
        val end = ObservanceEnd(LocalDate.of(2026, 7, 19), LocalTime.of(0, 13), false, lastDay = LocalDate.of(2026, 7, 18))
        val english = candleLightingNotificationText(app.withAppLocale(), friday, LocalTime.of(21, 40), end)
        assertEquals("Candle Lighting at 9:40 PM · Shabbat ends Sunday 12:13 AM", english.body)

        // Ordinary next-day end: time only
        val ordinary = ObservanceEnd(friday.plusDays(1), LocalTime.of(22, 50), false)
        assertEquals(
            "Candle Lighting at 9:40 PM · Shabbat ends 10:50 PM",
            candleLightingNotificationText(app.withAppLocale(), friday, LocalTime.of(21, 40), ordinary).body
        )

        val hebrewBody = candleLightingNotificationText(hebrew(), friday, LocalTime.of(21, 40), end).body
        assertTrue(hebrewBody, hebrewBody.contains("יום ראשון 00:13"))
        assertFalse(hebrewBody, hebrewBody.contains("יום שבת"))
    }

    @Test
    fun `alarm carries the civil end date to the posted notification`() {
        val prefs = UserPreferences(
            notifyCandleLighting = true,
            candleLightingNotifyMode = "hours_before",
            candleLightingHoursBefore = 2,
            notifyOmer = false
        )
        val zone = ZoneId.of(stPetersburg.timezoneId)
        val now = ZonedDateTime.of(friday, LocalTime.of(8, 0), zone).toInstant()
        val plan = planReminderAlarms(friday, now, zone, stPetersburg, prefs, zmanim, CalendarRepository())
        runBlocking {
            NotificationAlarmScheduler(app, testRegistry(), now::toEpochMilli).scheduleAll(plan)
        }
        val intent = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms
            .map { shadowOf(it.operation).savedIntent }
            .single {
                it.action == NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING &&
                    it.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, 0) == friday.toEpochDay()
            }
        assertEquals(LocalDate.of(2026, 7, 19).toEpochDay(),
            intent.getLongExtra(NotificationAlarmScheduler.EXTRA_END_CIVIL_DATE_EPOCH_DAY, 0))
        assertEquals(LocalDate.of(2026, 7, 18).toEpochDay(),
            intent.getLongExtra(NotificationAlarmScheduler.EXTRA_END_DATE_EPOCH_DAY, 0))

        assertTrue(deliverIfEnabled(app, intent, prefs))
        val posted = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
        val body = posted.extras.getCharSequence("android.text").toString()
        assertTrue(body, body.endsWith("Shabbat ends Sunday 12:13 AM"))
    }

    @Test
    fun `alarm from a previous version keeps working with its last-day extra`() {
        val legacy = Intent(app, NotificationAlarmReceiver::class.java).apply {
            action = NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING
            putExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, friday.toEpochDay())
            putExtra(NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME, "21:40")
            putExtra(NotificationAlarmScheduler.EXTRA_END_TIME, "22:50")
            putExtra(NotificationAlarmScheduler.EXTRA_END_DATE_EPOCH_DAY, friday.plusDays(1).toEpochDay())
        }
        assertTrue(deliverIfEnabled(app, legacy, UserPreferences(notifyCandleLighting = true)))
        val body = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
            .extras.getCharSequence("android.text").toString()
        assertEquals("Candle Lighting at 9:40 PM · Shabbat ends 10:50 PM", body)
    }

    private fun testRegistry() = ScheduledAlarmRegistry(testDataStore())
}
