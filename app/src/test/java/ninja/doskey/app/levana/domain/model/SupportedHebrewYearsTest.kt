package ninja.doskey.app.levana.domain.model

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.util.GregorianCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedHebrewYearsTest {

    @Test
    fun rangeIs5600To6000() {
        assertEquals(5600..6000, SupportedHebrewYears.range)
        assertTrue(5600 in SupportedHebrewYears)
        assertTrue(6000 in SupportedHebrewYears)
        assertFalse(5599 in SupportedHebrewYears)
        assertFalse(6001 in SupportedHebrewYears)
        assertFalse(0 in SupportedHebrewYears)
    }

    @Test
    fun coerceKeepsSupportedYearsAndMovesOthersToTheNearestBound() {
        assertEquals(5600, SupportedHebrewYears.coerce(5600))
        assertEquals(5787, SupportedHebrewYears.coerce(5787))
        assertEquals(6000, SupportedHebrewYears.coerce(6000))
        assertEquals(5600, SupportedHebrewYears.coerce(5599))
        assertEquals(6000, SupportedHebrewYears.coerce(6001))
        assertEquals(5600, SupportedHebrewYears.coerce(Int.MIN_VALUE))
        assertEquals(6000, SupportedHebrewYears.coerce(Int.MAX_VALUE))
    }

    private fun hebrewYear(year: Int, month: Int, day: Int) =
        JewishDate(GregorianCalendar(year, month - 1, day)).jewishYear

    @Test
    fun gregorianYearsLieWhollyInsideTheSupportedHebrewYears() {
        val years = SupportedHebrewYears.gregorianYears
        assertEquals(1840..2239, years)
        assertEquals(5600, hebrewYear(years.first, 1, 1))
        assertEquals(6000, hebrewYear(years.last, 12, 31))
        // One year more on either side would leave the range
        assertEquals(5599, hebrewYear(years.first - 1, 1, 1))
        assertEquals(6001, hebrewYear(years.last + 1, 12, 31))
    }
}
