package ninja.doskey.app.levana.ui.agenda

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import java.time.YearMonth
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.domain.agenda.AgendaDay
import ninja.doskey.app.levana.domain.agenda.AgendaItem
import ninja.doskey.app.levana.domain.model.Holiday
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

/** Month headers depend only on the listed days, never on how the window was loaded. */
class AgendaRowsTest {

    private val calendar = CalendarRepository()
    private val item = AgendaItem.HolidayItem(Holiday("Test", "בדיקה", HolidayCategory.MINOR))

    /** Every day from [from] to [to] (inclusive) with an entry, as the agenda lists them. */
    private fun days(from: LocalDate, to: LocalDate): List<AgendaDay> =
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }
            .map { AgendaDay(it, calendar.getHebrewDay(it), listOf(item)) }.toList()

    private fun headerDates(rows: List<AgendaRow>) = rows.filter { it.header != null }.map { it.day.date }

    private val kislev30 = JewishDate(5787, JewishDate.KISLEV, 30).localDate
    private val teves1 = kislev30.plusDays(1)

    @Test
    fun `a header starts the list and each new month of the leading calendar`() {
        val rows = agendaRows(days(LocalDate.of(2026, 11, 20), LocalDate.of(2027, 1, 15)), hebrewLeading = true)
        assertEquals(
            listOf(LocalDate.of(2026, 11, 20), teves1, JewishDate(5787, JewishDate.SHEVAT, 1).localDate),
            headerDates(rows)
        )
        assertEquals(AgendaMonth.Hebrew(HebrewYearMonth.from(teves1)), rows.first { it.day.date == teves1 }.header)

        val gregorian = agendaRows(days(LocalDate.of(2026, 11, 20), LocalDate.of(2027, 1, 15)), hebrewLeading = false)
        assertEquals(listOf(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1)), headerDates(gregorian))
        assertEquals(AgendaMonth.Gregorian(YearMonth.of(2026, 12)), gregorian.first { it.day.date == LocalDate.of(2026, 12, 1) }.header)
    }

    @Test
    fun `headers are the same however the window was assembled`() {
        val whole = days(LocalDate.of(2026, 10, 6), LocalDate.of(2027, 2, 8))
        for (hebrew in listOf(true, false)) {
            val expected = agendaRows(whole, hebrew)
            // Chunk boundaries on a month's first day, on its last day and mid-month, appended or prepended
            for (boundary in listOf(teves1, kislev30, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 15))) {
                val (before, after) = whole.partition { it.date.isBefore(boundary) }
                assertEquals("$boundary hebrew=$hebrew", expected, agendaRows(before + after, hebrew))
            }
        }
    }

    @Test
    fun `days with no entries leave no gap in the headers`() {
        // Only 30 Kislev and 2 Teves are listed: Teves still gets its header, on its first listed day
        val sparse = days(kislev30, kislev30) + days(teves1.plusDays(1), teves1.plusDays(1))
        assertEquals(listOf(kislev30, teves1.plusDays(1)), headerDates(agendaRows(sparse, hebrewLeading = true)))
    }

    @Test
    fun `switching the leading calendar keeps every list key`() {
        val listed = days(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 1, 31))
        assertEquals(agendaRows(listed, true).map { it.key }, agendaRows(listed, false).map { it.key })
        assertEquals(listed.size, agendaRows(listed, true).map { it.key }.toSet().size)
    }
}
