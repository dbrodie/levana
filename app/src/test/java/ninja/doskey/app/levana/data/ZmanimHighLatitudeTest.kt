package ninja.doskey.app.levana.data

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import ninja.doskey.app.levana.domain.agenda.AgendaAssembler
import ninja.doskey.app.levana.domain.agenda.AgendaItem
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SystemCalendarEvent
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.ZmanName
import ninja.doskey.app.levana.domain.model.ZmanTime
import ninja.doskey.app.levana.notifications.ReminderAlarm
import ninja.doskey.app.levana.notifications.planReminderAlarms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.abs

/**
 * Times that can't be computed at high latitudes (TASK-57.31): white nights (no 8.5°
 * nightfall), polar night and midnight sun (no sunrise or sunset). Nothing is substituted;
 * the time is missing, the candle-lighting and havdalah lines stay, and the selected city
 * carries a note.
 */
class ZmanimHighLatitudeTest {

    private val repo = ZmanimRepository()
    private val calendar = CalendarRepository()

    private val stPetersburg = Location(59.9343, 30.3351, timezoneId = "Europe/Moscow", name = "St. Petersburg")
    private val helsinki = Location(60.1699, 24.9384, timezoneId = "Europe/Helsinki", name = "Helsinki")
    private val longyearbyen = Location(78.2232, 15.6267, timezoneId = "Arctic/Longyearbyen", name = "Longyearbyen")
    private val london = Location(51.5074, -0.1278, timezoneId = "Europe/London", name = "London")
    private val paris = Location(48.8566, 2.3522, timezoneId = "Europe/Paris", name = "Paris")
    private val newYork = Location(40.7128, -74.006, timezoneId = "America/New_York", name = "New York")

    private fun List<ZmanTime>.time(name: ZmanName) = single { it.name == name.english }.time

    private fun List<ZmanTime>.has(name: ZmanName) =
        any { it.name == name.english }

    // --- White night: St. Petersburg ---------------------------------------------------

    private val stPeteFriday = LocalDate.of(2027, 6, 25)
    private val stPeteShabbat = stPeteFriday.plusDays(1)

    @Test
    fun `white night Friday keeps candle lighting before sunset`() {
        val info = repo.getShabbatInfo(stPeteFriday, stPetersburg)
        val candles = requireNotNull(info.candleLightingTime)
        assertTrue(info.showCandleLighting)
        assertEquals(22, candles.hour)
        assertEquals(repo.getSunsetTime(stPeteFriday, stPetersburg)!!.minusMinutes(18).withSecond(0).withNano(0),
            candles.withSecond(0).withNano(0))
    }

    @Test
    fun `white night havdalah is missing but its line stays`() {
        val info = repo.getShabbatInfo(stPeteShabbat, stPetersburg)
        assertNull(info.havdalahTime)
        assertTrue(info.showHavdalah)

        val zmanim = repo.getZmanim(stPeteShabbat, stPetersburg)
        assertTrue(zmanim.has(ZmanimRepository.HAVDALAH))
        assertNull(zmanim.time(ZmanimRepository.HAVDALAH))
        assertNull(zmanim.time(ZmanimRepository.NIGHTFALL))
        assertNull(zmanim.time(ZmanimRepository.ALOT_HASHACHAR))
        assertNotNull(zmanim.time(ZmanimRepository.SUNRISE))
        assertNotNull(zmanim.time(ZmanimRepository.SUNSET))

        val end = repo.getObservanceEnd(stPeteFriday, stPetersburg)!!
        assertNull(end.time)
        assertEquals(stPeteShabbat, end.date)
    }

    @Test
    fun `white night agenda keeps the havdalah line on the Shabbat card`() {
        val friday = AgendaAssembler.day(calendar.getDayInfo(stPeteFriday), repo.getShabbatInfo(stPeteFriday, stPetersburg))!!
        val candles = friday.items.filterIsInstance<AgendaItem.CandleLighting>().single()
        assertNotNull(candles.time)

        val shabbat = AgendaAssembler.day(calendar.getDayInfo(stPeteShabbat), repo.getShabbatInfo(stPeteShabbat, stPetersburg))!!
        val card = shabbat.items.filterIsInstance<AgendaItem.Shabbat>().single()
        assertTrue(card.hasHavdalah)
        assertNull(card.havdalah)
    }

    // --- Polar night: Longyearbyen, November 2026 --------------------------------------

    private val polarFriday = LocalDate.of(2026, 11, 20)
    private val polarShabbat = polarFriday.plusDays(1)

    @Test
    fun `polar night has neither candle lighting nor a midday havdalah`() {
        // The sun still crosses 8.5° below the horizon around midday; that isn't havdalah
        assertNotNull(repo.getTzaitTime(polarShabbat, longyearbyen))
        assertNull(repo.getSunsetTime(polarFriday, longyearbyen))

        val friday = repo.getShabbatInfo(polarFriday, longyearbyen)
        assertNull(friday.candleLightingTime)
        assertTrue(friday.showCandleLighting)

        val shabbat = repo.getShabbatInfo(polarShabbat, longyearbyen)
        assertNull(shabbat.havdalahTime)
        assertTrue(shabbat.showHavdalah)

        assertNull(repo.getCandleLightingInstant(polarFriday, longyearbyen))
        assertNull(repo.getObservanceEnd(polarFriday, longyearbyen)!!.time)
    }

    @Test
    fun `polar night agenda keeps both lines without times`() {
        val friday = AgendaAssembler.day(calendar.getDayInfo(polarFriday), repo.getShabbatInfo(polarFriday, longyearbyen))!!
        assertNull(friday.items.filterIsInstance<AgendaItem.CandleLighting>().single().time)
        val card = AgendaAssembler.day(calendar.getDayInfo(polarShabbat), repo.getShabbatInfo(polarShabbat, longyearbyen))!!
            .items.filterIsInstance<AgendaItem.Shabbat>().single()
        assertTrue(card.hasHavdalah)
        assertNull(card.havdalah)

        val zmanim = repo.getZmanim(polarFriday, longyearbyen)
        assertTrue(zmanim.has(ZmanimRepository.CANDLE_LIGHTING))
        assertNull(zmanim.time(ZmanimRepository.CANDLE_LIGHTING))
    }

    @Test
    fun `missing candle lighting sorts after the day's timed entries`() {
        val ordered = AgendaAssembler.order(listOf(
            AgendaItem.CandleLighting(null),
            AgendaItem.DeviceEventItem(
                SystemCalendarEvent(
                    1, "Dinner", polarFriday.atTime(19, 0), polarFriday.atTime(20, 0), false, 0, 0
                ),
                LocalTime.of(19, 0)
            ),
            AgendaItem.HolidayItem(calendar.getDayInfo(LocalDate.of(2026, 12, 5)).holidays.first()), // Chanukah
        ))
        assertTrue(ordered.first() is AgendaItem.HolidayItem)
        assertTrue(ordered.last() is AgendaItem.CandleLighting)
    }

    // --- Midnight sun: Longyearbyen, June 2027 ------------------------------------------

    @Test
    fun `midnight sun has no sunrise, sunset, candle lighting or havdalah`() {
        val friday = LocalDate.of(2027, 6, 18)
        val shabbat = friday.plusDays(1)
        val zmanim = repo.getZmanim(friday, longyearbyen)
        assertNull(zmanim.time(ZmanimRepository.SUNRISE))
        assertNull(zmanim.time(ZmanimRepository.SUNSET))
        assertNull(zmanim.time(ZmanimRepository.NIGHTFALL))
        assertNull(zmanim.time(ZmanimRepository.PLAG_HAMINCHA))
        assertNotNull(zmanim.time(ZmanimRepository.CHATZOT))
        assertNotNull(zmanim.time(ZmanimRepository.CHATZOT_HALAYLAH))

        val fridayInfo = repo.getShabbatInfo(friday, longyearbyen)
        assertTrue(fridayInfo.showCandleLighting)
        assertNull(fridayInfo.candleLightingTime)
        val shabbatInfo = repo.getShabbatInfo(shabbat, longyearbyen)
        assertTrue(shabbatInfo.showHavdalah)
        assertNull(shabbatInfo.havdalahTime)
    }

    // --- Reminders ----------------------------------------------------------------------

    @Test
    fun `missing candle lighting schedules no reminder and doesn't crash`() {
        val zone = ZoneId.of(longyearbyen.timezoneId)
        listOf("morning", "hours_before").forEach { mode ->
            val prefs = UserPreferences(notifyCandleLighting = true, candleLightingNotifyMode = mode)
            val plan = planReminderAlarms(
                polarFriday.minusDays(1), ZonedDateTime.of(polarFriday.minusDays(1), LocalTime.of(6, 0), zone).toInstant(),
                zone, longyearbyen, prefs, repo, calendar
            )
            assertTrue(mode, plan.alarms.filterIsInstance<ReminderAlarm.CandleLighting>().isEmpty())
            assertNull(mode, plan.dueCandleLighting)
        }
    }

    @Test
    fun `white night Omer schedules the morning reminder but no nightfall reminder`() {
        val date = LocalDate.of(2027, 6, 1) // in the Omer; no 8.5° nightfall in St. Petersburg
        val zone = ZoneId.of(stPetersburg.timezoneId)
        val prefs = UserPreferences(notifyOmer = true, notifyOmerTzait = true, notifyOmerMorning = true)
        val plan = planReminderAlarms(
            date, ZonedDateTime.of(date, LocalTime.of(5, 0), zone).toInstant(), zone, stPetersburg, prefs, repo, calendar
        )
        assertTrue(plan.alarms.none { it is ReminderAlarm.OmerEvening && it.date == date })
        assertTrue(plan.alarms.any { it is ReminderAlarm.OmerMorning && it.date == date })
    }

    // --- The selected city's note --------------------------------------------------------

    @Test
    fun `city note marks cities where sunrise, sunset, candle lighting or nightfall go missing`() {
        listOf(stPetersburg, helsinki, longyearbyen).forEach {
            assertTrue(it.name, ZmanimRepository.hasIncalculableTimes(it.latitude))
            assertTrue(it.name, yearHasMissingTimes(it))
        }
        listOf(london, paris, Location.JERUSALEM, newYork).forEach {
            assertFalse(it.name, ZmanimRepository.hasIncalculableTimes(it.latitude))
            assertFalse(it.name, yearHasMissingTimes(it))
        }
        // London loses only Alot in June, which doesn't earn the note
        assertNull(repo.getZmanim(LocalDate.of(2027, 6, 21), london).time(ZmanimRepository.ALOT_HASHACHAR))
    }

    @Test
    fun `latitude rule matches a full-year calculation`() {
        val band = 0.15 // the exact edge moves by a few hundredths of a degree with the year
        var latitude = -85.0
        while (latitude <= 85.0) {
            if (abs(abs(latitude) - ZmanimRepository.INCALCULABLE_TIMES_LATITUDE) > band) {
                val location = Location(latitude, 0.0, timezoneId = "UTC", name = "$latitude")
                assertEquals("latitude $latitude", ZmanimRepository.hasIncalculableTimes(latitude), scanYear(location))
            }
            latitude += 0.25
        }
    }

    /** Whether [location] misses sunrise, sunset, candle lighting or nightfall on a day of 2027, via the repository. */
    private fun yearHasMissingTimes(location: Location): Boolean =
        generateSequence(LocalDate.of(2027, 1, 1)) { it.plusDays(1) }.takeWhile { it.year == 2027 }.any { date ->
            val zmanim = repo.getZmanim(date, location)
            zmanim.time(ZmanimRepository.SUNRISE) == null || zmanim.time(ZmanimRepository.SUNSET) == null ||
                zmanim.time(ZmanimRepository.NIGHTFALL) == null ||
                (date.dayOfWeek == DayOfWeek.FRIDAY && repo.getShabbatInfo(date, location).candleLightingTime == null)
        }

    /** The same check straight from KosherJava, cheaper for the latitude sweep. */
    private fun scanYear(location: Location): Boolean {
        val tz = TimeZone.getTimeZone(location.timezoneId)
        val czc = ComplexZmanimCalendar(GeoLocation(location.name, location.latitude, location.longitude, 0.0, tz))
        return generateSequence(LocalDate.of(2027, 1, 1)) { it.plusDays(1) }.takeWhile { it.year == 2027 }.any { date ->
            czc.calendar = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth).also { it.timeZone = tz }
            czc.seaLevelSunrise == null || czc.seaLevelSunset == null || czc.tzais == null
        }
    }
}
