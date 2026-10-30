package ninja.doskey.app.levana.ui.datepicker

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.SupportedHebrewYears
import ninja.doskey.app.levana.ui.datepicker.DatePickerIntent.NextMonth
import ninja.doskey.app.levana.ui.datepicker.DatePickerIntent.PickYear
import ninja.doskey.app.levana.ui.datepicker.DatePickerIntent.PreviousMonth
import ninja.doskey.app.levana.ui.datepicker.DatePickerIntent.SelectDay
import ninja.doskey.app.levana.ui.datepicker.DatePickerIntent.SetLeading
import ninja.doskey.app.levana.ui.datepicker.DatePickerIntent.ToggleYearList
import ninja.doskey.app.levana.ui.datepicker.LeadingCalendar.GREGORIAN
import ninja.doskey.app.levana.ui.datepicker.LeadingCalendar.HEBREW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** The shared date picker's rules (TASK-45), outside Compose. */
class DatePickerReducerTest {

    private fun hebrew(day: Int, month: Int, year: Int): LocalDate = JewishDate(year, month, day).localDate

    private fun DatePickerState.after(vararg intents: DatePickerIntent): DatePickerState =
        intents.fold(this) { state, intent -> DatePickerReducer.reduce(state, intent) }

    private fun DatePickerState.shownHebrew() = Triple(hebrewMonth.year, hebrewMonth.jewishDateMonth, hebrewMonth.month)

    private fun hebrewOf(date: LocalDate): Triple<Int, Int, Int> =
        JewishDate(date).let { Triple(it.jewishDayOfMonth, it.jewishMonth, it.jewishYear) }

    // 18 Cheshvan 5787, the mockup's selected day
    private val oct29 = LocalDate.of(2026, 10, 29)

    @Test
    fun opensOnTheMonthHoldingTheInitialDateInBothCalendars() {
        val state = DatePickerReducer.initial(oct29, HEBREW)
        assertEquals(Triple(5787, JewishDate.CHESHVAN, HebrewMonth.CHESHVAN), state.shownHebrew())
        assertEquals(YearMonth.of(2026, 10), state.gregorianMonth)
        assertEquals(oct29, state.selection)
        assertFalse(state.yearListOpen)
        assertNull(state.extraHebrewYear)
    }

    @Test
    fun switchingTheLeadingCalendarShowsTheMonthHoldingTheSelection() {
        // Mockup states 04/05: pick 15 Nov in Gregorian mode, switch to Hebrew: Kislev 5787
        val gregorian = DatePickerReducer.initial(oct29, GREGORIAN).after(NextMonth, SelectDay(LocalDate.of(2026, 11, 15)))
        assertEquals(YearMonth.of(2026, 11), gregorian.gregorianMonth)
        val hebrew = gregorian.after(SetLeading(HEBREW))
        assertEquals(HEBREW, hebrew.leading)
        assertEquals(Triple(5787, JewishDate.KISLEV, HebrewMonth.KISLEV), hebrew.shownHebrew())
        assertEquals(LocalDate.of(2026, 11, 15), hebrew.selection)

        // Browsing Hebrew months away from the selection, then switching back (state 13)
        val browsed = hebrew.after(NextMonth, NextMonth, NextMonth, NextMonth, NextMonth)
        val back = browsed.after(SetLeading(GREGORIAN))
        assertEquals(YearMonth.of(2026, 11), back.gregorianMonth)
    }

    @Test
    fun switchingCloseTheYearList() {
        val state = DatePickerReducer.initial(oct29, HEBREW).after(ToggleYearList, SetLeading(GREGORIAN))
        assertFalse(state.yearListOpen)
    }

    @Test
    fun hebrewMonthsStepThroughAdarIAndAdarIIInALeapYear() {
        val shevat = DatePickerReducer.initial(hebrew(1, JewishDate.SHEVAT, 5787), HEBREW)
        val months = generateSequence(shevat) { it.after(NextMonth) }.take(4).map { it.hebrewMonth.month }.toList()
        assertEquals(listOf(HebrewMonth.SHEVAT, HebrewMonth.ADAR_I, HebrewMonth.ADAR_II, HebrewMonth.NISSAN), months)
        val nissan = shevat.after(NextMonth, NextMonth, NextMonth)
        assertEquals(HebrewMonth.ADAR_II, nissan.after(PreviousMonth).hebrewMonth.month)
        assertEquals(HebrewMonth.ADAR_I, nissan.after(PreviousMonth, PreviousMonth).hebrewMonth.month)
    }

    @Test
    fun hebrewMonthsSkipAdarIIInAnOrdinaryYear() {
        val shevat = DatePickerReducer.initial(hebrew(1, JewishDate.SHEVAT, 5786), HEBREW)
        assertEquals(HebrewMonth.ADAR, shevat.after(NextMonth).hebrewMonth.month)
        assertEquals(HebrewMonth.NISSAN, shevat.after(NextMonth, NextMonth).hebrewMonth.month)
    }

    @Test
    fun hebrewMonthsCrossTheYearAtElul() {
        val elul = DatePickerReducer.initial(hebrew(1, JewishDate.ELUL, 5786), HEBREW)
        assertEquals(Triple(5787, JewishDate.TISHREI, HebrewMonth.TISHREI), elul.after(NextMonth).shownHebrew())
        assertEquals(Triple(5786, JewishDate.ELUL, HebrewMonth.ELUL), elul.after(NextMonth, PreviousMonth).shownHebrew())
    }

    @Test
    fun gregorianMonthsStepAcrossYears() {
        val state = DatePickerReducer.initial(LocalDate.of(2026, 12, 5), GREGORIAN)
        assertEquals(YearMonth.of(2027, 1), state.after(NextMonth).gregorianMonth)
        assertEquals(YearMonth.of(2026, 11), state.after(PreviousMonth).gregorianMonth)
    }

    @Test
    fun steppingMonthsMovesOnlyTheViewNotTheSelection() {
        val state = DatePickerReducer.initial(oct29, HEBREW).after(NextMonth)
        assertEquals(oct29, state.selection)
    }

    @Test
    fun selectingADaySelectsItWithoutMovingTheView() {
        val day = LocalDate.of(2026, 11, 2)
        val state = DatePickerReducer.initial(oct29, HEBREW).after(SelectDay(day))
        assertEquals(day, state.selection)
        assertEquals(JewishDate.CHESHVAN, state.hebrewMonth.jewishDateMonth)
    }

    @Test
    fun pickingAHebrewYearMovesTheSelectionToTheSameDayAndMonth() {
        // Owner decision 2: Kislev 5787 -> 5784 moves 5 Kislev to 5 Kislev 5784
        val state = DatePickerReducer.initial(hebrew(5, JewishDate.KISLEV, 5787), HEBREW)
            .after(ToggleYearList, PickYear(5784))
        assertEquals(Triple(5, JewishDate.KISLEV, 5784), hebrewOf(state.selection))
        assertEquals(Triple(5784, JewishDate.KISLEV, HebrewMonth.KISLEV), state.shownHebrew())
        assertFalse(state.yearListOpen)
    }

    @Test
    fun pickingAnOrdinaryYearFromAdarIIGivesAdar() {
        // Mockup state 12: Purim (14 Adar II 5787) -> 5786 is 14 Adar 5786
        val state = DatePickerReducer.initial(hebrew(14, JewishDate.ADAR_II, 5787), HEBREW).after(PickYear(5786))
        assertEquals(Triple(14, JewishDate.ADAR, 5786), hebrewOf(state.selection))
        assertEquals(HebrewMonth.ADAR, state.hebrewMonth.month)
    }

    @Test
    fun pickingALeapYearFromAdarGivesAdarI() {
        // HebrewDateMatcher.monthForYear, the editors' rule: Adar stays Adar (Adar I)
        val state = DatePickerReducer.initial(hebrew(14, JewishDate.ADAR, 5786), HEBREW).after(PickYear(5787))
        assertEquals(Triple(14, JewishDate.ADAR, 5787), hebrewOf(state.selection))
        assertEquals(HebrewMonth.ADAR_I, state.hebrewMonth.month)
    }

    @Test
    fun pickingAYearWhereTheMonthIsShortGivesDay29() {
        // Adar I has 30 days; Adar of an ordinary year has 29
        val adarI30 = DatePickerReducer.initial(hebrew(30, JewishDate.ADAR, 5787), HEBREW).after(PickYear(5786))
        assertEquals(Triple(29, JewishDate.ADAR, 5786), hebrewOf(adarI30.selection))

        // Cheshvan has 30 days in some years and 29 in others
        val long = SupportedHebrewYears.range.first { HebrewDateMatcher.daysInMonth(it, JewishDate.CHESHVAN) == 30 }
        val short = SupportedHebrewYears.range.first { HebrewDateMatcher.daysInMonth(it, JewishDate.CHESHVAN) == 29 }
        val cheshvan30 = DatePickerReducer.initial(hebrew(30, JewishDate.CHESHVAN, long), HEBREW).after(PickYear(short))
        assertEquals(Triple(29, JewishDate.CHESHVAN, short), hebrewOf(cheshvan30.selection))
    }

    @Test
    fun pickingAGregorianYearMovesTheSelectionAndFitsLeapDay() {
        val nov15 = DatePickerReducer.initial(LocalDate.of(2026, 11, 15), GREGORIAN).after(ToggleYearList, PickYear(2030))
        assertEquals(LocalDate.of(2030, 11, 15), nov15.selection)
        assertEquals(YearMonth.of(2030, 11), nov15.gregorianMonth)
        assertFalse(nov15.yearListOpen)

        val leapDay = DatePickerReducer.initial(LocalDate.of(2028, 2, 29), GREGORIAN).after(PickYear(2027))
        assertEquals(LocalDate.of(2027, 2, 28), leapDay.selection)
    }

    @Test
    fun yearListsCoverTheSupportedRange() {
        val hebrew = DatePickerReducer.initial(oct29, HEBREW)
        assertEquals(5600..6000, DatePickerReducer.yearOptions(hebrew))
        assertEquals(5787, DatePickerReducer.viewedYear(hebrew))
        val gregorian = hebrew.after(SetLeading(GREGORIAN))
        assertEquals(SupportedHebrewYears.gregorianYears, DatePickerReducer.yearOptions(gregorian))
        assertEquals(1840..2239, DatePickerReducer.yearOptions(gregorian))
        assertEquals(2026, DatePickerReducer.viewedYear(gregorian))
    }

    @Test
    fun theCurrentYearComesFromToday() {
        val today = LocalDate.of(2026, 11, 17)
        val state = DatePickerReducer.initial(LocalDate.of(1990, 1, 1), HEBREW, today)
        assertEquals(5787, DatePickerReducer.currentYear(state))
        assertEquals(2026, DatePickerReducer.currentYear(state.after(SetLeading(GREGORIAN))))
        assertNull(DatePickerReducer.currentYear(DatePickerReducer.initial(oct29, HEBREW)))
    }

    @Test
    fun yearsOutsideTheListAreIgnored() {
        val state = DatePickerReducer.initial(oct29, HEBREW)
        assertEquals(state, state.after(PickYear(5599)))
        assertEquals(state, state.after(PickYear(6001)))
        val gregorian = state.after(SetLeading(GREGORIAN))
        assertEquals(gregorian, gregorian.after(PickYear(1839)))
        assertEquals(gregorian, gregorian.after(PickYear(2240)))
    }

    @Test
    fun hebrewMonthsStopAtTheSupportedRange() {
        val first = DatePickerReducer.initial(hebrew(1, JewishDate.TISHREI, 5600), HEBREW)
        assertFalse(DatePickerReducer.canGoPrevious(first))
        assertEquals(first, first.after(PreviousMonth))
        assertTrue(DatePickerReducer.canGoNext(first))

        val last = DatePickerReducer.initial(hebrew(29, JewishDate.ELUL, 6000), HEBREW)
        assertFalse(DatePickerReducer.canGoNext(last))
        assertEquals(last, last.after(NextMonth))
        assertTrue(DatePickerReducer.canGoPrevious(last))
    }

    @Test
    fun gregorianMonthsStopWhereNoDayIsInTheSupportedRange() {
        val firstDay = hebrew(1, JewishDate.TISHREI, 5600)
        val first = DatePickerReducer.initial(firstDay, GREGORIAN)
        assertFalse(DatePickerReducer.canGoPrevious(first))
        assertFalse(DatePickerReducer.isSelectable(first, firstDay.minusDays(1)))
        assertTrue(DatePickerReducer.isSelectable(first, firstDay))
        assertEquals(firstDay, first.after(SelectDay(firstDay.minusDays(1))).selection)

        val lastDay = hebrew(29, JewishDate.ELUL, 6000)
        val last = DatePickerReducer.initial(lastDay, GREGORIAN)
        assertFalse(DatePickerReducer.canGoNext(last))
        assertFalse(DatePickerReducer.isSelectable(last, lastDay.plusDays(1)))
    }

    @Test
    fun anOutOfRangeInitialDateOpensUnchangedWithinItsOwnYear() {
        // TASK-24: a stored 12 Nissan 5500 opens as is and stays pickable within 5500
        val stored = hebrew(12, JewishDate.NISSAN, 5500)
        val state = DatePickerReducer.initial(stored, HEBREW)
        assertEquals(stored, state.selection)
        assertEquals(5500, state.extraHebrewYear)
        assertEquals(Triple(5500, JewishDate.NISSAN, HebrewMonth.NISSAN), state.shownHebrew())
        assertTrue(DatePickerReducer.isSelectable(state, stored.plusDays(1)))
        assertEquals(stored.plusDays(1), state.after(SelectDay(stored.plusDays(1))).selection)

        // Months step within 5500 (Nissan..Elul) but not beyond it
        val elul = state.after(NextMonth, NextMonth, NextMonth, NextMonth, NextMonth)
        assertEquals(JewishDate.ELUL, elul.hebrewMonth.jewishDateMonth)
        assertFalse(DatePickerReducer.canGoNext(elul))
        val tishrei = DatePickerReducer.initial(hebrew(1, JewishDate.TISHREI, 5500), HEBREW)
        assertFalse(DatePickerReducer.canGoPrevious(tishrei))
        assertFalse(DatePickerReducer.isSelectable(state, hebrew(1, JewishDate.TISHREI, 5501)))

        // The year list moves it into the range
        val picked = state.after(ToggleYearList, PickYear(5787))
        assertEquals(Triple(12, JewishDate.NISSAN, 5787), hebrewOf(picked.selection))
    }

    @Test
    fun anOutOfRangeInitialDateInGregorianModeStaysWithinItsHebrewYear() {
        val stored = hebrew(12, JewishDate.NISSAN, 6100)
        val state = DatePickerReducer.initial(stored, GREGORIAN)
        assertEquals(6100, state.extraHebrewYear)
        assertTrue(DatePickerReducer.isSelectable(state, stored))
        // 1840..2239 offers no year of 6100 (2340); picking one moves it into the range
        assertEquals(2026, state.after(PickYear(2026)).selection.year)
        assertEquals(stored.withYear(2026), state.after(PickYear(2026)).selection)
    }
}
