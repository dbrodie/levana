package ninja.doskey.app.levana.notifications

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Independent verifier checks of planReminderAlarms (verify-notification-lookahead). */
class VerifyLookaheadPlanTest {

    private val zmanim = ZmanimRepository()
    private val calendar = CalendarRepository()

    private val ny = Location(40.7128, -74.006, timezoneId = "America/New_York", name = "NY")
    private val jlm = Location(31.7683, 35.2137, timezoneId = "Asia/Jerusalem", name = "JLM")
    private val nyZone = ZoneId.of("America/New_York")
    private val jlmZone = ZoneId.of("Asia/Jerusalem")
    private val utc = ZoneId.of("UTC")

    private val morningCandles = UserPreferences(
        notifyCandleLighting = true,
        candleLightingNotifyMode = "morning",
        candleLightingMorningTime = 9 * 60 + 30
    )
    private fun hoursBefore(h: Int) = UserPreferences(
        notifyCandleLighting = true,
        candleLightingNotifyMode = "hours_before",
        candleLightingHoursBefore = h
    )
    private val omer = UserPreferences(
        notifyCandleLighting = false,
        notifyOmer = true,
        notifyOmerTzait = true,
        notifyOmerMorning = true,
        notifyOmerMorningTime = 6 * 60 + 45
    )

    private fun run(
        today: LocalDate,
        at: LocalTime,
        prefs: UserPreferences,
        location: Location = ny,
        device: ZoneId = nyZone
    ) = planReminderAlarms(
        today, ZonedDateTime.of(today, at, device).toInstant(), device, location, prefs, zmanim, calendar
    )

    private fun runAt(
        today: LocalDate, now: Instant, prefs: UserPreferences, location: Location, device: ZoneId
    ) = planReminderAlarms(today, now, device, location, prefs, zmanim, calendar)

    private fun heb(year: Int, month: Int, day: Int) = JewishCalendar(year, month, day).localDate

    private fun ReminderAlarmPlan.candles() = alarms.filterIsInstance<ReminderAlarm.CandleLighting>()
    private fun ReminderAlarmPlan.evenings() = alarms.filterIsInstance<ReminderAlarm.OmerEvening>()
    private fun ReminderAlarmPlan.mornings() = alarms.filterIsInstance<ReminderAlarm.OmerMorning>()

    // Friday 2026-11-13, Thursday 2026-11-12 (no holidays nearby)
    private val fri = LocalDate.of(2026, 11, 13)

    @Test
    fun `window is exactly today plus seven days`() {
        val p = run(fri, LocalTime.NOON, morningCandles)
        assertEquals((0..7L).map { fri.plusDays(it) }, p.window)
    }

    @Test
    fun `late Thursday run keeps Friday and next Friday mornings scheduled`() {
        // Window from Wednesday covers Friday; window from Friday covers next Friday
        val p = run(fri.minusDays(2), LocalTime.of(23, 30), morningCandles)
        assertEquals(listOf(fri), p.candles().map { it.date })
        assertEquals(listOf(fri, fri.plusWeeks(1)), run(fri, LocalTime.of(0, 5), morningCandles).candles().map { it.date })
        p.candles().forEach {
            assertEquals(ZonedDateTime.of(it.date, LocalTime.of(9, 30), nyZone).toInstant(), it.triggerAt)
            assertEquals(zmanim.getShabbatInfo(it.date, ny).candleLightingTime, it.candleLightingTime)
            assertEquals(zmanim.getObservanceEnd(it.date, ny), it.end)
        }
        assertNull("Friday's notice must not be posted early on Thursday", p.dueCandleLighting)
    }

    @Test
    fun `late run on Friday after the morning time posts today and schedules next Friday`() {
        val p = run(fri, LocalTime.of(21, 0), morningCandles)
        assertEquals(fri, p.dueCandleLighting?.date)
        assertEquals(listOf(fri.plusWeeks(1)), p.candles().map { it.date })
    }

    @Test
    fun `run exactly at the trigger instant treats it as passed`() {
        val trigger = ZonedDateTime.of(fri, LocalTime.of(9, 30), nyZone).toInstant()
        val p = runAt(fri, trigger, morningCandles, ny, nyZone)
        assertEquals(fri, p.dueCandleLighting?.date)
        assertFalse(p.candles().any { it.date == fri })
        val p2 = runAt(fri, trigger.minusMillis(1), morningCandles, ny, nyZone)
        assertNull(p2.dueCandleLighting)
        assertTrue(p2.candles().any { it.date == fri })
    }

    @Test
    fun `skipped runs - Saturday run already covers next Friday`() {
        val p = run(fri.plusDays(1), LocalTime.of(3, 0), morningCandles)
        assertEquals(listOf(fri.plusWeeks(1)), p.candles().map { it.date })
        assertNull(p.dueCandleLighting)
    }

    @Test
    fun `hours before mode never yields a due notice`() {
        val p = run(fri, LocalTime.of(23, 0), hoursBefore(2))
        assertNull(p.dueCandleLighting)
        assertEquals(listOf(fri.plusWeeks(1)), p.candles().map { it.date })
    }

    // --- Omer -----------------------------------------------------------------------------

    private val firstNightCivil = heb(5787, 1, 15) // evening -> day 1
    private val lastNightCivil = heb(5787, 3, 4) // evening -> day 49

    @Test
    fun `first Omer night appears exactly when it enters the window`() {
        val out = run(firstNightCivil.minusDays(8), LocalTime.of(5, 0), omer)
        assertTrue(out.evenings().isEmpty() && out.mornings().isEmpty())

        val inWin = run(firstNightCivil.minusDays(7), LocalTime.of(5, 0), omer)
        assertEquals(listOf(firstNightCivil to 1), inWin.evenings().map { it.date to it.omerDay })
        assertTrue(inWin.mornings().isEmpty())

        val tzait = zmanim.getTzaitTime(firstNightCivil, ny)!!
        assertEquals(ZonedDateTime.of(firstNightCivil, tzait, nyZone).toInstant(), inWin.evenings()[0].triggerAt)
    }

    @Test
    fun `counts advance one per civil date and morning count lags evening by one`() {
        val today = firstNightCivil.minusDays(1)
        val p = run(today, LocalTime.of(5, 0), omer)
        assertEquals((0..6).map { firstNightCivil.plusDays(it.toLong()) to it + 1 },
            p.evenings().map { it.date to it.omerDay })
        assertEquals((1..6).map { firstNightCivil.plusDays(it.toLong()) to it },
            p.mornings().map { it.date to it.omerDay })
        p.mornings().forEach {
            assertEquals(ZonedDateTime.of(it.date, LocalTime.of(6, 45), nyZone).toInstant(), it.triggerAt)
        }
    }

    @Test
    fun `last Omer night and morning, nothing after`() {
        val p = run(lastNightCivil.minusDays(2), LocalTime.of(5, 0), omer)
        assertEquals(lastNightCivil to 49, p.evenings().last().let { it.date to it.omerDay })
        assertEquals(lastNightCivil.plusDays(1) to 49, p.mornings().last().let { it.date to it.omerDay })
        assertTrue(p.evenings().none { it.date.isAfter(lastNightCivil) })
        assertTrue(p.mornings().none { it.date.isAfter(lastNightCivil.plusDays(1)) })
    }

    @Test
    fun `late run after nightfall keeps all future evenings and mornings`() {
        val today = firstNightCivil.plusDays(10)
        val p = run(today, LocalTime.of(23, 50), omer)
        assertEquals((1..7L).map { today.plusDays(it) }, p.evenings().map { it.date })
        assertEquals((1..7L).map { today.plusDays(it) }, p.mornings().map { it.date })
        assertTrue(p.evenings().all { it.triggerAt.isAfter(ZonedDateTime.of(today, LocalTime.of(23, 50), nyZone).toInstant()) })
    }

    @Test
    fun `Omer evenings continue on Shabbat and Yom Tov eves (count unchanged by holy days)`() {
        // Window from 1 day before the first night covers the diaspora 7th/8th day of Pesach
        val p = run(firstNightCivil.plusDays(3), LocalTime.of(5, 0), omer)
        val fridays = p.evenings().filter { it.date.dayOfWeek == DayOfWeek.FRIDAY }
        assertTrue(fridays.isNotEmpty())
        p.evenings().forEach {
            assertEquals(it.date.toEpochDay() - firstNightCivil.toEpochDay() + 1, it.omerDay.toLong())
        }
        println("Pesach 5787 15 Nisan civil=$firstNightCivil (${firstNightCivil.dayOfWeek}); evenings=${p.evenings().map { it.date to it.omerDay }}")
    }

    // --- Holy-day transitions for candle lighting --------------------------------------------

    @Test
    fun `Pesach window - diaspora lights second night from tzait, Israel does not`() {
        val erev = firstNightCivil.minusDays(1)
        val dia = run(erev.minusDays(1), LocalTime.of(5, 0), morningCandles)
        val isr = run(erev.minusDays(1), LocalTime.of(5, 0), morningCandles.copy(isInIsrael = true), location = jlm, device = jlmZone)
        println("diaspora candles=${dia.candles().map { Triple(it.date, it.candleLightingTime, it.end) }}")
        println("israel candles=${isr.candles().map { Triple(it.date, it.candleLightingTime, it.end) }}")
        assertTrue(dia.candles().any { it.date == erev })
        assertTrue(dia.candles().any { it.date == firstNightCivil })
        assertFalse(isr.candles().any { it.date == firstNightCivil && firstNightCivil.dayOfWeek != DayOfWeek.FRIDAY })
        // Second-night lighting (Yom Tov -> Yom Tov, not Friday) waits for tzait
        val second = dia.candles().first { it.date == firstNightCivil }
        if (firstNightCivil.dayOfWeek != DayOfWeek.FRIDAY) {
            assertEquals(zmanim.getTzaitTime(firstNightCivil, ny), second.candleLightingTime)
        }
        // Every candle date has its own end matching the repository (multi-day observance)
        dia.candles().forEach { assertEquals(zmanim.getObservanceEnd(it.date, ny, false), it.end) }
        assertNotEquals(dia.candles().size, isr.candles().size)
    }

    // --- Zones and DST ---------------------------------------------------------------------

    // TASK-5: these used the selected city's zone for candle lighting; now the device zone
    // sets every wall-clock time and the device position only the astronomical instants
    @Test
    fun `zone mismatch - wall-clock times in the device zone, nightfall at the device position`() {
        val today = firstNightCivil.plusDays(2)
        val p = run(today, LocalTime.of(1, 0), omer.copy(notifyCandleLighting = true, candleLightingNotifyMode = "morning", candleLightingMorningTime = 9 * 60 + 30), location = jlm, device = utc)
        p.mornings().forEach { assertEquals(ZonedDateTime.of(it.date, LocalTime.of(6, 45), utc).toInstant(), it.triggerAt) }
        p.evenings().forEach {
            assertEquals(ZonedDateTime.of(it.date, zmanim.getTzaitTime(it.date, jlm)!!, jlmZone).toInstant(), it.triggerAt)
        }
        p.candles().forEach { assertEquals(ZonedDateTime.of(it.date, LocalTime.of(9, 30), utc).toInstant(), it.triggerAt) }
        assertTrue(p.candles().isNotEmpty())
    }

    /**
     * Device clock in Los Angeles at a Jerusalem position. Late on Thursday the Friday 09:30
     * morning notice is still ahead on the device clock, so this run schedules it (it was
     * previously in Jerusalem time, already passed, and deferred to Friday's run).
     */
    @Test
    fun `zone mismatch late run - Friday's morning notice is on the device clock`() {
        val la = ZoneId.of("America/Los_Angeles")
        val thu = fri.minusDays(1)
        val p = run(thu, LocalTime.of(23, 30), morningCandles, location = jlm, device = la)
        assertEquals(
            ZonedDateTime.of(fri, LocalTime.of(9, 30), la).toInstant(),
            p.candles().single { it.date == fri }.triggerAt
        )
        assertNull(p.dueCandleLighting)
    }

    @Test
    fun `NY fall-back inside the window gives each Friday its own offset`() {
        val p = run(LocalDate.of(2026, 10, 28), LocalTime.of(5, 0), morningCandles)
        val byDate = p.candles().associate { it.date to it.triggerAt }
        assertEquals(Instant.parse("2026-10-30T13:30:00Z"), byDate[LocalDate.of(2026, 10, 30)])
        val p2 = run(LocalDate.of(2026, 10, 31), LocalTime.of(5, 0), morningCandles)
        assertEquals(Instant.parse("2026-11-06T14:30:00Z"), p2.candles().single { it.date == LocalDate.of(2026, 11, 6) }.triggerAt)
    }

    @Test
    fun `hours before is real elapsed hours across Israel's Friday spring-forward`() {
        val israelFri = LocalDate.of(2027, 3, 26) // DST starts 02:00 this Friday
        assertTrue(jlmZone.rules.isDaylightSavings(ZonedDateTime.of(israelFri, LocalTime.NOON, jlmZone).toInstant()))
        assertFalse(jlmZone.rules.isDaylightSavings(ZonedDateTime.of(israelFri, LocalTime.of(1, 0), jlmZone).toInstant()))
        val isr = { h: Int -> hoursBefore(h).copy(isInIsrael = true) }
        for (h in listOf(1, 6, 12, 18, 20)) {
            val p = run(israelFri.minusDays(2), LocalTime.of(5, 0), isr(h), location = jlm, device = jlmZone)
            val c = p.candles().single { it.date == israelFri }
            val candleInstant = ZonedDateTime.of(israelFri, c.candleLightingTime, jlmZone).toInstant()
            assertEquals("h=$h", Duration.ofHours(h.toLong()), Duration.between(c.triggerAt, candleInstant))
            if (h >= 18) {
                val wall = c.triggerAt.atZone(jlmZone)
                assertEquals("h=$h crosses civil midnight", israelFri.minusDays(1), wall.toLocalDate())
                // Wall-clock subtraction would be one hour later (gap skipped)
                assertEquals(c.candleLightingTime.minusHours(h.toLong()).minusHours(1), wall.toLocalTime().withSecond(c.candleLightingTime.second).withNano(c.candleLightingTime.nano))
            }
        }
    }

    @Test
    fun `hours before crossing civil midnight is still planned when run the previous day`() {
        val thu = fri.minusDays(1)
        val p = run(thu, LocalTime.of(12, 0), hoursBefore(20))
        val c = p.candles().first { it.date == fri }
        assertEquals(thu, c.triggerAt.atZone(nyZone).toLocalDate())
        // After it passes (Thursday evening) it is dropped, not posted
        val p2 = runAt(thu, c.triggerAt.plusSeconds(1), hoursBefore(20), ny, nyZone)
        assertFalse(p2.candles().any { it.date == fri })
        assertNull(p2.dueCandleLighting)
    }
}
