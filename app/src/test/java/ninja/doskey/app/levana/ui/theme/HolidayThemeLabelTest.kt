package ninja.doskey.app.levana.ui.theme

import ninja.doskey.app.levana.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HolidayThemeLabelTest {

    @Test
    fun `every holiday theme has a distinct label`() {
        val labels = HolidayTheme.entries.map { it.labelRes }
        assertEquals(HolidayTheme.entries.size, labels.toSet().size)
        assertFalse(labels.contains(R.string.holiday_theme_none))
    }

    @Test
    fun `labels map to the expected holidays`() {
        assertEquals(R.string.holiday_theme_shemini_atzeret, HolidayTheme.SHMINI_ATZERET.labelRes)
        assertEquals(R.string.holiday_theme_rosh_hashanah, HolidayTheme.ROSH_HASHANAH.labelRes)
        assertEquals(R.string.holiday_theme_fast_day, HolidayTheme.FAST_DAY.labelRes)
    }
}
