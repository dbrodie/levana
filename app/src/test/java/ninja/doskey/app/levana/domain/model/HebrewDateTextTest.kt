package ninja.doskey.app.levana.domain.model

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** The app's one way of writing Hebrew dates and numbers (TASK-57.25). */
class HebrewDateTextTest {

    @Test
    fun yearsUseTheShortForm() {
        assertEquals("תשפ״ז", HebrewDateText.hebrewYear(5787))
        assertEquals("ת״ר", HebrewDateText.hebrewYear(5600))
        assertEquals("תשע״ה", HebrewDateText.hebrewYear(5775))
    }

    @Test
    fun aWholeThousandIsWrittenInWords() {
        assertEquals("ו׳ אלפים", HebrewDateText.hebrewYear(6000))
    }

    @Test
    fun numbersUseGereshAndGershayim() {
        assertEquals("א׳", HebrewDateText.hebrewNumber(1))
        assertEquals("ט״ו", HebrewDateText.hebrewNumber(15))
        assertEquals("ל׳", HebrewDateText.hebrewNumber(30))
    }

    @Test
    fun wholeDatesUseTheSameYearForm() {
        val date = JewishDate(5787, JewishDate.CHESHVAN, 18)
        assertEquals("י״ח חשון תשפ״ז", HebrewDateText.hebrewDate(date))
        assertEquals("י״ח חשון תשפ״ז", HebrewDateText.date(date, hebrew = true))
        assertEquals("18 Cheshvan 5787", HebrewDateText.date(date, hebrew = false))
    }

    @Test
    fun monthsInBothLanguages() {
        val adarII = JewishDate(5787, JewishDate.ADAR_II, 1)
        assertEquals("אדר ב׳", HebrewDateText.hebrewMonth(adarII))
        assertEquals("Adar II", HebrewDateText.transliteratedMonth(adarII))
    }

    @Test
    fun omerCount() {
        assertEquals("ל״ג בעומר", HebrewDateText.hebrewOmer(JewishCalendar(5786, JewishDate.IYAR, 18)))
    }
}
