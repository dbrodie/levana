package ninja.doskey.app.levana.ui.datepicker

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** The picker's text (design 1b), and its month names matching the rest of the app. */
class DatePickerFormatterTest {

    private val repository = CalendarRepository()
    private val english = Locale.ENGLISH
    private val hebrew = Locale("iw")
    private val formatter = HebrewDateFormatter().apply { isHebrewFormat = true }

    private fun hebrewMonth(year: Int, month: Int) =
        HebrewYearMonth.from(JewishDate(year, month, 1).localDate)

    @Test
    fun hebrewLedTitleAndGregorianSpan() {
        val cheshvan = hebrewMonth(5787, JewishDate.CHESHVAN)
        assertEquals("חשון ה׳תשפ״ז", DatePickerFormatter.hebrewMonthTitle(cheshvan))
        assertEquals("Oct – Nov 2026", DatePickerFormatter.gregorianSpan(repository.getHebrewMonthDays(cheshvan), english))
        val teves = hebrewMonth(5787, JewishDate.TEVES)
        assertEquals("Dec 2026 – Jan 2027", DatePickerFormatter.gregorianSpan(repository.getHebrewMonthDays(teves), english))
        assertEquals("אדר ב׳ ה׳תשפ״ז", DatePickerFormatter.hebrewMonthTitle(hebrewMonth(5787, JewishDate.ADAR_II)))
    }

    @Test
    fun gregorianLedTitleAndHebrewSpan() {
        val october = YearMonth.of(2026, 10)
        assertEquals("October 2026", DatePickerFormatter.gregorianMonthTitle(october, english))
        val days = repository.getMonthDays(october)
        assertEquals("Tishrei–Cheshvan 5787", DatePickerFormatter.hebrewSpan(days, english))
        assertEquals("תשרי–חשון תשפ״ז", DatePickerFormatter.hebrewSpan(days, hebrew))
        val march = repository.getMonthDays(YearMonth.of(2027, 3))
        assertEquals("Adar I–Adar II 5787", DatePickerFormatter.hebrewSpan(march, english))
    }

    @Test
    fun hebrewYearsAtTheRangeEnds() {
        assertEquals("ה׳תר״צ", DatePickerFormatter.hebrewYear(5690))
        assertEquals("ה׳ת״ר", DatePickerFormatter.hebrewYear(5600))
        assertEquals("ו׳ אלפים", DatePickerFormatter.hebrewYear(6000))
    }

    @Test
    fun footerDates() {
        val oct29 = LocalDate.of(2026, 10, 29)
        assertEquals("י״ח חשון ה׳תשפ״ז", DatePickerFormatter.hebrewDate(oct29))
        assertEquals("Thu, 29 Oct 2026", DatePickerFormatter.gregorianDate(oct29, english))
    }

    @Test
    fun cellLabels() {
        val cheshvan = repository.getHebrewMonthDays(hebrewMonth(5787, JewishDate.CHESHVAN))
        // 1 Cheshvan is 12 Oct; 21 Cheshvan is 1 Nov
        assertEquals("12 Oct", DatePickerFormatter.gregorianCellLabel(cheshvan[0], firstCell = true, english))
        assertEquals("13", DatePickerFormatter.gregorianCellLabel(cheshvan[1], firstCell = false, english))
        assertEquals("1 Nov", DatePickerFormatter.gregorianCellLabel(cheshvan[20], firstCell = false, english))

        val october = repository.getMonthDays(YearMonth.of(2026, 10))
        assertEquals("חשון", DatePickerFormatter.hebrewCellLabel(october[11]))
        assertEquals("ל׳", DatePickerFormatter.hebrewCellLabel(october[10]))
    }

    @Test
    fun dayDescriptionNamesBothDatesTheHolidayAndToday() {
        val purim = JewishDate(5787, JewishDate.ADAR_II, 14).localDate
        val day = repository.getMonthDays(YearMonth.from(purim)).first { it.gregorianDate == purim }
        assertEquals(
            "Tuesday, March 23, 2027, 14 Adar II, 5787, Purim, Today",
            DatePickerFormatter.dayDescription(day, english, today = "Today")
        )
        assertEquals("Purim", DatePickerFormatter.holidayName(day, english))
        assertEquals("פורים", DatePickerFormatter.holidayName(day, hebrew))
    }

    /** The picker's titles use the same month names as the calendar and the events list. */
    @Test
    fun monthTitlesMatchTheAppsMonthNamesThroughOrdinaryAndLeapYears() {
        for (year in listOf(5786, 5787)) {
            var state = DatePickerReducer.initial(JewishDate(year, JewishDate.TISHREI, 1).localDate, LeadingCalendar.HEBREW)
            val titles = mutableListOf<String>()
            while (state.hebrewMonth.year == year) {
                titles += DatePickerFormatter.hebrewMonthTitle(state.hebrewMonth)
                assertEquals(
                    "${state.hebrewMonth.month.hebrewName} ${DatePickerFormatter.hebrewYear(year)}",
                    titles.last()
                )
                state = DatePickerReducer.reduce(state, DatePickerIntent.NextMonth)
            }
            val leap = JewishDate(year, JewishDate.TISHREI, 1).isJewishLeapYear
            assertEquals(if (leap) 13 else 12, titles.size)
        }
    }

    @Test
    fun sharedHebrewMonthNamesMatchKosherJava() {
        for (month in HebrewMonth.entries) {
            val isLeap = month == HebrewMonth.ADAR_I || month == HebrewMonth.ADAR_II
            val year = if (isLeap) 5787 else 5786
            assertEquals(month.name, formatter.formatMonth(JewishDate(year, month.jewishDateValue, 1)), month.hebrewName)
        }
    }
}
