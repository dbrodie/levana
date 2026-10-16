package ninja.doskey.app.levana.ui.calendar

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarHeaderFormatterTest {
    private val repository = CalendarRepository()
    private val english = Locale.US
    private val hebrew = Locale.forLanguageTag("iw")

    private val october2026 = repository.getMonthDays(YearMonth.of(2026, 10))
    private val tishrei5787 = HebrewYearMonth.from(LocalDate.of(2026, 9, 12))
    private val tishreiDays = repository.getHebrewMonthDays(tishrei5787)

    @Test
    fun `english hebrew month range is unchanged`() {
        assertEquals(
            "Tishrei–Cheshvan 5787",
            CalendarHeaderFormatter.hebrewMonthRange(october2026, english)
        )
    }

    @Test
    fun `hebrew month range uses hebrew month names and gematria year`() {
        assertEquals(
            "תשרי–חשון תשפ״ז",
            CalendarHeaderFormatter.hebrewMonthRange(october2026, hebrew)
        )
    }

    @Test
    fun `leap year adar months read Adar I and Adar II in english`() {
        // March 2027 spans Adar I and Adar II of leap year 5787
        val march2027 = repository.getMonthDays(YearMonth.of(2027, 3))
        assertEquals(
            "Adar I–Adar II 5787",
            CalendarHeaderFormatter.hebrewMonthRange(march2027, english)
        )
        val february2027 = repository.getMonthDays(YearMonth.of(2027, 2))
        assertEquals(
            "Shevat–Adar I 5787",
            CalendarHeaderFormatter.hebrewMonthRange(february2027, english)
        )
        // Regular year: plain Adar
        val march2026 = repository.getMonthDays(YearMonth.of(2026, 3))
        assertEquals(
            "Adar–Nissan 5786",
            CalendarHeaderFormatter.hebrewMonthRange(march2026, english)
        )
    }

    @Test
    fun `leap year adar months are unchanged in hebrew`() {
        val march2027 = repository.getMonthDays(YearMonth.of(2027, 3))
        assertEquals(
            "אדר א׳–אדר ב׳ תשפ״ז",
            CalendarHeaderFormatter.hebrewMonthRange(march2027, hebrew)
        )
    }

    @Test
    fun `hebrew month range across years shows both years`() {
        val september2026 = repository.getMonthDays(YearMonth.of(2026, 9))
        assertEquals(
            "Elul 5786 – Tishrei 5787",
            CalendarHeaderFormatter.hebrewMonthRange(september2026, english)
        )
        assertEquals(
            "אלול תשפ״ו – תשרי תשפ״ז",
            CalendarHeaderFormatter.hebrewMonthRange(september2026, hebrew)
        )
    }

    @Test
    fun `hebrew month title keeps english output and uses gematria in hebrew`() {
        assertEquals("תשרי 5787", CalendarHeaderFormatter.hebrewMonthTitle(tishrei5787, english))
        assertEquals("תשרי תשפ״ז", CalendarHeaderFormatter.hebrewMonthTitle(tishrei5787, hebrew))
        assertEquals("", CalendarHeaderFormatter.hebrewMonthTitle(null, hebrew))
    }

    @Test
    fun `gregorian range is locale formatted`() {
        assertEquals(
            "Sep 12, 2026 – Oct 11, 2026",
            CalendarHeaderFormatter.gregorianRange(tishreiDays, english)
        )
        val he = CalendarHeaderFormatter.gregorianRange(tishreiDays, hebrew)
        assertTrue(he, he.contains("ספט") && he.contains("אוק"))
    }

    @Test
    fun `empty days give empty headers`() {
        assertEquals("", CalendarHeaderFormatter.hebrewMonthRange(emptyList(), hebrew))
        assertEquals("", CalendarHeaderFormatter.gregorianRange(emptyList(), hebrew))
    }
}
