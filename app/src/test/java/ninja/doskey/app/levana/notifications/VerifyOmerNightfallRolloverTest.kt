package ninja.doskey.app.levana.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
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
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Independent verification of evening Omer reminders whose nightfall falls after civil
 * midnight. Uses only APIs that also exist at the baseline, so the same file can be run
 * against the pre-fix code. Nightfall oracle: KosherJava tzais computed directly here, plus
 * hard-coded anchor instants and the property that the evening of D fires between noon of D
 * and noon of D + 1 (location time).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class VerifyOmerNightfallRolloverTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)
    private val zmanim = ZmanimRepository()
    private val calendar = CalendarRepository()

    private val stPetersburg = Location(59.9343, 30.3351, timezoneId = "Europe/Moscow", name = "St Petersburg")
    private val nome = Location(64.5011, -165.4064, timezoneId = "America/Nome", name = "Nome")
    private val stornoway = Location(58.2093, -6.3865, timezoneId = "Europe/London", name = "Stornoway")
    private val helsinki = Location(60.1699, 24.9384, timezoneId = "Europe/Helsinki", name = "Helsinki")
    private val casablanca = Location(33.5731, -7.5898, timezoneId = "Africa/Casablanca", name = "Casablanca")

    /** Evening on which the count of 1 is said. */
    private val firstEvening2027 = LocalDate.of(2027, 4, 22)
    private val firstEvening2020 = LocalDate.of(2020, 4, 9)

    private val omerPrefs = UserPreferences(
        notifyCandleLighting = false,
        notifyOmer = true,
        notifyOmerTzait = true,
        notifyOmerMorning = false
    )

    @Before
    fun setUp() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.createAll(context)
    }

    // ---- oracle -------------------------------------------------------------------------

    private fun nightfall(date: LocalDate, loc: Location): Instant? {
        val tz = TimeZone.getTimeZone(loc.timezoneId)
        val czc = ComplexZmanimCalendar(GeoLocation(loc.name, loc.latitude, loc.longitude, loc.elevation, tz))
        czc.calendar = GregorianCalendar(tz).apply {
            clear()
            set(date.year, date.monthValue - 1, date.dayOfMonth)
        }
        return czc.tzais?.toInstant()
    }

    private fun count(evening: LocalDate, first: LocalDate): Int? =
        (ChronoUnit.DAYS.between(first, evening) + 1).toInt().takeIf { it in 1..49 }

    private fun at(loc: Location, date: LocalDate, time: LocalTime): Instant =
        ZonedDateTime.of(date, time, ZoneId.of(loc.timezoneId)).toInstant()

    private fun at(loc: Location, iso: String): Instant =
        LocalDateTime.parse(iso).atZone(ZoneId.of(loc.timezoneId)).toInstant()

    // ---- harness ------------------------------------------------------------------------

    private data class Pending(
        val action: String,
        val date: LocalDate,
        val trigger: Instant,
        val omerDay: Int,
        val uri: String,
        val raw: ShadowAlarmManager.ScheduledAlarm
    )

    @Suppress("DEPRECATION")
    private fun pending(): List<Pending> = shadowOf(alarmManager).scheduledAlarms.map {
        val intent = shadowOf(it.operation).savedIntent
        Pending(
            intent.action!!,
            LocalDate.ofEpochDay(intent.getLongExtra(NotificationAlarmScheduler.EXTRA_DATE_EPOCH_DAY, -1)),
            Instant.ofEpochMilli(it.triggerAtTime),
            intent.getIntExtra(NotificationAlarmScheduler.EXTRA_OMER_DAY, 0),
            intent.dataString.orEmpty(),
            it
        )
    }

    private fun evenings() = pending().filter { it.action == NotificationAlarmScheduler.ACTION_OMER }
    private fun mornings() = pending().filter { it.action == NotificationAlarmScheduler.ACTION_OMER_MORNING }

    /** Worker path: plan in [deviceZone] at [now] and apply with the production helper. */
    private fun workerRun(loc: Location, now: Instant, prefs: UserPreferences = omerPrefs, deviceZone: ZoneId = ZoneId.of(loc.timezoneId)) {
        val today = now.atZone(deviceZone).toLocalDate()
        val plan = planReminderAlarms(today, now, deviceZone, loc, prefs, zmanim, calendar)
        runBlocking {
            applyReminderAlarmPlan(plan, NotificationAlarmScheduler(context) { now.toEpochMilli() }, NotificationDeliveryLog(context)) {}
        }
    }

    /** Settings-change path: the rebuilder with a device clock. */
    private fun rebuild(loc: Location, now: Instant, prefs: UserPreferences = omerPrefs, deviceZone: ZoneId = ZoneId.of(loc.timezoneId)) {
        runBlocking {
            NotificationAlarmRebuilder(
                flowOf(prefs.copy(currentLocation = loc)),
                zmanim,
                calendar,
                NotificationAlarmScheduler(context) { now.toEpochMilli() },
                Clock.fixed(now, deviceZone)
            ).rebuild()
        }
    }

    /** Asserts every pending evening alarm fires at its own evening's nightfall with its count. */
    private fun assertEveningsCorrect(loc: Location, first: LocalDate) {
        val zone = ZoneId.of(loc.timezoneId)
        val evenings = evenings()
        assertEquals("one alarm per evening", evenings.size, evenings.map { it.date }.toSet().size)
        assertEquals("one alarm per count", evenings.size, evenings.map { it.omerDay }.toSet().size)
        for (e in evenings) {
            val expected = nightfall(e.date, loc)
            assertNotNull("alarm for ${e.date} without nightfall", expected)
            assertEquals("trigger of ${e.date}", expected!!.toEpochMilli(), e.trigger.toEpochMilli())
            assertEquals("count of ${e.date}", count(e.date, first), e.omerDay)
            val local = e.trigger.atZone(zone).toLocalDateTime()
            assertTrue(
                "${e.date} fires at $local, outside its evening",
                local.isAfter(e.date.atTime(12, 0)) && local.isBefore(e.date.plusDays(1).atTime(12, 0))
            )
            assertEquals(NotificationAlarmScheduler.alarmUri(NotificationAlarmScheduler.ACTION_OMER, e.date).toString(), e.uri)
        }
    }

    // ---- oracle sanity ------------------------------------------------------------------

    @Test
    fun `oracle anchors match the bug report and fixtures`() {
        fun local(loc: Location, d: LocalDate) =
            nightfall(d, loc)!!.atZone(ZoneId.of(loc.timezoneId)).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS)
        assertEquals(LocalDateTime.parse("2027-05-25T00:02:42"), local(stPetersburg, LocalDate.of(2027, 5, 24)))
        assertEquals(LocalDateTime.parse("2027-04-23T00:39:02"), local(nome, firstEvening2027))
        assertEquals(LocalDateTime.parse("2027-06-10T00:50:32"), local(stornoway, LocalDate.of(2027, 6, 9)))
        assertNull(nightfall(LocalDate.of(2027, 5, 29), stPetersburg))
        // The production repository agrees on the wall-clock time; only the date was lost
        assertEquals(LocalTime.of(0, 2, 42), zmanim.getTzaitTime(LocalDate.of(2027, 5, 24), stPetersburg)!!.withNano(0))
        assertEquals(33, calendar.getDayInfo(LocalDate.of(2027, 5, 25), inIsrael = false).omerDay)
    }

    // ---- reported example and consecutive post-midnight nights ----------------------------

    @Test
    fun `St Petersburg evening of May 24 carries count 33 and fires on May 25 at 00 02 42`() {
        workerRun(stPetersburg, at(stPetersburg, "2027-05-24T12:00"))

        val may24 = evenings().single { it.date == LocalDate.of(2027, 5, 24) }
        assertEquals(33, may24.omerDay)
        assertEquals(LocalDateTime.parse("2027-05-25T00:02:42"),
            may24.trigger.atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS))
        // Nothing is set for 24 May 00:02, and count 34 is not what fires that night
        assertTrue(evenings().none { it.trigger.isBefore(at(stPetersburg, "2027-05-24T12:00")) })
        assertEquals(34, evenings().single { it.date == LocalDate.of(2027, 5, 25) }.omerDay)
        assertTrue(evenings().filter { it.omerDay == 34 }.all { it.trigger.isAfter(may24.trigger) })
        assertEveningsCorrect(stPetersburg, firstEvening2027)
    }

    @Test
    fun `consecutive post-midnight nights each fire at their own nightfall, then stop when nightfall disappears`() {
        workerRun(stPetersburg, at(stPetersburg, "2027-05-23T18:00"))

        // 23 May (23:56) is before midnight; 24..28 May are after; 29 May onwards has no nightfall
        val expected = (23..28).map { LocalDate.of(2027, 5, it) }
        assertEquals(expected, evenings().map { it.date }.sorted())
        assertEquals((32..37).toList(), evenings().sortedBy { it.trigger }.map { it.omerDay })
        assertEveningsCorrect(stPetersburg, firstEvening2027)
    }

    @Test
    fun `ordinary latitude keeps evening alarms on the same civil date`() {
        val jerusalem = Location.JERUSALEM
        workerRun(jerusalem, at(jerusalem, "2027-05-01T08:00"))
        assertEquals(8, evenings().size)
        assertEveningsCorrect(jerusalem, firstEvening2027)
        val zone = ZoneId.of(jerusalem.timezoneId)
        assertTrue(evenings().all { it.trigger.atZone(zone).toLocalDate() == it.date })
    }

    // ---- runs around midnight -----------------------------------------------------------

    @Test
    fun `first run between midnight and the previous evening's nightfall schedules that evening`() {
        val now = at(stPetersburg, "2027-05-25T00:01")
        workerRun(stPetersburg, now)

        val prev = evenings().single { it.date == LocalDate.of(2027, 5, 24) }
        assertEquals(33, prev.omerDay)
        assertEquals(nightfall(LocalDate.of(2027, 5, 24), stPetersburg), prev.trigger.truncatedTo(ChronoUnit.MILLIS))
        assertTrue(pending().all { it.trigger.isAfter(now) })
        assertEveningsCorrect(stPetersburg, firstEvening2027)
    }

    @Test
    fun `first run after the previous evening's nightfall schedules nothing for it`() {
        workerRun(stPetersburg, at(stPetersburg, "2027-05-25T00:05"))

        assertTrue(evenings().none { it.date == LocalDate.of(2027, 5, 24) })
        assertEquals(34, evenings().single { it.date == LocalDate.of(2027, 5, 25) }.omerDay)
        assertEveningsCorrect(stPetersburg, firstEvening2027)
    }

    @Test
    fun `overdue undelivered previous-evening alarm survives later runs without duplication`() {
        workerRun(stPetersburg, at(stPetersburg, "2027-05-24T12:00"))
        val original = evenings().single { it.date == LocalDate.of(2027, 5, 24) }

        // Android has not delivered it yet (e.g. Doze); runs after its nightfall must keep it
        workerRun(stPetersburg, at(stPetersburg, "2027-05-25T00:05"))
        rebuild(stPetersburg, at(stPetersburg, "2027-05-25T03:00"))

        val kept = evenings().single { it.date == LocalDate.of(2027, 5, 24) }
        assertEquals(original.trigger, kept.trigger)
        assertEquals(33, kept.omerDay)
        assertEveningsCorrect(stPetersburg, firstEvening2027)
    }

    /**
     * Simulates a week of runs at 00:01 / 06:00 / 12:00 / 18:00 with alarms firing in between,
     * and checks every evening is delivered exactly once with its own count at its nightfall.
     */
    @Test
    fun `simulated week delivers each evening once with the right count`() {
        val zone = ZoneId.of(stPetersburg.timezoneId)
        val delivered = mutableListOf<Pending>()
        var day = LocalDate.of(2027, 5, 20)
        while (day.isBefore(LocalDate.of(2027, 6, 1))) {
            for (time in listOf(LocalTime.of(0, 1), LocalTime.of(6, 0), LocalTime.of(12, 0), LocalTime.of(18, 0))) {
                val now = ZonedDateTime.of(day, time, zone).toInstant()
                pending().filter { !it.trigger.isAfter(now) }.forEach {
                    delivered += it
                    alarmManager.cancel(it.raw.operation!!)
                }
                workerRun(stPetersburg, now)
                assertEveningsCorrect(stPetersburg, firstEvening2027)
                // Completeness: every evening whose nightfall is still ahead in the window is pending
                for (offset in -1..REMINDER_LOOKAHEAD_DAYS) {
                    val d = day.plusDays(offset.toLong())
                    val nf = nightfall(d, stPetersburg) ?: continue
                    if (nf.isAfter(now) && count(d, firstEvening2027) != null) {
                        assertTrue("missing evening $d at $now", evenings().any { it.date == d })
                    }
                }
            }
            day = day.plusDays(1)
        }
        val firedEvenings = delivered.filter { it.action == NotificationAlarmScheduler.ACTION_OMER }
        assertEquals(dates(LocalDate.of(2027, 5, 20), LocalDate.of(2027, 5, 28)), firedEvenings.map { it.date })
        assertEquals((29..37).toList(), firedEvenings.map { it.omerDay })
        firedEvenings.forEach {
            assertEquals(nightfall(it.date, stPetersburg)!!.toEpochMilli(), it.trigger.toEpochMilli())
        }
    }

    private fun dates(from: LocalDate, to: LocalDate): List<LocalDate> =
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()

    // ---- season edges -------------------------------------------------------------------

    @Test
    fun `first night after midnight in Nome carries count 1`() {
        workerRun(nome, at(nome, "2027-04-21T12:00"))
        val first = evenings().single { it.date == firstEvening2027 }
        assertEquals(1, first.omerDay)
        assertEquals(LocalDateTime.parse("2027-04-23T00:39:02"),
            first.trigger.atZone(ZoneId.of(nome.timezoneId)).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS))
        assertTrue("no evening alarm before the season", evenings().none { it.date.isBefore(firstEvening2027) })
        assertEveningsCorrect(nome, firstEvening2027)
    }

    @Test
    fun `run after midnight before the first night's nightfall still schedules count 1`() {
        workerRun(nome, at(nome, "2027-04-23T00:30"))
        assertEquals(1, evenings().single { it.date == firstEvening2027 }.omerDay)
        assertEquals(2, evenings().single { it.date == firstEvening2027.plusDays(1) }.omerDay)
        assertEveningsCorrect(nome, firstEvening2027)
    }

    @Test
    fun `last night after midnight in Stornoway carries count 49 and nothing follows`() {
        workerRun(stornoway, at(stornoway, "2027-06-09T12:00"))
        val last = evenings().single { it.date == LocalDate.of(2027, 6, 9) }
        assertEquals(49, last.omerDay)
        assertEquals(LocalDateTime.parse("2027-06-10T00:50:32"),
            last.trigger.atZone(ZoneId.of(stornoway.timezoneId)).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS))
        assertEquals(listOf(LocalDate.of(2027, 6, 9)), evenings().map { it.date })
        assertEveningsCorrect(stornoway, firstEvening2027)
    }

    @Test
    fun `run between midnight and the last nightfall keeps only count 49`() {
        val prefs = omerPrefs.copy(notifyOmerMorning = true)
        workerRun(stornoway, at(stornoway, "2027-06-10T00:30"), prefs)
        assertEquals(listOf(49), evenings().map { it.omerDay })
        assertEquals(LocalDate.of(2027, 6, 9), evenings().single().date)
        // Only the 49th day's morning remains
        assertEquals(listOf(LocalDate.of(2027, 6, 10) to 49), mornings().map { it.date to it.omerDay })
    }

    @Test
    fun `no reminder after the season`() {
        workerRun(stornoway, at(stornoway, "2027-06-10T01:00"), omerPrefs.copy(notifyOmerMorning = true))
        assertTrue(evenings().isEmpty())
        assertEquals(listOf(LocalDate.of(2027, 6, 10) to 49), mornings().map { it.date to it.omerDay })
        workerRun(stornoway, at(stornoway, "2027-06-10T12:00"), omerPrefs.copy(notifyOmerMorning = true))
        assertTrue(pending().none { it.date.isAfter(LocalDate.of(2027, 6, 10)) })
        assertTrue(evenings().isEmpty())
    }

    // ---- missing nightfall --------------------------------------------------------------

    @Test
    fun `missing nightfall drops only the evening reminder and keeps mornings`() {
        val zone = ZoneId.of(stPetersburg.timezoneId)
        val prefs = omerPrefs.copy(notifyOmerMorning = true)
        workerRun(stPetersburg, at(stPetersburg, "2027-05-28T12:00"), prefs)

        assertEquals(listOf(LocalDate.of(2027, 5, 28)), evenings().map { it.date })
        assertEquals(37, evenings().single().omerDay)
        val m = mornings().sortedBy { it.date }
        assertEquals(dates(LocalDate.of(2027, 5, 29), LocalDate.of(2027, 6, 4)), m.map { it.date })
        // The morning of D + 1 is still the day whose count began on the evening of D
        assertEquals((37..43).toList(), m.map { it.omerDay })
        m.forEach { assertEquals(ZonedDateTime.of(it.date, LocalTime.of(7, 0), zone).toInstant(), it.trigger) }
    }

    @Test
    fun `run after midnight with missing previous nightfall still plans mornings`() {
        workerRun(stPetersburg, at(stPetersburg, "2027-05-30T00:30"), omerPrefs.copy(notifyOmerMorning = true))
        assertTrue(evenings().isEmpty())
        assertEquals(38, mornings().single { it.date == LocalDate.of(2027, 5, 30) }.omerDay)
    }

    // ---- horizon ------------------------------------------------------------------------

    @Test
    fun `last window date's post-midnight evening is planned and nothing beyond the window`() {
        val today = LocalDate.of(2027, 5, 21)
        workerRun(stPetersburg, at(stPetersburg, today, LocalTime.NOON))
        val lastDate = today.plusDays(REMINDER_LOOKAHEAD_DAYS.toLong())
        val last = evenings().single { it.date == lastDate }
        assertEquals(37, last.omerDay)
        assertTrue(last.trigger.atZone(ZoneId.of(stPetersburg.timezoneId)).toLocalDate().isAfter(lastDate))
        assertTrue(pending().none { it.date.isAfter(lastDate) })
        assertEveningsCorrect(stPetersburg, firstEvening2027)
    }

    // ---- device vs location zone, DST ---------------------------------------------------

    @Test
    fun `device ahead of location still covers the location's post-midnight evening`() {
        // 00:01 MSK on 25 May = 06:01 on 25 May in Tokyo; also 21:00 MSK on 24 May = 03:00 JST 25 May
        val tokyo = ZoneId.of("Asia/Tokyo")
        rebuild(stPetersburg, at(stPetersburg, "2027-05-24T21:00"), deviceZone = tokyo)
        assertEquals(33, evenings().single { it.date == LocalDate.of(2027, 5, 24) }.omerDay)
        assertEveningsCorrect(stPetersburg, firstEvening2027)

        rebuild(stPetersburg, at(stPetersburg, "2027-05-25T00:01"), deviceZone = tokyo)
        assertEquals(33, evenings().single { it.date == LocalDate.of(2027, 5, 24) }.omerDay)
        assertEveningsCorrect(stPetersburg, firstEvening2027)
    }

    @Test
    fun `device behind location still covers the location's post-midnight evening`() {
        // 00:01 MSK on 25 May = 14:01 PDT on 24 May
        rebuild(stPetersburg, at(stPetersburg, "2027-05-25T00:01"), deviceZone = ZoneId.of("America/Los_Angeles"))
        assertEquals(33, evenings().single { it.date == LocalDate.of(2027, 5, 24) }.omerDay)
        assertEveningsCorrect(stPetersburg, firstEvening2027)
    }

    @Test
    fun `foreign device zone keeps Helsinki post-midnight evenings at their nightfall`() {
        rebuild(helsinki, at(helsinki, "2020-05-24T12:00"), deviceZone = ZoneId.of("Africa/Casablanca"))
        assertEquals(listOf(46, 47, 48), evenings().sortedBy { it.date }.map { it.omerDay })
        assertEveningsCorrect(helsinki, firstEvening2020)
        assertEquals(Instant.parse("2020-05-26T22:11:46Z"),
            evenings().single { it.omerDay == 48 }.trigger.truncatedTo(ChronoUnit.SECONDS))
    }

    @Test
    fun `device-zone DST change in the window moves mornings but not evenings`() {
        // Casablanca moved from +01:00 to +00:00 at 03:00 on 19 April 2020
        val device = ZoneId.of("Africa/Casablanca")
        rebuild(helsinki, at(helsinki, "2020-04-16T12:00"), omerPrefs.copy(notifyOmerMorning = true), device)

        assertEquals(8, evenings().size)
        assertEveningsCorrect(helsinki, firstEvening2020)
        mornings().forEach { assertEquals(ZonedDateTime.of(it.date, LocalTime.of(7, 0), device).toInstant(), it.trigger) }
        val apr18 = mornings().single { it.date == LocalDate.of(2020, 4, 18) }.trigger
        val apr19 = mornings().single { it.date == LocalDate.of(2020, 4, 19) }.trigger
        assertEquals(Duration.ofHours(25), Duration.between(apr18, apr19))
    }

    @Test
    fun `location-zone DST change keeps every evening at its nightfall`() {
        // Casablanca moved from +01:00 to +00:00 at 03:00 on 19 April 2020
        workerRun(casablanca, at(casablanca, "2020-04-16T12:00"))
        assertEquals(8, evenings().size)
        assertEveningsCorrect(casablanca, firstEvening2020)
        val zone = ZoneId.of(casablanca.timezoneId)
        val times = evenings().sortedBy { it.date }.map { it.trigger.atZone(zone).toLocalTime() }
        // Wall clock drops about an hour across the transition, from ~20:3x to ~19:3x
        assertTrue(times.first().isAfter(LocalTime.of(20, 0)) && times.last().isBefore(LocalTime.of(20, 0)))
    }

    // ---- settings: disabling and re-enabling --------------------------------------------

    @Test
    fun `turning the evening reminder off after midnight cancels the previous evening and keeps mornings`() {
        val both = omerPrefs.copy(notifyOmerMorning = true)
        workerRun(stPetersburg, at(stPetersburg, "2027-05-24T12:00"), both)
        val mornings = mornings().map { it.date to it.trigger }.toSet()

        rebuild(stPetersburg, at(stPetersburg, "2027-05-25T00:01"), both.copy(notifyOmerTzait = false))
        assertTrue(evenings().isEmpty())
        // The 24 May 07:00 morning has passed; the rest stay, and a new last-day one may be added
        assertTrue(mornings().map { it.date to it.trigger }.toSet().containsAll(mornings.filter { it.first.isAfter(LocalDate.of(2027, 5, 24)) }))

        rebuild(stPetersburg, at(stPetersburg, "2027-05-25T00:01"), both)
        assertEquals(33, evenings().single { it.date == LocalDate.of(2027, 5, 24) }.omerDay)
        assertEveningsCorrect(stPetersburg, firstEvening2027)
    }

    @Test
    fun `turning the morning reminder off keeps the previous evening's post-midnight alarm`() {
        val both = omerPrefs.copy(notifyOmerMorning = true)
        workerRun(stPetersburg, at(stPetersburg, "2027-05-24T12:00"), both)
        rebuild(stPetersburg, at(stPetersburg, "2027-05-25T00:01"), both.copy(notifyOmerMorning = false))
        assertTrue(mornings().isEmpty())
        assertEquals(33, evenings().single { it.date == LocalDate.of(2027, 5, 24) }.omerDay)
    }

    @Test
    fun `turning all Omer reminders off cancels the previous evening too, even once overdue`() {
        val both = omerPrefs.copy(notifyOmerMorning = true)
        workerRun(stPetersburg, at(stPetersburg, "2027-05-24T12:00"), both)
        rebuild(stPetersburg, at(stPetersburg, "2027-05-25T00:01"), both.copy(notifyOmer = false))
        assertTrue(pending().isEmpty())

        workerRun(stPetersburg, at(stPetersburg, "2027-05-24T12:00"), both)
        // After its nightfall: still undelivered, but no longer wanted
        rebuild(stPetersburg, at(stPetersburg, "2027-05-25T00:10"), both.copy(notifyOmerTzait = false))
        assertTrue(evenings().isEmpty())
    }

    // ---- identity and delivery ----------------------------------------------------------

    @Test
    fun `repeated runs across midnight replace rather than duplicate, and delivery posts count 33`() {
        repeat(3) { workerRun(stPetersburg, at(stPetersburg, "2027-05-24T12:00")) }
        workerRun(stPetersburg, at(stPetersburg, "2027-05-25T00:01"))
        rebuild(stPetersburg, at(stPetersburg, "2027-05-25T00:02"))
        assertEveningsCorrect(stPetersburg, firstEvening2027)
        assertEquals(pending().size, pending().map { it.uri }.toSet().size)

        val alarm = evenings().single { it.date == LocalDate.of(2027, 5, 24) }
        val intent: Intent = shadowOf(alarm.raw.operation).savedIntent
        deliverIfEnabled(context, intent, omerPrefs)
        val posted = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications.single()
        assertTrue(posted.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("33"))
        val tag = shadowOf(context.getSystemService(NotificationManager::class.java)).activeNotifications.single().tag
        assertEquals("omer_tzait/2027-05-24", tag)
    }
}
