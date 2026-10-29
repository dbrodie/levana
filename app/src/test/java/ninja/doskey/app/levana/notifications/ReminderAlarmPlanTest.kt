package ninja.doskey.app.levana.notifications

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ReminderAlarmPlanTest {

    private val zmanim = ZmanimRepository()
    private val calendar = CalendarRepository()

    private val newYork = Location(
        latitude = 40.7128,
        longitude = -74.006,
        timezoneId = "America/New_York",
        name = "New York"
    )
    private val nyZone = ZoneId.of(newYork.timezoneId)

    private val candlePrefs = UserPreferences(
        notifyCandleLighting = true,
        candleLightingNotifyMode = "morning",
        candleLightingMorningTime = 8 * 60
    )
    private val omerPrefs = UserPreferences(
        notifyCandleLighting = false,
        notifyOmer = true,
        notifyOmerTzait = true,
        notifyOmerMorning = true,
        notifyOmerMorningTime = 7 * 60
    )

    private fun plan(
        today: LocalDate,
        now: LocalTime,
        prefs: UserPreferences,
        location: Location = newYork,
        deviceZone: ZoneId = nyZone
    ) = planReminderAlarms(
        today,
        ZonedDateTime.of(today, now, deviceZone).toInstant(),
        deviceZone,
        location,
        prefs,
        zmanim,
        calendar
    )

    private fun hebrew(month: Int, day: Int): LocalDate = JewishCalendar(5786, month, day).localDate

    private inline fun <reified T : ReminderAlarm> ReminderAlarmPlan.of() = alarms.filterIsInstance<T>()

    // --- Candle lighting ---------------------------------------------------------------

    private val friday = LocalDate.of(2026, 10, 23)

    @Test
    fun `run before the morning time schedules today and the following week`() {
        val result = plan(friday, LocalTime.of(6, 0), candlePrefs)
        val candles = result.of<ReminderAlarm.CandleLighting>()

        assertEquals(listOf(friday, friday.plusWeeks(1)), candles.map { it.date })
        candles.forEach {
            assertEquals(ZonedDateTime.of(it.date, LocalTime.of(8, 0), nyZone).toInstant(), it.triggerAt)
        }
        assertNull(result.dueCandleLighting)
    }

    @Test
    fun `run after the morning time posts today's notice and still schedules next week`() {
        val result = plan(friday, LocalTime.of(10, 0), candlePrefs)

        val due = requireNotNull(result.dueCandleLighting)
        assertEquals(friday, due.date)
        assertEquals(zmanim.getShabbatInfo(friday, newYork).candleLightingTime, due.candleLightingTime)
        assertEquals(zmanim.getObservanceEnd(friday, newYork), due.end)
        assertEquals(listOf(friday.plusWeeks(1)), result.of<ReminderAlarm.CandleLighting>().map { it.date })
    }

    @Test
    fun `future candle lighting is never due early`() {
        val thursdayNight = plan(friday.minusDays(1), LocalTime.of(23, 30), candlePrefs)
        assertNull(thursdayNight.dueCandleLighting)
        assertEquals(friday, thursdayNight.of<ReminderAlarm.CandleLighting>().first().date)
    }

    @Test
    fun `hours before mode drops a passed trigger today but schedules next week`() {
        val prefs = candlePrefs.copy(candleLightingNotifyMode = "hours_before", candleLightingHoursBefore = 2)
        val result = plan(friday, LocalTime.of(17, 0), prefs)

        assertNull(result.dueCandleLighting)
        val next = result.of<ReminderAlarm.CandleLighting>().single()
        val nextFriday = friday.plusWeeks(1)
        assertEquals(nextFriday, next.date)
        val candle = zmanim.getShabbatInfo(nextFriday, newYork).candleLightingTime!!
        assertEquals(ZonedDateTime.of(nextFriday, candle, nyZone).minusHours(2).toInstant(), next.triggerAt)
    }

    @Test
    fun `holy day transitions keep the repository's candle lighting and havdalah`() {
        // Window over Shavuot 5786 in the Diaspora: erev Shavuot, then lighting on the
        // second night after nightfall, with Shabbat following
        val prefs = candlePrefs.copy(candleLightingNotifyMode = "hours_before")
        val start = hebrew(JewishCalendar.SIVAN, 3)
        val result = plan(start, LocalTime.of(0, 1), prefs)
        val candles = result.of<ReminderAlarm.CandleLighting>()

        val expectedDates = (0..REMINDER_LOOKAHEAD_DAYS.toLong()).map { start.plusDays(it) }
            .filter { zmanim.hasCandles(it) && zmanim.getShabbatInfo(it, newYork).candleLightingTime != null }
        assertEquals(expectedDates, candles.map { it.date })
        assertTrue(candles.size >= 2)
        candles.forEach {
            assertEquals(zmanim.getShabbatInfo(it.date, newYork).candleLightingTime, it.candleLightingTime)
            assertEquals(zmanim.getObservanceEnd(it.date, newYork), it.end)
        }
    }

    // --- Omer ----------------------------------------------------------------------------

    @Test
    fun `run after nightfall skips tonight but schedules the following evenings and mornings`() {
        val today = hebrew(JewishCalendar.NISSAN, 16) // morning count 1, tonight counts 2
        val result = plan(today, LocalTime.of(23, 0), omerPrefs)

        val evenings = result.of<ReminderAlarm.OmerEvening>()
        assertEquals((1..7L).map { today.plusDays(it) }, evenings.map { it.date })
        assertEquals((3..9).toList(), evenings.map { it.omerDay })
        evenings.forEach {
            val tzait = zmanim.getTzaitTime(it.date, newYork)!!
            assertEquals(ZonedDateTime.of(it.date, tzait, nyZone).toInstant(), it.triggerAt)
        }

        val mornings = result.of<ReminderAlarm.OmerMorning>()
        assertEquals((1..7L).map { today.plusDays(it) }, mornings.map { it.date })
        assertEquals((2..8).toList(), mornings.map { it.omerDay })
    }

    @Test
    fun `run after the morning time keeps tonight's reminder`() {
        val today = hebrew(JewishCalendar.NISSAN, 16)
        val result = plan(today, LocalTime.of(12, 0), omerPrefs)

        val tonight = result.of<ReminderAlarm.OmerEvening>().first()
        assertEquals(today, tonight.date)
        assertEquals(2, tonight.omerDay)
        assertEquals(today.plusDays(1), result.of<ReminderAlarm.OmerMorning>().first().date)
    }

    @Test
    fun `first night is planned ahead from erev Pesach`() {
        val erevPesach = hebrew(JewishCalendar.NISSAN, 14)
        val result = plan(erevPesach, LocalTime.of(23, 0), omerPrefs)

        val first = result.of<ReminderAlarm.OmerEvening>().first()
        assertEquals(hebrew(JewishCalendar.NISSAN, 15), first.date)
        assertEquals(1, first.omerDay)
        assertEquals(hebrew(JewishCalendar.NISSAN, 16), result.of<ReminderAlarm.OmerMorning>().first().date)
    }

    @Test
    fun `window over the end of the Omer stops after day 49`() {
        val today = hebrew(JewishCalendar.SIVAN, 1)
        val result = plan(today, LocalTime.of(0, 1), omerPrefs)

        val evenings = result.of<ReminderAlarm.OmerEvening>()
        assertEquals(hebrew(JewishCalendar.SIVAN, 4), evenings.last().date)
        assertEquals(49, evenings.last().omerDay)
        val mornings = result.of<ReminderAlarm.OmerMorning>()
        assertEquals(hebrew(JewishCalendar.SIVAN, 5), mornings.last().date)
        assertEquals(49, mornings.last().omerDay)
    }

    @Test
    fun `omer morning uses the device zone and tzait the device position`() {
        val jerusalemZone = ZoneId.of(Location.JERUSALEM.timezoneId)
        val today = hebrew(JewishCalendar.IYAR, 1)
        val result = plan(today, LocalTime.of(0, 1), omerPrefs, Location.JERUSALEM, deviceZone = nyZone)

        result.of<ReminderAlarm.OmerMorning>().forEach {
            assertEquals(ZonedDateTime.of(it.date, LocalTime.of(7, 0), nyZone).toInstant(), it.triggerAt)
        }
        result.of<ReminderAlarm.OmerEvening>().forEach {
            assertEquals(
                zmanim.getTzaitTime(it.date, Location.JERUSALEM),
                it.triggerAt.atZone(jerusalemZone).toLocalTime()
            )
            assertEquals(it.date, it.triggerAt.atZone(jerusalemZone).toLocalDate())
        }
    }

    // --- Time zones, DST and rollover ---------------------------------------------------

    @Test
    fun `morning alarms take each date's offset across the fall-back change`() {
        val lastEdtFriday = LocalDate.of(2026, 10, 30) // New York falls back on 1 Nov 2026
        val result = plan(lastEdtFriday, LocalTime.of(6, 0), candlePrefs)
        val candles = result.of<ReminderAlarm.CandleLighting>()

        assertEquals(listOf(lastEdtFriday, lastEdtFriday.plusWeeks(1)), candles.map { it.date })
        assertEquals(ZoneOffset.ofHours(-4), candles[0].triggerAt.atZone(nyZone).offset)
        assertEquals(ZoneOffset.ofHours(-5), candles[1].triggerAt.atZone(nyZone).offset)
        candles.forEach { assertEquals(LocalTime.of(8, 0), it.triggerAt.atZone(nyZone).toLocalTime()) }
    }

    @Test
    fun `a morning time inside the DST gap moves forward`() {
        // Israel springs forward at 02:00 on Friday 27 March 2026
        val jerusalemZone = ZoneId.of(Location.JERUSALEM.timezoneId)
        val friday = LocalDate.of(2026, 3, 27)
        val prefs = candlePrefs.copy(candleLightingMorningTime = 2 * 60 + 30)
        val result = plan(friday, LocalTime.of(0, 0), prefs, Location.JERUSALEM, jerusalemZone)

        val today = result.of<ReminderAlarm.CandleLighting>().first()
        assertEquals(friday, today.date)
        assertEquals(LocalTime.of(3, 30), today.triggerAt.atZone(jerusalemZone).toLocalTime())
        assertEquals(ZoneOffset.ofHours(3), today.triggerAt.atZone(jerusalemZone).offset)
    }

    @Test
    fun `window across the new year keeps the correct dates`() {
        val today = LocalDate.of(2026, 12, 28)
        val result = plan(today, LocalTime.of(12, 0), candlePrefs)
        val candles = result.of<ReminderAlarm.CandleLighting>()

        assertEquals(listOf(LocalDate.of(2027, 1, 1)), candles.map { it.date })
        assertEquals(
            ZonedDateTime.of(2027, 1, 1, 8, 0, 0, 0, nyZone).toInstant(),
            candles.single().triggerAt
        )
        assertEquals((0..7L).map { today.plusDays(it) }, result.window)
        assertEquals(result.window.size, result.window.map { it.dayOfYear }.toSet().size)
    }

    @Test
    fun `disabled reminders plan nothing`() {
        val result = plan(
            hebrew(JewishCalendar.NISSAN, 16),
            LocalTime.of(0, 1),
            UserPreferences(notifyCandleLighting = false, notifyOmer = false)
        )
        assertTrue(result.alarms.isEmpty())
        assertNull(result.dueCandleLighting)
    }
}
