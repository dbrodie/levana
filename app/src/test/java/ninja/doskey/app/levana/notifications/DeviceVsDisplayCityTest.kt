package ninja.doskey.app.levana.notifications

import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.device.fixedClock
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * TASK-5 case table, rows 1-5, 11 and 12: the device (New York) decides every reminder; the
 * selected city (Jerusalem) has no effect on any trigger or notice text.
 */
class DeviceVsDisplayCityTest {

    private val zmanim = ZmanimRepository()
    private val calendar = CalendarRepository()
    private val nyZone = ZoneId.of("America/New_York")
    private val jerusalemZone = ZoneId.of(Location.JERUSALEM.timezoneId)
    private val newYork = Location(40.7128, -74.0060, 0.0, nyZone.id, "New York")

    // A plain Friday with candles, and a weekday in the Omer
    private val friday: LocalDate = generateSequence(LocalDate.of(2027, 1, 1)) { it.plusDays(1) }
        .first { it.dayOfWeek == DayOfWeek.FRIDAY && zmanim.hasCandles(it) && !zmanim.hasCandles(it.minusDays(1)) }
    private val omerDay: LocalDate = generateSequence(LocalDate.of(2027, 4, 1)) { it.plusDays(1) }
        .first { it.dayOfWeek == DayOfWeek.TUESDAY && calendar.getDayInfo(it, inIsrael = false).omerDay != null }

    private val base = UserPreferences(
        currentLocation = newYork,
        isInIsrael = false,
        notifyCandleLighting = true,
        candleLightingNotifyMode = "hours_before",
        candleLightingHoursBefore = 2,
        candleLightingMorningTime = 9 * 60,
        notifyOmer = true,
        notifyOmerTzait = true,
        notifyOmerMorning = true,
        notifyOmerMorningTime = 7 * 60
    )
    /** The same preferences with Jerusalem selected in the sidebar. */
    private val jerusalemSelected = base.copy(
        savedLocations = listOf(SavedLocation("j", Location.JERUSALEM)),
        activeLocationId = "j"
    )

    private fun plan(
        prefs: UserPreferences,
        date: LocalDate,
        time: LocalTime = LocalTime.of(0, 30),
        zone: ZoneId = nyZone
    ): ReminderAlarmPlan {
        val device = DeviceContext(MutableStateFlow(prefs)) { fixedClock(date, time, zone) }.now(prefs)
        return planReminderAlarms(device, prefs, zmanim, calendar).onSystemClock(device)
    }

    private fun ReminderAlarmPlan.candle(date: LocalDate) =
        alarms.filterIsInstance<ReminderAlarm.CandleLighting>().single { it.date == date }
    private fun ReminderAlarmPlan.evening(date: LocalDate) =
        alarms.filterIsInstance<ReminderAlarm.OmerEvening>().single { it.date == date }
    private fun ReminderAlarmPlan.morning(date: LocalDate) =
        alarms.filterIsInstance<ReminderAlarm.OmerMorning>().single { it.date == date }

    @Test
    fun `the selected city changes no reminder at all`() {
        for (date in listOf(friday, omerDay)) {
            assertEquals(plan(base, date), plan(jerusalemSelected, date))
            assertEquals(
                plan(base.copy(candleLightingNotifyMode = "morning"), date),
                plan(jerusalemSelected.copy(candleLightingNotifyMode = "morning"), date)
            )
        }
    }

    @Test
    fun `row 1 - hours before is the device position's candle lighting, text in device time`() {
        val candle = plan(jerusalemSelected, friday).candle(friday)
        val nyCandles = zmanim.getCandleLightingInstant(friday, newYork, 18.0, false)!!
        assertEquals(nyCandles.minus(Duration.ofHours(2)), candle.triggerAt)
        assertEquals(nyCandles.atZone(nyZone).toLocalTime(), candle.candleLightingTime)
        assertNotEquals(zmanim.getShabbatInfo(friday, Location.JERUSALEM, 18.0).candleLightingTime, candle.candleLightingTime)
        assertEquals(zmanim.getObservanceEnd(friday, newYork, false), candle.end)
    }

    @Test
    fun `row 2 - morning mode is the morning in the device zone`() {
        val candle = plan(jerusalemSelected.copy(candleLightingNotifyMode = "morning"), friday).candle(friday)
        // Previously 09:00 Jerusalem, i.e. 02:00 or 03:00 in New York
        assertEquals(ZonedDateTime.of(friday, LocalTime.of(9, 0), nyZone).toInstant(), candle.triggerAt)
    }

    @Test
    fun `rows 3 and 4 - Omer at the device position's nightfall, morning in the device zone`() {
        val plan = plan(jerusalemSelected, omerDay)
        assertEquals(zmanim.getTzaitInstant(omerDay, newYork), plan.evening(omerDay).triggerAt)
        assertEquals(
            ZonedDateTime.of(omerDay, LocalTime.of(7, 0), nyZone).toInstant(),
            plan.morning(omerDay).triggerAt
        )
    }

    @Test
    fun `row 5 - the window is the device date only, at the midnight boundary`() {
        // 23:59 Thursday in New York is Friday morning in Jerusalem; the window is Thursday's
        val thursday = friday.minusDays(1)
        val late = plan(jerusalemSelected, thursday, LocalTime.of(23, 59))
        assertEquals((0..REMINDER_LOOKAHEAD_DAYS.toLong()).map { thursday.plusDays(it) }, late.window)
        assertEquals(thursday.minusDays(1), late.previousEvening)
        // One minute later the device date, and so the window, moves on
        val next = plan(jerusalemSelected, friday, LocalTime.of(0, 0))
        assertEquals(friday, next.window.first())
        assertEquals(REMINDER_LOOKAHEAD_DAYS + 1, next.window.size)
    }

    @Test
    fun `row 11 - nightfall after midnight is still planned for the device position`() {
        // Omer evening planned just after midnight: the previous evening is covered
        val dayAfter = omerDay.plusDays(1)
        val plan = plan(jerusalemSelected, dayAfter, LocalTime.of(0, 1))
        assertEquals(omerDay, plan.previousEvening)
        val all = plan.alarms + plan.overdue
        assertTrue(all.filterIsInstance<ReminderAlarm.OmerEvening>().any { it.date == omerDay })
    }

    @Test
    fun `DST day - morning notices keep their device wall time`() {
        // US clocks spring forward on Sun 14 Mar 2027, between these two Fridays
        val friday = LocalDate.of(2027, 3, 12)
        val nextFriday = friday.plusDays(7)
        val morning = plan(base.copy(candleLightingNotifyMode = "morning"), friday)
        for (day in listOf(friday, nextFriday)) {
            assertEquals(
                ZonedDateTime.of(day, LocalTime.of(9, 0), nyZone).toInstant(),
                morning.candle(day).triggerAt
            )
        }
        assertEquals(java.time.ZoneOffset.ofHours(-5), morning.candle(friday).triggerAt.atZone(nyZone).offset)
        assertEquals(java.time.ZoneOffset.ofHours(-4), morning.candle(nextFriday).triggerAt.atZone(nyZone).offset)
    }

    @Test
    fun `row 12 - with the override on, alarms are planned as if on the shifted date`() {
        // The real clock reads Tuesday; the override makes it the Friday three days later
        val tuesday = friday.minusDays(3)
        val shifted = base.copy(devDayShift = 3, candleLightingNotifyMode = "morning")
        val plan = plan(shifted, tuesday)
        assertEquals(friday, plan.window.first())
        // Friday's 09:00 notice fires when the as-if clock reads 09:00, i.e. real Tuesday 09:00
        val candle = plan.candle(friday)
        assertEquals(ZonedDateTime.of(tuesday, LocalTime.of(9, 0), nyZone).toInstant(), candle.triggerAt)

        // Off again: Tuesday's own plan on the real clock
        val off = plan(shifted.copy(devDayShift = 0), tuesday)
        assertEquals(tuesday, off.window.first())
        assertEquals(
            ZonedDateTime.of(friday, LocalTime.of(9, 0), nyZone).toInstant(),
            off.candle(friday).triggerAt
        )
    }

    @Test
    fun `a device in Jerusalem plans in Jerusalem whatever city is selected`() {
        val inJerusalem = base.copy(
            currentLocation = Location.JERUSALEM,
            savedLocations = listOf(SavedLocation("ny", newYork)),
            activeLocationId = "ny",
            candleLightingNotifyMode = "morning"
        )
        val candle = plan(inJerusalem, friday, zone = jerusalemZone).candle(friday)
        assertEquals(ZonedDateTime.of(friday, LocalTime.of(9, 0), jerusalemZone).toInstant(), candle.triggerAt)
    }
}
