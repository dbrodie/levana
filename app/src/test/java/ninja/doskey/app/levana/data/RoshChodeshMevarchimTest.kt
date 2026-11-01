package ninja.doskey.app.levana.data

import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import ninja.doskey.app.levana.domain.model.Molad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Rosh Chodesh in the month grid, Shabbat Mevarchim and its molad (TASK-57.6). */
class RoshChodeshMevarchimTest {

    private val calendar = CalendarRepository()

    private fun day(date: LocalDate) =
        calendar.getMonthDays(YearMonth.from(date)).single { it.gregorianDate == date }

    @Test
    fun `both days of a two-day Rosh Chodesh carry the chip`() {
        // 30 Cheshvan and 1 Kislev 5787
        for (date in listOf(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11))) {
            val day = day(date)
            assertEquals(date.toString(), "Rosh Chodesh", day.holidayLabel)
            assertEquals("ראש חודש", day.holidayLabelHebrew)
            assertEquals(HolidayCategory.MINOR, day.holidayCategory)
        }
        assertNull(day(LocalDate.of(2026, 11, 12)).holidayLabel)
    }

    @Test
    fun `a one-day Rosh Chodesh carries the chip`() {
        // Adar II always has 29 days, so Rosh Chodesh Nisan is one day
        val firstOfNisan = LocalDate.of(2027, 4, 8)
        assertEquals("Rosh Chodesh", day(firstOfNisan).holidayLabel)
        assertNull(day(firstOfNisan.minusDays(1)).holidayLabel)
    }

    @Test
    fun `Rosh Chodesh Tevet during Chanukah keeps the Chanukah chip`() {
        for (date in listOf(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 11))) {
            assertEquals("Chanukah", day(date).holidayLabel)
        }
    }

    @Test
    fun `Rosh Hashana is not marked as Rosh Chodesh`() {
        assertEquals("Rosh Hashanah", calendar.getMonthDays(YearMonth.of(2026, 9)).single { it.gregorianDate == LocalDate.of(2026, 9, 12) }.holidayLabel)
    }

    @Test
    fun `the Hebrew-mode grid marks Rosh Chodesh too`() {
        val cheshvan = calendar.getHebrewMonthDays(HebrewYearMonth(5787, HebrewMonth.CHESHVAN, HebrewMonth.CHESHVAN.jewishDateValue))
        assertEquals("Rosh Chodesh", cheshvan.first().holidayLabel)
        assertEquals("Rosh Chodesh", cheshvan.last().holidayLabel)
        assertEquals(30, cheshvan.last().day)
    }

    @Test
    fun `Shabbat Mevarchim is the Shabbat before Rosh Chodesh, but not in Elul`() {
        assertTrue(day(LocalDate.of(2026, 11, 7)).isShabbatMevarchim) // 27 Cheshvan
        assertTrue(day(LocalDate.of(2026, 10, 10)).isShabbatMevarchim) // 29 Tishrei
        assertFalse(day(LocalDate.of(2026, 10, 31)).isShabbatMevarchim) // 20 Cheshvan
        assertFalse(day(LocalDate.of(2027, 9, 4)).isShabbatMevarchim) // 27 Elul 5787
        assertFalse(day(LocalDate.of(2026, 11, 8)).isShabbatMevarchim) // Sunday
        assertNull(calendar.getDayInfo(LocalDate.of(2027, 9, 4)).molad)
    }

    /**
     * Pinned to Hebcal's published molad (hebcal.com/hebcal?v=1&cfg=json&molad=on), which gives
     * the traditional reckoning in Jerusalem mean time, e.g. "Molad Kislev: Monday, 10:27pm and
     * 3 chalakim". Nisan and Tammuz fall in Israeli summer time, where civil time would be off
     * by about 1h21m instead of 21m.
     */
    @Test
    fun `the molad matches the published table`() {
        val expected = mapOf(
            LocalDate.of(2026, 11, 7) to Molad(HebrewMonth.KISLEV, DayOfWeek.MONDAY, 22, 27, 3),
            LocalDate.of(2027, 3, 6) to Molad(HebrewMonth.ADAR_II, DayOfWeek.MONDAY, 1, 23, 7),
            LocalDate.of(2027, 4, 3) to Molad(HebrewMonth.NISSAN, DayOfWeek.TUESDAY, 14, 7, 8),
            LocalDate.of(2027, 6, 5) to Molad(HebrewMonth.SIVAN, DayOfWeek.FRIDAY, 15, 35, 10),
            LocalDate.of(2027, 7, 3) to Molad(HebrewMonth.TAMMUZ, DayOfWeek.SUNDAY, 4, 19, 11),
        )
        expected.forEach { (shabbat, molad) ->
            assertEquals(shabbat.toString(), molad, calendar.getDayInfo(shabbat).molad)
        }
    }
}
