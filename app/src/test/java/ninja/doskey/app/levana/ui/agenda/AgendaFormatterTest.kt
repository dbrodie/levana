package ninja.doskey.app.levana.ui.agenda

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class AgendaFormatterTest {

    private val english = Locale.US
    private val hebrew = Locale.forLanguageTag("iw")
    private val cheshvan = AgendaMonth.of(LocalDate.of(2026, 10, 30), hebrewLeading = true)
    private val kislev = AgendaMonth.of(LocalDate.of(2026, 11, 20), hebrewLeading = true)

    @Test
    fun `a Hebrew month spans its Gregorian dates`() {
        assertEquals(AgendaMonth.Hebrew(HebrewYearMonth(5787, HebrewMonth.CHESHVAN, 8)), cheshvan)
        assertEquals(LocalDate.of(2026, 10, 12), cheshvan.first)
        assertEquals(LocalDate.of(2026, 11, 10), cheshvan.last)
        assertEquals("October – November 2026", AgendaFormatter.otherCalendarSpan(cheshvan, english))
        assertEquals("Nov – Dec 2026", AgendaFormatter.otherCalendarSpan(kislev, english, short = true))
    }

    @Test
    fun `the titles follow the Month screen`() {
        assertEquals("חשון 5787", AgendaFormatter.title(cheshvan, english))
        assertEquals("כסלו תשפ״ז", AgendaFormatter.title(kislev, hebrew))
        assertEquals("November 2026", AgendaFormatter.title(AgendaMonth.Gregorian(YearMonth.of(2026, 11)), english))
    }

    @Test
    fun `a Gregorian month spans its Hebrew months`() {
        val november = AgendaMonth.of(LocalDate.of(2026, 11, 5), hebrewLeading = false)
        assertEquals("Cheshvan–Kislev 5787", AgendaFormatter.otherCalendarSpan(november, english))
        val december = AgendaMonth.Gregorian(YearMonth.of(2026, 12))
        assertEquals(LocalDate.of(2026, 12, 31), december.last)
    }

    @Test
    fun `a span across years names both years`() {
        val tevet = AgendaMonth.of(LocalDate.of(2026, 12, 20), hebrewLeading = true)
        assertEquals("Dec 2026 – Jan 2027", AgendaFormatter.otherCalendarSpan(tevet, english, short = true))
    }

    @Test
    fun `the rail shows the weekday and a short Gregorian date`() {
        assertEquals("FRI", AgendaFormatter.weekday(LocalDate.of(2026, 10, 30), english))
        assertEquals("30 Oct", AgendaFormatter.shortGregorian(LocalDate.of(2026, 10, 30), english))
    }
}
