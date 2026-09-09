package ninja.doskey.app.levana.data

import java.time.LocalDate
import java.time.YearMonth
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import ninja.doskey.app.levana.domain.model.HolidayCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The two calendar presentations must carry the same real-world observances. */
class CalendarPresentationTest {
    private val repository = CalendarRepository()

    @Test
    fun `holiday labels agree when switching calendar modes`() {
        val date = LocalDate.of(2026, 9, 12)
        val gregorian = repository.getMonthDays(YearMonth.from(date), inIsrael = true)
            .single { it.gregorianDate == date }
        val hebrew = repository.getHebrewMonthDays(HebrewYearMonth.from(date), inIsrael = true)
            .single { it.gregorianDate == date }
        assertEquals("Rosh Hashanah", gregorian.holidayLabel)
        assertEquals(gregorian.holidayLabel, hebrew.holidayLabel)
        assertEquals(gregorian.holidayCategory, hebrew.holidayCategory)
        assertEquals(gregorian.hebrewFormatted, hebrew.hebrewFormatted)
    }

    @Test
    fun `Israel and diaspora retain distinct second day observances`() {
        val date = LocalDate.of(2026, 4, 3)
        val israel = repository.getMonthDays(YearMonth.from(date), inIsrael = true)
            .single { it.gregorianDate == date }
        val diaspora = repository.getMonthDays(YearMonth.from(date), inIsrael = false)
            .single { it.gregorianDate == date }
        assertNotNull(israel.holidayLabel)
        assertNotNull(diaspora.holidayLabel)
        assertNotEquals(israel.holidayLabel, diaspora.holidayLabel)
    }

    @Test
    fun `modern holiday visibility applies in both calendar modes`() {
        val date = LocalDate.of(2026, 4, 22)
        val shown = repository.getMonthDays(YearMonth.from(date), showModernIsraeli = true)
            .single { it.gregorianDate == date }
        assertEquals(HolidayCategory.MODERN_ISRAELI, shown.holidayCategory)
        assertNotNull(shown.holidayLabel)
        val hiddenGregorian = repository.getMonthDays(YearMonth.from(date), showModernIsraeli = false)
            .single { it.gregorianDate == date }
        val hiddenHebrew = repository.getHebrewMonthDays(HebrewYearMonth.from(date), showModernIsraeli = false)
            .single { it.gregorianDate == date }
        assertNull(hiddenGregorian.holidayLabel)
        assertNull(hiddenHebrew.holidayLabel)
    }
}
