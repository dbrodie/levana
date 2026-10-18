package ninja.doskey.app.levana.ui.settings

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DateOverrideParsingTest {

    @Test
    fun validHebrewDateIsConverted() {
        // 1 Tishrei 5787 = 12 September 2026
        assertEquals(LocalDate.of(2026, 9, 12), parseHebrewDateOverride("5787", "7", "1"))
        assertEquals(LocalDate.of(2026, 9, 12), parseHebrewDateOverride(" 5787 ", "7", "1"))
    }

    @Test
    fun yearsOutsideThePickerRangeAreRejected() {
        for (year in listOf("0", "1", "-1", "3761", "3762", "5699", "5901", "10000", "999999")) {
            assertNull("year $year", parseHebrewDateOverride(year, "7", "1"))
        }
        assertNotNull(parseHebrewDateOverride("5700", "7", "1"))
        assertNotNull(parseHebrewDateOverride("5900", "7", "1"))
    }

    @Test
    fun impossibleDatesAndGarbageAreRejected() {
        assertNull(parseHebrewDateOverride("5787", "14", "1"))
        assertNull(parseHebrewDateOverride("5786", "13", "1"))
        assertNull(parseHebrewDateOverride("5787", "7", "31"))
        assertNull(parseHebrewDateOverride("", "7", "1"))
        assertNull(parseHebrewDateOverride("abc", "7", "1"))
    }
}
