package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.LocaleManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.LocaleList
import androidx.core.app.NotificationCompat
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import ninja.doskey.app.levana.data.HolidayMapper
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

/** Independent adversarial checks of notification and alarm identity / replacement. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NotificationIdentityVerificationTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val manager get() = app.getSystemService(NotificationManager::class.java)
    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)
    private val scheduler get() = NotificationAlarmScheduler(app)
    private val active get() = manager.activeNotifications.toList()
    private val alarms get() = shadowOf(alarmManager).scheduledAlarms

    private val day = LocalDate.of(2026, 4, 20)
    private val future = LocalDate.of(2041, 4, 20)

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(app)
    }

    @After
    fun resetLanguage() {
        app.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.getEmptyLocaleList()
    }

    private fun hebrew(): Context {
        app.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags("iw")
        return app.withAppLocale()
    }

    private fun titles() = active.map {
        it.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
    }

    private fun legacyNotification(id: Int) {
        val n = NotificationCompat.Builder(app, NotificationChannels.PERSONAL_EVENTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("legacy $id")
            .build()
        manager.notify(id, n)
    }

    // --- Omer ---------------------------------------------------------------------------

    @Test
    fun `whole omer season of morning and evening reminders never collide`() {
        val start = LocalDate.of(2026, 4, 2)
        for (i in 0 until 50) {
            NotificationPoster.postOmerMorning(app, start.plusDays(i.toLong()), omerDay = i + 1)
            NotificationPoster.postOmer(app, start.plusDays(i.toLong()), omerDay = i + 2)
        }
        assertEquals(100, active.size)
    }

    @Test
    fun `omer evening of next day does not replace morning of previous day in either order`() {
        NotificationPoster.postOmer(app, day.plusDays(1), omerDay = 8)
        NotificationPoster.postOmerMorning(app, day, omerDay = 6)
        assertEquals(2, active.size)
        NotificationPoster.postOmer(app, day.plusDays(1), omerDay = 8)
        assertEquals(2, active.size)
    }

    // --- Dates --------------------------------------------------------------------------

    @Test
    fun `same day of year in different years stays separate for every category`() {
        val a = LocalDate.of(2025, 3, 14)
        val b = LocalDate.of(2026, 3, 14) // both non-leap: same dayOfYear
        val leapShift = LocalDate.of(2024, 3, 13) // dayOfYear 73 like 2025-03-14
        assertEquals(a.dayOfYear, b.dayOfYear)
        assertEquals(a.dayOfYear, leapShift.dayOfYear)
        val chanukah = HolidayMapper.mapHoliday(JewishCalendar.CHANUKAH)!!
        val fast = HolidayMapper.mapHoliday(JewishCalendar.TENTH_OF_TEVES)!!
        for (d in listOf(a, b, leapShift)) {
            NotificationPoster.postCandleLighting(app, d, LocalTime.of(18, 0), null)
            NotificationPoster.postHoliday(app, d, chanukah, 0)
            NotificationPoster.postFast(app, d, fast, null, null)
            NotificationPoster.postPersonalEvent(app, d, 7, "Anniversary", null)
            NotificationPoster.postBirthday(app, d, "lk", "Sarah", null)
            NotificationPoster.postOmer(app, d, 3)
            NotificationPoster.postOmerMorning(app, d, 2)
        }
        assertEquals(21, active.size)
    }

    @Test
    fun `all categories on one date coexist`() {
        val chanukah = HolidayMapper.mapHoliday(JewishCalendar.CHANUKAH)!!
        val fast = HolidayMapper.mapHoliday(JewishCalendar.TENTH_OF_TEVES)!!
        NotificationPoster.postCandleLighting(app, day, LocalTime.of(18, 0), null)
        NotificationPoster.postHoliday(app, day, chanukah, 0)
        NotificationPoster.postFast(app, day, fast, null, null)
        NotificationPoster.postPersonalEvent(app, day, 1, "1", null)
        NotificationPoster.postBirthday(app, day, "1", "1", null)
        NotificationPoster.postOmer(app, day, 3)
        NotificationPoster.postOmerMorning(app, day, 2)
        assertEquals(7, active.size)
    }

    // --- Holidays -----------------------------------------------------------------------

    @Test
    fun `coinciding holidays each keep a notification and daily repost replaces`() {
        val chanukah = HolidayMapper.mapHoliday(JewishCalendar.CHANUKAH)!!
        val roshChodesh = HolidayMapper.mapHoliday(JewishCalendar.ROSH_CHODESH)!!
        NotificationPoster.postHoliday(app, day, chanukah, daysUntil = 2)
        NotificationPoster.postHoliday(app, day, roshChodesh, daysUntil = 2)
        assertEquals(2, active.size)
        // Next days' runs re-post the same observances with fewer days remaining
        NotificationPoster.postHoliday(app, day, chanukah, daysUntil = 1)
        NotificationPoster.postHoliday(app, day, roshChodesh, daysUntil = 1)
        NotificationPoster.postHoliday(app, day, chanukah, daysUntil = 0)
        assertEquals(2, active.size)
    }

    @Test
    fun `holiday language change replaces rather than duplicates`() {
        val chanukah = HolidayMapper.mapHoliday(JewishCalendar.CHANUKAH)!!
        NotificationPoster.postHoliday(app, day, chanukah, 0)
        val english = titles().single()
        NotificationPoster.postHoliday(hebrew(), day, chanukah, 0)
        assertEquals(1, active.size)
        assertTrue(titles().single() != english)
    }

    @Test
    fun `holiday and fast on the same date are separate`() {
        val fast = HolidayMapper.mapHoliday(JewishCalendar.TENTH_OF_TEVES)!!
        val chanukah = HolidayMapper.mapHoliday(JewishCalendar.CHANUKAH)!!
        NotificationPoster.postHoliday(app, day, chanukah, 0)
        NotificationPoster.postFast(app, day, fast, null, null)
        NotificationPoster.postFast(app, day, fast, LocalTime.of(5, 0), LocalTime.of(18, 0))
        assertEquals(2, active.size)
    }

    // --- Personal events ----------------------------------------------------------------

    @Test
    fun `titles whose legacy hashes collide stay separate`() {
        // "Aa" and "BB" have identical String.hashCode, so the old title-hash ids collided
        assertEquals("event|Aa|null".hashCode(), "event|BB|null".hashCode())
        NotificationPoster.postPersonalEvent(app, day, 10, "Aa", null)
        NotificationPoster.postPersonalEvent(app, day, 11, "BB", null)
        assertEquals(2, active.size)
    }

    @Test
    fun `event hash offset cannot alias a neighbouring day's event`() {
        // Old id = 30000 + dayOfYear + hash % 1000: find titles making day D and D+1 alias
        val d1 = day
        val d2 = day.plusDays(1)
        val t1 = "x"
        val target = ("event|$t1|null".hashCode() % 1000) - 1
        val t2 = (0..200_000).map { "t$it" }
            .first { "event|$it|null".hashCode() % 1000 == target }
        assertEquals(
            30000 + d1.dayOfYear + "event|$t1|null".hashCode() % 1000,
            30000 + d2.dayOfYear + "event|$t2|null".hashCode() % 1000
        )
        NotificationPoster.postPersonalEvent(app, d1, 1, t1, null)
        NotificationPoster.postPersonalEvent(app, d2, 2, t2, null)
        assertEquals(2, active.size)
    }

    @Test
    fun `same title events stay separate and edits or language changes replace`() {
        NotificationPoster.postPersonalEvent(app, day, 1, "Yahrzeit", null)
        NotificationPoster.postPersonalEvent(app, day, 2, "Yahrzeit", null)
        assertEquals(2, active.size)
        NotificationPoster.postPersonalEvent(app, day, 1, "Yahrzeit of Moshe", null)
        NotificationPoster.postPersonalEvent(hebrew(), day, 2, "Yahrzeit", null)
        assertEquals(2, active.size)
        val bodies = active.map {
            it.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        }
        assertEquals(1, bodies.count { it.contains("Yahrzeit of Moshe") })
    }

    @Test
    fun `label change for the same event and date replaces`() {
        NotificationPoster.postPersonalEvent(app, day, 3, "Bar mitzva", OccurrenceLabel.ADAR_I)
        NotificationPoster.postPersonalEvent(app, day, 3, "Bar mitzva", null)
        assertEquals(1, active.size)
    }

    @Test
    fun `event id and contact key with equal text do not collide`() {
        NotificationPoster.postPersonalEvent(app, day, 42, "Dana", null)
        NotificationPoster.postBirthday(app, day, "42", "Dana", null)
        assertEquals(2, active.size)
    }

    // --- Birthdays ----------------------------------------------------------------------

    @Test
    fun `same name contacts separate and renamed contact replaces`() {
        NotificationPoster.postBirthday(app, day, "lookup-a", "David", null)
        NotificationPoster.postBirthday(app, day, "lookup-b", "David", null)
        assertEquals(2, active.size)
        NotificationPoster.postBirthday(app, day, "lookup-a", "David Levi", null)
        NotificationPoster.postBirthday(hebrew(), day, "lookup-b", "David", null)
        assertEquals(2, active.size)
    }

    @Test
    fun `contacts without lookup key fall back to name and never alias a keyed contact`() {
        NotificationPoster.postBirthday(app, day, "", "Rivka", null)
        NotificationPoster.postBirthday(app, day, "", "Leah", null)
        NotificationPoster.postBirthday(app, day, "Rivka", "Rivka", null)
        assertEquals(3, active.size)
    }

    // --- Legacy migration ---------------------------------------------------------------

    @Test
    fun `posting replaces the previous version's untagged copy only`() {
        val other = day.plusDays(3)
        legacyNotification(0 * 10000 + day.dayOfYear) // candle lighting, same date
        legacyNotification(0 * 10000 + other.dayOfYear) // candle lighting, another date
        legacyNotification(3 * 10000 + day.dayOfYear + "event|Yahrzeit|null".hashCode() % 1000)
        legacyNotification(4 * 10000 + day.dayOfYear + 1) // omer morning
        legacyNotification(9001) // update available

        NotificationPoster.postCandleLighting(app, day, LocalTime.of(18, 0), null)
        NotificationPoster.postPersonalEvent(app, day, 5, "Yahrzeit", null)
        NotificationPoster.postOmerMorning(app, day, 4)

        val ids = active.map { it.tag to it.id }
        assertEquals(5, active.size)
        assertTrue((null to other.dayOfYear) in ids)
        assertTrue((null to 9001) in ids)
        assertEquals(3, active.count { it.tag != null })
    }

    // --- Deep-link PendingIntent --------------------------------------------------------

    @Test
    fun `each notification opens its own date`() {
        val dates = listOf(day, day.plusDays(1), day.plusYears(1))
        for (d in dates) NotificationPoster.postOmer(app, d, 3)
        NotificationPoster.postPersonalEvent(app, day, 1, "x", null)
        val opened = active.associate { sbn ->
            sbn.tag to shadowOf(sbn.notification.contentIntent).savedIntent
                .getLongExtra(NotificationPoster.EXTRA_DATE_EPOCH_DAY, -1)
        }
        assertEquals(day.toEpochDay(), opened.getValue("omer_tzait/$day"))
        assertEquals(day.plusDays(1).toEpochDay(), opened.getValue("omer_tzait/${day.plusDays(1)}"))
        assertEquals(day.plusYears(1).toEpochDay(), opened.getValue("omer_tzait/${day.plusYears(1)}"))
        assertEquals(day.toEpochDay(), opened.getValue("personal_event/$day/1"))
    }

    // --- Alarms -------------------------------------------------------------------------

    private fun savedIntents() = alarms.map { shadowOf(it.operation).savedIntent }

    @Test
    fun `alarms for the same day of year in different years stay separate`() {
        val next = future.plusYears(1)
        assertEquals(future.dayOfYear, next.dayOfYear)
        for (d in listOf(future, next)) {
            scheduler.scheduleCandleLighting(d, LocalTime.of(10, 0), Location.JERUSALEM, LocalTime.of(18, 0), null)
            scheduler.scheduleOmerReminder(d, LocalTime.of(19, 45), Location.JERUSALEM, 3)
            scheduler.scheduleOmerMorningReminder(d, LocalTime.of(7, 0), 2)
        }
        assertEquals(6, alarms.size)
    }

    @Test
    fun `rescheduling replaces the alarm and updates its extras`() {
        scheduler.scheduleOmerReminder(future, LocalTime.of(19, 45), Location.JERUSALEM, 3)
        scheduler.scheduleOmerReminder(future, LocalTime.of(20, 5), Location.JERUSALEM, 4)
        scheduler.scheduleCandleLighting(future, LocalTime.of(9, 0), Location.JERUSALEM, LocalTime.of(18, 0), null)
        scheduler.scheduleCandleLighting(future, LocalTime.of(11, 0), Location.JERUSALEM, LocalTime.of(18, 5), null)
        assertEquals(2, alarms.size)
        val omer = savedIntents().single { it.action == NotificationAlarmScheduler.ACTION_OMER }
        assertEquals(4, omer.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0))
        val candle = savedIntents().single { it.action == NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING }
        assertEquals("18:05", candle.getStringExtra(NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME))
    }

    @Test
    fun `morning alarm and next day's evening alarm stay separate`() {
        scheduler.scheduleOmerMorningReminder(future, LocalTime.of(7, 0), 5)
        scheduler.scheduleOmerReminder(future.plusDays(1), LocalTime.of(19, 45), Location.JERUSALEM, 7)
        scheduler.scheduleOmerReminder(future, LocalTime.of(19, 45), Location.JERUSALEM, 6)
        assertEquals(3, alarms.size)
    }

    @Test
    fun `scheduling cancels the previous version's alarm for that reminder only`() {
        fun legacy(action: String, code: Int): PendingIntent {
            val pi = PendingIntent.getBroadcast(
                app, code,
                Intent(app, NotificationAlarmReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Long.MAX_VALUE / 2, pi)
            return pi
        }
        legacy(NotificationAlarmScheduler.ACTION_OMER, 200000 + future.dayOfYear)
        legacy(NotificationAlarmScheduler.ACTION_OMER, 200000 + future.dayOfYear + 1)
        legacy(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, 100000 + future.dayOfYear)
        assertEquals(3, alarms.size)

        scheduler.scheduleOmerReminder(future, LocalTime.of(19, 45), Location.JERUSALEM, 3)

        // Legacy same-date omer gone; next-day omer and candle legacy alarms kept; new one added
        assertEquals(3, alarms.size)
        assertEquals(1, savedIntents().count { it.data != null })
        assertNull(
            PendingIntent.getBroadcast(
                app, 200000 + future.dayOfYear,
                Intent(app, NotificationAlarmReceiver::class.java)
                    .setAction(NotificationAlarmScheduler.ACTION_OMER),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        )

        scheduler.cancelAll(around = future)
        assertEquals(0, alarms.size)
    }

    @Test
    fun `fired alarm posts the reminder for its own date`() {
        scheduler.scheduleOmerMorningReminder(future, LocalTime.of(7, 0), 5)
        scheduler.scheduleOmerReminder(future, LocalTime.of(19, 45), Location.JERUSALEM, 6)
        for (intent in savedIntents()) deliverIfEnabled(
            app, intent, ninja.doskey.app.levana.domain.model.UserPreferences(
                notifyOmer = true, notifyOmerMorning = true
            )
        )
        // Re-delivery of the same alarm replaces its notification
        for (intent in savedIntents()) deliverIfEnabled(
            app, intent, ninja.doskey.app.levana.domain.model.UserPreferences(
                notifyOmer = true, notifyOmerMorning = true
            )
        )
        assertEquals(
            setOf("omer_morning/$future", "omer_tzait/$future"),
            active.map { it.tag }.toSet()
        )
        assertEquals(2, active.size)
        assertNotNull(active.first().notification.contentIntent)
    }
}
