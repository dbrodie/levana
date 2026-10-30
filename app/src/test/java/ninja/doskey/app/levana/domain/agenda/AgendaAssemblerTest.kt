package ninja.doskey.app.levana.domain.agenda

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.domain.model.BirthdayOccurrence
import ninja.doskey.app.levana.domain.model.ContactBirthday
import ninja.doskey.app.levana.domain.model.EventOccurrence
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.Holiday
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.domain.model.ShabbatInfo
import ninja.doskey.app.levana.domain.model.SystemCalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgendaAssemblerTest {

    private val calendar = CalendarRepository()
    private val friday = LocalDate.of(2026, 10, 30)
    private val saturday = friday.plusDays(1)

    private fun shabbat(candles: LocalTime? = null, havdalah: LocalTime? = null) =
        ShabbatInfo(candles, havdalah, isErevShabbat = false, isShabbat = false, isErevYomTov = false, isYomTov = false)

    private fun device(title: String, start: LocalDateTime, allDay: Boolean = false, end: LocalDateTime = start.plusHours(1)) =
        SystemCalendarEvent(1, title, start, end, allDay, 7, 0, location = "Rechavia")

    private val yahrzeit = PersonalEvent(id = 1, title = "Moshe ben Avraham", hebrewDay = 19, hebrewMonth = 8, hebrewYear = 5770, useYahrzeitRules = true)
    private val event = PersonalEvent(id = 2, title = "Anniversary", hebrewDay = 19, hebrewMonth = 8, hebrewYear = 5770)
    private val birthday = ContactBirthday("key", "Sarah", null, 19, 8, 0)

    @Test
    fun `all-day entries come first, then timed ones by time`() {
        val day = AgendaAssembler.day(
            dayInfo = calendar.getDayInfo(friday),
            shabbatInfo = shabbat(candles = LocalTime.of(16, 21)),
            events = listOf(EventOccurrence(event), EventOccurrence(yahrzeit)),
            birthdays = listOf(BirthdayOccurrence(birthday)),
            deviceEvents = listOf(
                device("Shabbat dinner", friday.atTime(19, 0)),
                device("Breakfast", friday.atTime(8, 30)),
                device("Trip", friday.atStartOfDay(), allDay = true, end = friday.plusDays(1).atStartOfDay()),
            )
        )!!
        val kinds = day.items.map {
            when (it) {
                is AgendaItem.PersonalEventItem -> if (it.isYahrzeit) "yahrzeit" else "event"
                is AgendaItem.BirthdayItem -> "birthday"
                is AgendaItem.DeviceEventItem -> it.event.title
                is AgendaItem.CandleLighting -> "candles"
                else -> it::class.simpleName
            }
        }
        assertEquals(listOf("yahrzeit", "birthday", "event", "Trip", "Breakfast", "candles", "Shabbat dinner"), kinds)
    }

    @Test
    fun `a Shabbat lists its reading with havdalah`() {
        val day = AgendaAssembler.day(calendar.getDayInfo(saturday), shabbat(havdalah = LocalTime.of(17, 34)))!!
        val shabbat = day.items.single() as AgendaItem.Shabbat
        assertEquals("Vayera", shabbat.parsha)
        assertEquals(LocalTime.of(17, 34), shabbat.havdalah)
        assertEquals(DayOfWeek.SATURDAY, day.date.dayOfWeek)
    }

    @Test
    fun `havdalah without a reading is its own timed entry`() {
        val weekday = LocalDate.of(2026, 11, 3)
        val day = AgendaAssembler.day(calendar.getDayInfo(weekday), shabbat(havdalah = LocalTime.of(19, 10)))!!
        assertEquals(listOf(AgendaItem.Havdalah(LocalTime.of(19, 10))), day.items)
    }

    @Test
    fun `holidays lead the day in their own order`() {
        val roshChodesh = Holiday("Rosh Chodesh", "ראש חודש", HolidayCategory.MINOR)
        val chanukah = Holiday("Chanukah", "חנוכה", HolidayCategory.RABBINIC)
        val info = calendar.getDayInfo(LocalDate.of(2026, 12, 10)).copy(holidays = listOf(chanukah, roshChodesh))
        val day = AgendaAssembler.day(info, shabbat(candles = LocalTime.of(16, 0)), events = listOf(EventOccurrence(event)))!!
        // 10 December 2026 is 30 Kislev: Rosh Chodesh Teves
        assertEquals(
            listOf(AgendaItem.HolidayItem(chanukah), AgendaItem.HolidayItem(roshChodesh, HebrewMonth.TEVES)),
            day.items.take(2)
        )
        val firstOfTeves = AgendaAssembler.day(calendar.getDayInfo(LocalDate.of(2026, 12, 11)), shabbat())!!
        assertEquals(HebrewMonth.TEVES, firstOfTeves.items.filterIsInstance<AgendaItem.HolidayItem>()
            .single { it.holiday == roshChodesh }.roshChodeshOf)
        assertEquals(AgendaItem.CandleLighting(LocalTime.of(16, 0)), day.items.last())
    }

    @Test
    fun `a device event lists its start time only on its first day`() {
        val start = friday.atTime(22, 0)
        val event = device("Night shift", start, end = start.plusHours(10))
        val first = AgendaAssembler.day(calendar.getDayInfo(friday), shabbat(), deviceEvents = listOf(event))!!
        val second = AgendaAssembler.day(calendar.getDayInfo(saturday), shabbat(), deviceEvents = listOf(event))!!
        assertEquals(LocalTime.of(22, 0), (first.items.single() as AgendaItem.DeviceEventItem).time)
        assertNull((second.items.last() as AgendaItem.DeviceEventItem).time)
    }

    @Test
    fun `a day with nothing on it is left out`() {
        assertNull(AgendaAssembler.day(calendar.getDayInfo(LocalDate.of(2026, 11, 4)), shabbat()))
    }
}
