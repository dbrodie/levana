package ninja.doskey.app.levana.domain.agenda

import java.time.LocalDate
import java.time.LocalTime
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.domain.model.ContactBirthday
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.Holiday
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.domain.model.SystemCalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class AgendaSearchTest {

    private val calendar = CalendarRepository()
    private fun day(date: LocalDate, vararg items: AgendaItem) = AgendaDay(date, calendar.getHebrewDay(date), items.toList())

    private val shabbat = AgendaItem.Shabbat("Chayei Sara", "חיי שרה", null, LocalTime.of(17, 29))
    private val roshChodesh = AgendaItem.HolidayItem(Holiday("Rosh Chodesh", "ראש חודש", HolidayCategory.MINOR), HebrewMonth.KISLEV)
    private val yahrzeit = AgendaItem.PersonalEventItem(
        PersonalEvent(id = 1, title = "Moshe ben Avraham", hebrewDay = 23, hebrewMonth = 8, hebrewYear = 5770,
            notes = "Candle at tzeit", useYahrzeitRules = true), null
    )
    private val birthday = AgendaItem.BirthdayItem(ContactBirthday("k", "Sarah Levi", null, 1, 1, 0), null)
    private val kiddush = AgendaItem.DeviceEventItem(
        SystemCalendarEvent(9, "Kiddush — Cohen bar mitzvah", LocalDate.of(2026, 10, 31).atTime(11, 30),
            LocalDate.of(2026, 10, 31).atTime(13, 0), false, 1, 0, location = "Yeshurun Synagogue"),
        LocalTime.of(11, 30)
    )
    private val candles = AgendaItem.CandleLighting(LocalTime.of(16, 21))

    private val days = listOf(
        day(LocalDate.of(2026, 10, 30), candles),
        day(LocalDate.of(2026, 10, 31), kiddush),
        day(LocalDate.of(2026, 11, 3), yahrzeit),
        day(LocalDate.of(2026, 11, 7), shabbat, birthday),
        day(LocalDate.of(2026, 11, 10), roshChodesh),
    )

    private fun dates(query: String) = AgendaSearch.search(days, query).map { it.date.dayOfMonth }

    @Test
    fun `matches titles, notes, locations and both holiday languages`() {
        assertEquals(listOf(3), dates("moshe"))
        assertEquals(listOf(3), dates("TZEIT"))
        assertEquals(listOf(31), dates("yeshurun"))
        assertEquals(listOf(10), dates("rosh chodesh"))
        assertEquals(listOf(10), dates("ראש חודש"))
        assertEquals(listOf(10), dates("rosh chodesh kislev"))
        assertEquals(listOf(10), dates("ראש חודש כסלו"))
        assertEquals(listOf(7), dates("חיי"))
    }

    @Test
    fun `a matching day keeps only its matching entries`() {
        val result = AgendaSearch.search(days, "sarah").single()
        assertEquals(listOf(birthday), result.items)
        // "Sara" matches both the parasha and the birthday
        assertEquals(listOf(shabbat, birthday), AgendaSearch.search(days, "sara").single().items)
    }

    @Test
    fun `punctuation, gershayim and spacing are ignored`() {
        assertEquals(listOf(7), dates("  chayei   sara "))
        assertEquals(listOf(31), dates("cohen bar"))
        val hebrew = listOf(day(LocalDate.of(2026, 12, 5), AgendaItem.HolidayItem(Holiday("Chanukah", "חנוכה: נר א׳", HolidayCategory.RABBINIC))))
        assertEquals(1, AgendaSearch.search(hebrew, "נר א").size)
    }

    @Test
    fun `a blank query or times alone find nothing`() {
        assertEquals(emptyList<Int>(), dates(" "))
        assertEquals(emptyList<Int>(), dates("16:21"))
    }

    @Test
    fun `the search range is twelve months each way`() {
        val range = AgendaSearch.range(LocalDate.of(2026, 10, 30))
        assertEquals(LocalDate.of(2025, 10, 30), range.start)
        assertEquals(LocalDate.of(2027, 10, 30), range.endInclusive)
    }
}
