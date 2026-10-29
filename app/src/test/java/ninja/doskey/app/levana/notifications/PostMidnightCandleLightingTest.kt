package ninja.doskey.app.levana.notifications

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.abs

/**
 * Hours-before candle-lighting reminders when lighting is at nightfall after civil midnight
 * (levana-yal). Expected instants are written out from KosherJava 2.5.0 for real cases, not
 * derived through the planner.
 */
class PostMidnightCandleLightingTest {

    private val zmanim = ZmanimRepository()
    private val calendar = CalendarRepository()

    private val tartu = Location(58.3780, 26.7290, timezoneId = "Europe/Tallinn", name = "Tartu")
    private val tartuZone = ZoneId.of(tartu.timezoneId)
    private val stPetersburg = Location(59.9343, 30.3351, timezoneId = "Europe/Moscow", name = "St. Petersburg")
    private val newYork = Location(40.7128, -74.0060, timezoneId = "America/New_York", name = "New York")

    /** Wed 31 May 2028, Erev Shavuot day 2 in the Diaspora: lit at nightfall, 1 Jun 00:08:25. */
    private val shavuot2028Eve2 = LocalDate.of(2028, 5, 31)
    private val shavuot2028Lighting = OffsetDateTime.parse("2028-06-01T00:08:25.617+03:00").toInstant()

    private fun prefs(hoursBefore: Int = 2, inIsrael: Boolean = false, mode: String = "hours_before") =
        UserPreferences(
            notifyCandleLighting = true,
            candleLightingNotifyMode = mode,
            candleLightingHoursBefore = hoursBefore,
            candleLightingMorningTime = 9 * 60,
            isInIsrael = inIsrael,
            notifyOmer = false
        )

    private fun plan(
        today: LocalDate,
        at: LocalTime,
        prefs: UserPreferences,
        location: Location = tartu,
        deviceZone: ZoneId = ZoneId.of(location.timezoneId)
    ) = planReminderAlarms(
        today,
        ZonedDateTime.of(today, at, deviceZone).toInstant(),
        deviceZone,
        location,
        prefs,
        zmanim,
        calendar
    )

    private fun ReminderAlarmPlan.candles() =
        (alarms + overdue).filterIsInstance<ReminderAlarm.CandleLighting>()

    private fun assertNear(expected: Instant, actual: Instant) =
        assertTrue("expected $expected but was $actual", abs(Duration.between(expected, actual).toMillis()) < 1000)

    @Test
    fun `repository returns the actual post-midnight candle-lighting instant`() {
        val instant = zmanim.getCandleLightingInstant(shavuot2028Eve2, tartu, 18.0, inIsrael = false)!!
        assertNear(shavuot2028Lighting, instant)
        assertEquals(LocalDate.of(2028, 6, 1), instant.atZone(tartuZone).toLocalDate())
        // The display time is unchanged: the same wall time, without its date
        assertEquals(
            LocalTime.of(0, 8),
            zmanim.getShabbatInfo(shavuot2028Eve2, tartu, 18.0, false).candleLightingTime!!.withSecond(0).withNano(0)
        )
    }

    @Test
    fun `hours-before reminder fires before the next-day lighting, keyed on the source evening`() {
        val alarm = plan(shavuot2028Eve2, LocalTime.NOON, prefs(hoursBefore = 2)).alarms
            .filterIsInstance<ReminderAlarm.CandleLighting>()
            .single { it.date == shavuot2028Eve2 }

        // 2 hours before 1 Jun 00:08:25 is 31 May 22:08:25 — not 30 May 22:08:25
        assertNear(OffsetDateTime.parse("2028-05-31T22:08:25.617+03:00").toInstant(), alarm.triggerAt)
        assertNotEquals(LocalDate.of(2028, 5, 30), alarm.triggerAt.atZone(tartuZone).toLocalDate())
        assertNear(shavuot2028Lighting, alarm.candleLightingAt)
        assertEquals(LocalTime.of(0, 8), alarm.candleLightingTime.withSecond(0).withNano(0))
        assertEquals(shavuot2028Eve2, alarm.date)
    }

    @Test
    fun `old LocalTime reconstruction would have fired a day early`() {
        val lighting = zmanim.getShabbatInfo(shavuot2028Eve2, tartu, 18.0, false).candleLightingTime!!
        val reconstructed = ZonedDateTime.of(shavuot2028Eve2, lighting, tartuZone).minusHours(2).toInstant()
        val actual = plan(shavuot2028Eve2, LocalTime.NOON, prefs()).candles().single { it.date == shavuot2028Eve2 }
        assertNear(reconstructed.plus(Duration.ofDays(1)), actual.triggerAt)
    }

    @Test
    fun `run after midnight keeps the source evening's reminder until the candles are lit`() {
        val june1 = LocalDate.of(2028, 6, 1)
        val hoursBefore1 = prefs(hoursBefore = 1)

        // 22:30 on 31 May: the 1-hour reminder (23:08:25) is still ahead
        val evening = plan(shavuot2028Eve2, LocalTime.of(22, 30), hoursBefore1)
        val future = evening.alarms.filterIsInstance<ReminderAlarm.CandleLighting>().single { it.date == shavuot2028Eve2 }
        assertNear(OffsetDateTime.parse("2028-05-31T23:08:25.617+03:00").toInstant(), future.triggerAt)

        // 00:01 on 1 Jun: source evening is yesterday, lighting not yet — kept as overdue
        val afterMidnight = plan(june1, LocalTime.of(0, 1), hoursBefore1)
        assertEquals(shavuot2028Eve2, afterMidnight.previousEvening)
        val kept = afterMidnight.overdue.filterIsInstance<ReminderAlarm.CandleLighting>().single()
        assertEquals(shavuot2028Eve2, kept.date)
        assertNear(future.triggerAt, kept.triggerAt)
        assertTrue(afterMidnight.alarms.none { it is ReminderAlarm.CandleLighting && it.date == shavuot2028Eve2 })

        // 00:10, after lighting: no longer wanted at all
        val lit = plan(june1, LocalTime.of(0, 10), hoursBefore1)
        assertTrue(lit.candles().none { it.date == shavuot2028Eve2 })
    }

    @Test
    fun `consecutive post-midnight lightings are separate reminders from their own instants`() {
        // Sat 31 May 2036: Shabbat into Shavuot; Sun 1 Jun: Shavuot day 1 into day 2 (Diaspora)
        val sat = LocalDate.of(2036, 5, 31)
        val sun = LocalDate.of(2036, 6, 1)
        val candles = plan(LocalDate.of(2036, 5, 30), LocalTime.NOON, prefs(hoursBefore = 3)).candles()
            .filter { it.date == sat || it.date == sun }
        assertEquals(listOf(sat, sun), candles.map { it.date })
        assertNear(OffsetDateTime.parse("2036-06-01T00:08:34.473+03:00").toInstant(), candles[0].candleLightingAt)
        assertNear(OffsetDateTime.parse("2036-06-02T00:12:47.909+03:00").toInstant(), candles[1].candleLightingAt)
        assertNear(OffsetDateTime.parse("2036-05-31T21:08:34.473+03:00").toInstant(), candles[0].triggerAt)
        assertNear(OffsetDateTime.parse("2036-06-01T21:12:47.909+03:00").toInstant(), candles[1].triggerAt)

        // Israel: one-day Shavuot, so only the Saturday-night lighting
        val israel = plan(LocalDate.of(2036, 5, 30), LocalTime.NOON, prefs(hoursBefore = 3, inIsrael = true)).candles()
            .filter { it.date == sat || it.date == sun }
        assertEquals(listOf(sat), israel.map { it.date })
        assertNear(candles[0].triggerAt, israel.single().triggerAt)
    }

    @Test
    fun `ordinary sunset lighting is unchanged`() {
        val friday = LocalDate.of(2026, 11, 13)
        val alarm = plan(friday, LocalTime.of(8, 0), prefs(hoursBefore = 2), location = newYork).alarms
            .filterIsInstance<ReminderAlarm.CandleLighting>().single { it.date == friday }
        val expected = kosherJavaCandleLighting(newYork, friday).minus(Duration.ofHours(2))
        assertEquals(expected, alarm.triggerAt)
        assertEquals(friday, alarm.triggerAt.atZone(ZoneId.of(newYork.timezoneId)).toLocalDate())
        // Around 4:20 PM EST, so 2:20 PM
        val wall = alarm.triggerAt.atZone(ZoneId.of(newYork.timezoneId)).toLocalTime()
        assertTrue("$wall", wall.isAfter(LocalTime.of(14, 10)) && wall.isBefore(LocalTime.of(14, 30)))
    }

    @Test
    fun `hours-before triggers follow DST at the device position whatever the device zone`() {
        // EU spring-forward on Sun 29 Mar 2026: Fridays 27 Mar (+02:00) and 3 Apr (+03:00)
        val tallinn = Location(59.4370, 24.7536, timezoneId = "Europe/Tallinn", name = "Tallinn")
        val today = LocalDate.of(2026, 3, 27)
        val fromDevice = plan(today, LocalTime.of(1, 0), prefs(hoursBefore = 2), location = tallinn,
            deviceZone = ZoneId.of("America/New_York")).candles()
        val local = plan(today, LocalTime.of(6, 0), prefs(hoursBefore = 2), location = tallinn).candles()
        for (friday in listOf(LocalDate.of(2026, 3, 27), LocalDate.of(2026, 4, 3))) {
            val expected = kosherJavaCandleLighting(tallinn, friday).minus(Duration.ofHours(2))
            assertEquals(expected, local.single { it.date == friday }.triggerAt)
            assertEquals(expected, fromDevice.single { it.date == friday }.triggerAt)
        }
        assertEquals("+02:00", local.single { it.date == LocalDate.of(2026, 3, 27) }.triggerAt.atZone(ZoneId.of("Europe/Tallinn")).offset.id)
        assertEquals("+03:00", local.single { it.date == LocalDate.of(2026, 4, 3) }.triggerAt.atZone(ZoneId.of("Europe/Tallinn")).offset.id)

        // The post-midnight case planned from a device in New York fires at the same instant
        val tartuFromNy = plan(LocalDate.of(2028, 5, 31), LocalTime.of(5, 0), prefs(), deviceZone = ZoneId.of("America/New_York"))
            .candles().single { it.date == shavuot2028Eve2 }
        assertNear(OffsetDateTime.parse("2028-05-31T22:08:25.617+03:00").toInstant(), tartuFromNy.triggerAt)
    }

    @Test
    fun `no reminder when nightfall is unavailable`() {
        // White nights: no nightfall in St. Petersburg on 31 May 2028
        assertNull(zmanim.getCandleLightingInstant(shavuot2028Eve2, stPetersburg, 18.0, false))
        val candles = plan(shavuot2028Eve2, LocalTime.NOON, prefs(), location = stPetersburg).candles()
        assertTrue(candles.none { it.date == shavuot2028Eve2 })
        // ...and none for the previous evening after midnight either
        val after = plan(LocalDate.of(2028, 6, 1), LocalTime.of(0, 5), prefs(), location = stPetersburg).candles()
        assertTrue(after.none { it.date == shavuot2028Eve2 })
    }

    @Test
    fun `morning mode keeps its morning trigger on the source date`() {
        val alarm = plan(shavuot2028Eve2, LocalTime.of(6, 0), prefs(mode = "morning")).alarms
            .filterIsInstance<ReminderAlarm.CandleLighting>().single { it.date == shavuot2028Eve2 }
        assertEquals(ZonedDateTime.of(shavuot2028Eve2, LocalTime.of(9, 0), tartuZone).toInstant(), alarm.triggerAt)
        assertNear(shavuot2028Lighting, alarm.candleLightingAt)
    }

    /** Sea-level sunset − 18 min straight from KosherJava, bypassing the repository. */
    private fun kosherJavaCandleLighting(location: Location, date: LocalDate): Instant {
        val tz = TimeZone.getTimeZone(location.timezoneId)
        val czc = ComplexZmanimCalendar(GeoLocation(location.name, location.latitude, location.longitude, 0.0, tz))
        czc.calendar = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth).also { it.timeZone = tz }
        czc.candleLightingOffset = 18.0
        return czc.candleLighting.toInstant()
    }
}
