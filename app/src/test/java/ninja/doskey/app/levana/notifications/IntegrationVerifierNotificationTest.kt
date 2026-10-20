package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Independent integration checks (verify-bugfix-integration): planner -> scheduler ->
 * delivery, combined with the settings-change rebuild path. Calendar facts are taken from
 * the published Hebrew calendar for 2026 (Pesach begins the evening of Wed 1 Apr 2026,
 * Shavuot the evening of Thu 21 May 2026, Sukkot the evening of Fri 25 Sep 2026,
 * Shemini Atzeret Sat 3 Oct 2026), not from the code under test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class IntegrationVerifierNotificationTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)
    private val notificationManager get() = app.getSystemService(NotificationManager::class.java)

    private val zmanim = ZmanimRepository()
    private val calendar = CalendarRepository()
    private val ny = Location(40.7128, -74.006, timezoneId = "America/New_York", name = "NY")
    private val nyZone = ZoneId.of("America/New_York")

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(app)
        val prefs = PreferencesRepository(app, CityRepository(app))
        startKoin { modules(module { single { prefs } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun scheduler(now: ZonedDateTime) =
        NotificationAlarmScheduler(app) { now.toInstant().toEpochMilli() }

    private fun pendingAlarms() = shadowOf(alarmManager).scheduledAlarms.map { shadowOf(it.operation).savedIntent }

    private fun pending(action: String, date: LocalDate): Intent? = pendingAlarms().singleOrNull {
        it.action == action &&
            it.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, -1) == date.toEpochDay()
    }

    private fun at(date: LocalDate, h: Int, m: Int) = ZonedDateTime.of(date, LocalTime.of(h, m), nyZone)

    private fun plan(now: ZonedDateTime, prefs: UserPreferences, location: Location = ny, lookahead: Int = REMINDER_LOOKAHEAD_DAYS) =
        planReminderAlarms(now.toLocalDate(), now.toInstant(), now.zone, location, prefs, zmanim, calendar, lookahead)

    private fun Notification.body(): String =
        (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT)).toString()

    // ---- Settings-change rebuild vs. an overdue (not yet delivered) morning candle alarm ----

    /**
     * Friday 23 Oct 2026, morning mode at 09:30. The daily run at 08:00 schedules the alarm.
     * At 09:35 Android has not delivered it yet (Doze may defer setAndAllowWhileIdle), and the
     * user changes an unrelated reminder setting (Omer morning time), which triggers the
     * settings-change rebuild (NotificationAlarmRebuilder.rebuild() with no delivery log ->
     * scheduler.scheduleAll(plan)). The still-wanted, not-yet-delivered alarm must survive.
     */
    @Test
    fun `settings change rebuild keeps today's undelivered morning candle alarm`() {
        val friday = LocalDate.of(2026, 10, 23)
        val prefs = UserPreferences(
            notifyCandleLighting = true,
            candleLightingNotifyMode = "morning",
            candleLightingMorningTime = 9 * 60 + 30
        )
        val first = at(friday, 8, 0)
        runBlocking {
            applyReminderAlarmPlan(plan(first, prefs), scheduler(first), NotificationDeliveryLog(app)) {}
        }
        assertNotNull("daily run schedules Friday's alarm", pending(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, friday))

        val later = at(friday, 9, 35)
        val changed = prefs.copy(notifyOmerMorningTime = 400)
        // Exactly what rebuild() does without a delivery log
        scheduler(later).scheduleAll(plan(later, changed))

        assertNotNull(
            "an unrelated settings change must not cancel today's still-undelivered candle alarm",
            pending(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, friday)
        )
    }

    /** Same scenario for hours_before mode: the overdue alarm is in plan.overdue and kept. */
    @Test
    fun `settings change rebuild keeps an overdue hours-before candle alarm`() {
        val friday = LocalDate.of(2026, 10, 23)
        val prefs = UserPreferences(
            notifyCandleLighting = true,
            candleLightingNotifyMode = "hours_before",
            candleLightingHoursBefore = 2
        )
        val first = at(friday, 8, 0)
        scheduler(first).scheduleAll(plan(first, prefs))
        val alarm = pending(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, friday)
        assertNotNull(alarm)
        val candle = LocalTime.parse(alarm!!.getStringExtra(NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME))
        val after = ZonedDateTime.of(friday, candle.minusHours(2).plusMinutes(3), nyZone)
        scheduler(after).scheduleAll(plan(after, prefs.copy(notifyOmerMorningTime = 400)))
        assertNotNull(pending(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, friday))
    }

    @Test
    fun `turning candle lighting off cancels the whole forward horizon and blocks delivery`() {
        val start = at(LocalDate.of(2026, 10, 20), 8, 0)
        val on = UserPreferences(notifyCandleLighting = true, candleLightingNotifyMode = "hours_before")
        scheduler(start).scheduleAll(plan(start, on, lookahead = 14))
        val fridays = listOf(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 30))
        fridays.forEach { assertNotNull(pending(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, it)) }
        val stale = pending(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, fridays[0])!!

        val off = on.copy(notifyCandleLighting = false)
        scheduler(start).scheduleAll(plan(start, off, lookahead = 14))
        fridays.forEach { assertNull(pending(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, it)) }

        // An already-fired intent reaching the receiver after the switch-off posts nothing
        deliverIfEnabled(app, stale, off)
        assertEquals(0, shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun `daily run at different times of day keeps a full forward horizon`() {
        val prefs = UserPreferences(
            notifyCandleLighting = true,
            candleLightingNotifyMode = "hours_before"
        )
        // Run on Friday evening after candle lighting: next Friday must be scheduled
        val fridayNight = at(LocalDate.of(2026, 10, 23), 22, 0)
        scheduler(fridayNight).scheduleAll(plan(fridayNight, prefs))
        assertNotNull(pending(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING, LocalDate.of(2026, 10, 30)))
        // Crosses the US DST change (1 Nov 2026): trigger is 2h before candle lighting in EST
        val fri = LocalDate.of(2026, 11, 6)
        val sat = at(LocalDate.of(2026, 10, 31), 12, 0)
        scheduler(sat).scheduleAll(plan(sat, prefs))
        val alarm = shadowOf(alarmManager).scheduledAlarms.single {
            shadowOf(it.operation).savedIntent.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, -1) == fri.toEpochDay()
        }
        val candle = LocalTime.parse(shadowOf(alarm.operation).savedIntent
            .getStringExtra(NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME))
        val expected = ZonedDateTime.of(fri, candle, nyZone).minusHours(2).toInstant().toEpochMilli()
        assertEquals(expected, alarm.triggerAtTime)
        assertEquals("EST offset", -5 * 3600, ZonedDateTime.of(fri, candle, nyZone).offset.totalSeconds)
    }

    // ---- Holy-day transitions end-to-end ----

    private fun czc(date: LocalDate, location: Location): ComplexZmanimCalendar {
        val geo = GeoLocation(location.name, location.latitude, location.longitude, TimeZone.getTimeZone(location.timezoneId))
        return ComplexZmanimCalendar(geo).apply {
            calendar = GregorianCalendar(TimeZone.getTimeZone(location.timezoneId)).apply {
                clear(); set(date.year, date.monthValue - 1, date.dayOfMonth)
            }
        }
    }

    private fun sunset(date: LocalDate): LocalTime =
        czc(date, ny).sunset.toInstant().atZone(nyZone).toLocalTime()

    private fun assertNear(expected: LocalTime, actual: LocalTime, what: String) {
        val diff = Duration.between(expected, actual).abs()
        assertTrue("$what: expected ~$expected got $actual", diff <= Duration.ofMinutes(1))
    }

    private fun candleAlarms(prefs: UserPreferences, now: ZonedDateTime, lookahead: Int) =
        plan(now, prefs, lookahead = lookahead).alarms.filterIsInstance<ReminderAlarm.CandleLighting>()
            .associateBy { it.date }

    @Test
    fun `Diaspora Sukkot and Shemini Atzeret 2026 candle lighting and havdalah`() {
        val prefs = UserPreferences(notifyCandleLighting = true, candleLightingNotifyMode = "hours_before", isInIsrael = false)
        val alarms = candleAlarms(prefs, at(LocalDate.of(2026, 9, 24), 6, 0), lookahead = 10)
        val d = { day: Int, month: Int -> LocalDate.of(2026, month, day) }
        assertEquals(setOf(d(25, 9), d(26, 9), d(2, 10), d(3, 10)), alarms.keys)

        // Friday -> Shabbat + Sukkot I: before sunset
        val fri = alarms.getValue(d(25, 9))
        assertNear(sunset(d(25, 9)).minusMinutes(18), fri.candleLightingTime, "erev Sukkot")
        assertEquals(d(27, 9), fri.end!!.date)
        assertTrue(fri.end!!.includesYomTov)
        // Shabbat -> Sukkot II: only after nightfall
        val sat = alarms.getValue(d(26, 9))
        assertTrue("after sunset", sat.candleLightingTime.isAfter(sunset(d(26, 9))))
        assertEquals(zmanim.getTzaitTime(d(26, 9), ny), sat.candleLightingTime)
        assertEquals(d(27, 9), sat.end!!.date)
        assertEquals(zmanim.getTzaitTime(d(27, 9), ny), sat.end!!.time)
        // Hoshana Rabba (Fri) -> Shemini Atzeret on Shabbat -> Simchat Torah Sunday
        assertEquals(d(4, 10), alarms.getValue(d(2, 10)).end!!.date)
        assertTrue(alarms.getValue(d(3, 10)).candleLightingTime.isAfter(sunset(d(3, 10))))

        // Displayed day info agrees: no havdalah on 26 Sep, havdalah on 27 Sep
        assertNull(zmanim.getShabbatInfo(d(26, 9), ny, inIsrael = false).havdalahTime)
        assertNotNull(zmanim.getShabbatInfo(d(27, 9), ny, inIsrael = false).havdalahTime)
        assertNull(zmanim.getShabbatInfo(d(27, 9), ny, inIsrael = false).candleLightingTime)

        // Delivery text names the end weekday (Sunday) since it is not the next day
        val intent = Intent(NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING).apply {
            putExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, fri.date.toEpochDay())
            putExtra(NotificationAlarmScheduler.EXTRA_CANDLE_LIGHTING_TIME, fri.candleLightingTime.toString())
            putExtra(NotificationAlarmScheduler.EXTRA_END_TIME, fri.end!!.time.toString())
            putExtra(NotificationAlarmScheduler.EXTRA_END_DATE_EPOCH_DAY, fri.end!!.date.toEpochDay())
            putExtra(NotificationAlarmScheduler.EXTRA_END_INCLUDES_YOM_TOV, true)
        }
        deliverIfEnabled(app, intent, prefs)
        val body = shadowOf(notificationManager).allNotifications.single().body()
        assertTrue(body, body.contains("Sunday"))
    }

    @Test
    fun `Israel override for the same Sukkot 2026 dates`() {
        val prefs = UserPreferences(notifyCandleLighting = true, candleLightingNotifyMode = "hours_before", isInIsrael = true)
        val alarms = candleAlarms(prefs, at(LocalDate.of(2026, 9, 24), 6, 0), lookahead = 10)
        val d = { day: Int, month: Int -> LocalDate.of(2026, month, day) }
        assertEquals(setOf(d(25, 9), d(2, 10)), alarms.keys)
        assertEquals(d(26, 9), alarms.getValue(d(25, 9)).end!!.date)
        assertEquals(d(3, 10), alarms.getValue(d(2, 10)).end!!.date)
        assertNotNull(zmanim.getShabbatInfo(d(26, 9), ny, inIsrael = true).havdalahTime)
    }

    @Test
    fun `Diaspora Pesach 2026 Yom Tov into Yom Tov and Yom Tov into Shabbat`() {
        val d = { day: Int -> LocalDate.of(2026, 4, day) }
        // Wed 1 Apr: erev Pesach, before sunset
        val wed = zmanim.getShabbatInfo(d(1), ny, 18.0, inIsrael = false)
        assertNear(sunset(d(1)).minusMinutes(18), wed.candleLightingTime!!, "erev Pesach")
        assertNull(wed.havdalahTime)
        // Thu 2 Apr: Yom Tov I -> Yom Tov II, after nightfall, no havdalah
        val thu = zmanim.getShabbatInfo(d(2), ny, 18.0, inIsrael = false)
        assertEquals(zmanim.getTzaitTime(d(2), ny), thu.candleLightingTime)
        assertNull(thu.havdalahTime)
        // Fri 3 Apr: Yom Tov II -> Shabbat, before sunset
        val fri = zmanim.getShabbatInfo(d(3), ny, 18.0, inIsrael = false)
        assertNear(sunset(d(3)).minusMinutes(18), fri.candleLightingTime!!, "Yom Tov into Shabbat")
        assertNull(fri.havdalahTime)
        // Sat 4 Apr: havdalah
        val sat = zmanim.getShabbatInfo(d(4), ny, 18.0, inIsrael = false)
        assertNull(sat.candleLightingTime)
        assertNotNull(sat.havdalahTime)
        assertEquals(d(4), zmanim.getObservanceEnd(d(1), ny, false)!!.date)

        // Israel: Yom Tov Thursday ends with havdalah; ordinary Friday candles
        val thuIl = zmanim.getShabbatInfo(d(2), ny, 18.0, inIsrael = true)
        assertNull(thuIl.candleLightingTime)
        assertNotNull(thuIl.havdalahTime)
        assertEquals(d(2), zmanim.getObservanceEnd(d(1), ny, true)!!.date)
        assertNear(sunset(d(3)).minusMinutes(18), zmanim.getShabbatInfo(d(3), ny, 18.0, true).candleLightingTime!!, "IL Friday")
    }

    @Test
    fun `Rosh Hashana 5787 Shabbat into Yom Tov and Shavuot 2026 Friday`() {
        // RH: Fri 11 Sep evening -> Sat 12 (RH I, Shabbat) -> Sun 13 (RH II), both Israel and Diaspora
        for (israel in listOf(false, true)) {
            val sat = zmanim.getShabbatInfo(LocalDate.of(2026, 9, 12), ny, 18.0, israel)
            assertEquals(zmanim.getTzaitTime(LocalDate.of(2026, 9, 12), ny), sat.candleLightingTime)
            assertNull(sat.havdalahTime)
            assertEquals(LocalDate.of(2026, 9, 13), zmanim.getObservanceEnd(LocalDate.of(2026, 9, 11), ny, israel)!!.date)
        }
        // Shavuot Diaspora: Fri 22 May (YT I) -> Shabbat 23 May (YT II): before sunset on Friday
        val fri = zmanim.getShabbatInfo(LocalDate.of(2026, 5, 22), ny, 18.0, false)
        assertNear(sunset(LocalDate.of(2026, 5, 22)).minusMinutes(18), fri.candleLightingTime!!, "Shavuot Friday")
        assertNull(fri.havdalahTime)
        assertEquals(LocalDate.of(2026, 5, 23), zmanim.getObservanceEnd(LocalDate.of(2026, 5, 21), ny, false)!!.date)
    }

    @Test
    fun `ordinary Friday and Saturday unchanged`() {
        val fri = zmanim.getShabbatInfo(LocalDate.of(2026, 10, 23), ny)
        assertNear(sunset(LocalDate.of(2026, 10, 23)).minusMinutes(18), fri.candleLightingTime!!, "Friday")
        assertNull(fri.havdalahTime)
        val sat = zmanim.getShabbatInfo(LocalDate.of(2026, 10, 24), ny)
        assertNull(sat.candleLightingTime)
        assertEquals(zmanim.getTzaitTime(LocalDate.of(2026, 10, 24), ny), sat.havdalahTime)
    }

    @Test
    fun `calendar candle indicator agrees with candle lighting over 2025-2030`() {
        val mismatches = mutableListOf<String>()
        for (israel in listOf(false, true)) {
            var date = LocalDate.of(2025, 1, 1)
            while (date.isBefore(LocalDate.of(2031, 1, 1))) {
                val indicator = calendar.getDayInfo(date, israel).hebrewDay.hasCandles
                val candles = zmanim.getShabbatInfo(date, ny, 18.0, israel).candleLightingTime != null
                if (indicator != candles) mismatches += "$date israel=$israel indicator=$indicator candles=$candles"
                date = date.plusDays(1)
            }
        }
        assertEquals(emptyList<String>(), mismatches)
    }

    // ---- Omer ----

    @Test
    fun `Omer counts on first night, ordinary days, last night and after`() {
        val prefs = UserPreferences(notifyOmer = true, notifyOmerTzait = true, notifyOmerMorning = true)
        val early = plan(at(LocalDate.of(2026, 3, 31), 6, 0), prefs).alarms
        val evenings = early.filterIsInstance<ReminderAlarm.OmerEvening>().associate { it.date to it.omerDay }
        val mornings = early.filterIsInstance<ReminderAlarm.OmerMorning>().associate { it.date to it.omerDay }
        assertNull("erev Pesach evening: no count", evenings[LocalDate.of(2026, 4, 1)])
        assertEquals(1, evenings[LocalDate.of(2026, 4, 2)])
        assertEquals(2, evenings[LocalDate.of(2026, 4, 3)])
        assertNull(mornings[LocalDate.of(2026, 4, 2)])
        assertEquals(1, mornings[LocalDate.of(2026, 4, 3)])
        assertEquals(5, mornings[LocalDate.of(2026, 4, 7)])

        val late = plan(at(LocalDate.of(2026, 5, 18), 6, 0), prefs).alarms
        val lateEvenings = late.filterIsInstance<ReminderAlarm.OmerEvening>().associate { it.date to it.omerDay }
        val lateMornings = late.filterIsInstance<ReminderAlarm.OmerMorning>().associate { it.date to it.omerDay }
        assertEquals(49, lateEvenings[LocalDate.of(2026, 5, 20)])
        assertNull("erev Shavuot: no count", lateEvenings[LocalDate.of(2026, 5, 21)])
        assertEquals(49, lateMornings[LocalDate.of(2026, 5, 21)])
        assertNull(lateMornings[LocalDate.of(2026, 5, 22)])
    }

    @Test
    fun `morning Omer reminder survives when tzait is unavailable`() {
        val tromso = Location(69.6492, 18.9553, timezoneId = "Europe/Oslo", name = "Tromso")
        val date = LocalDate.of(2026, 5, 15)
        assertNull("precondition: no tzait", zmanim.getTzaitTime(date, tromso))
        val prefs = UserPreferences(notifyOmer = true, notifyOmerTzait = true, notifyOmerMorning = true)
        val now = ZonedDateTime.of(date, LocalTime.of(5, 0), ZoneId.of("Europe/Oslo"))
        val alarms = plan(now, prefs, tromso).alarms
        assertTrue(alarms.any { it is ReminderAlarm.OmerMorning && it.date == date })
        assertFalse(alarms.any { it is ReminderAlarm.OmerEvening && it.date == date })
    }

    @Test
    fun `Omer evening and next morning are distinct alarms and notifications`() {
        val prefs = UserPreferences(notifyOmer = true, notifyOmerTzait = true, notifyOmerMorning = true)
        val now = at(LocalDate.of(2026, 4, 10), 6, 0)
        scheduler(now).scheduleAll(plan(now, prefs))
        val evening = pending(NotificationAlarmScheduler.ACTION_OMER, LocalDate.of(2026, 4, 10))!!
        val nextMorning = pending(NotificationAlarmScheduler.ACTION_OMER_MORNING, LocalDate.of(2026, 4, 11))!!
        val sameMorning = pending(NotificationAlarmScheduler.ACTION_OMER_MORNING, LocalDate.of(2026, 4, 10))!!
        // Omer day 1 = 16 Nisan = 3 Apr 2026, so 10 Apr = day 8; its evening starts day 9
        assertEquals(9, evening.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0))
        assertEquals(9, nextMorning.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0))
        assertEquals(8, sameMorning.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0))
        listOf(sameMorning, evening, nextMorning).forEach { deliverIfEnabled(app, it, prefs) }
        assertEquals(3, shadowOf(notificationManager).allNotifications.size)
        // Re-delivering the same reminder replaces rather than duplicates
        deliverIfEnabled(app, evening, prefs)
        assertEquals(3, shadowOf(notificationManager).allNotifications.size)

        // Omer-morning off: its pending alarms are cancelled, evenings kept, stale morning blocked
        val noMorning = prefs.copy(notifyOmerMorning = false)
        scheduler(now).scheduleAll(plan(now, noMorning))
        assertNull(pending(NotificationAlarmScheduler.ACTION_OMER_MORNING, LocalDate.of(2026, 4, 11)))
        assertNotNull(pending(NotificationAlarmScheduler.ACTION_OMER, LocalDate.of(2026, 4, 11)))
        assertFalse(isAlarmEnabled(NotificationAlarmScheduler.ACTION_OMER_MORNING, noMorning))
    }

    @Test
    fun `changing reminder time replaces pending alarms instead of adding`() {
        val prefs = UserPreferences(notifyOmer = true, notifyOmerTzait = false, notifyOmerMorning = true)
        val now = at(LocalDate.of(2026, 4, 10), 6, 0)
        scheduler(now).scheduleAll(plan(now, prefs))
        val count = shadowOf(alarmManager).scheduledAlarms.size
        assertEquals(8, count)
        scheduler(now).scheduleAll(plan(now, prefs.copy(notifyOmerMorningTime = 9 * 60)))
        assertEquals(count, shadowOf(alarmManager).scheduledAlarms.size)
        val alarm = shadowOf(alarmManager).scheduledAlarms.first {
            shadowOf(it.operation).savedIntent.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, -1) ==
                LocalDate.of(2026, 4, 12).toEpochDay()
        }
        assertEquals(at(LocalDate.of(2026, 4, 12), 9, 0).toInstant().toEpochMilli(), alarm.triggerAtTime)
    }
}
