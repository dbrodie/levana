package ninja.doskey.app.levana

import java.time.LocalDate
import ninja.doskey.app.levana.domain.model.ThemeMode
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.ui.theme.HolidayTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MainActivity's holiday theme for the device's day. */
class MainActivityThemeTest {

    private val chanukah = LocalDate.of(2026, 12, 8)
    private val weekday = LocalDate.of(2026, 11, 17)

    @Test
    fun `no theme until the preferences and today have loaded`() {
        assertNull(resolveHolidayTheme(null, chanukah))
        assertNull(resolveHolidayTheme(UserPreferences(), null))
        assertNull(resolveHolidayTheme(null, null))
    }

    @Test
    fun `a holiday applies its theme`() {
        assertEquals(HolidayTheme.CHANUKAH, resolveHolidayTheme(UserPreferences(), chanukah))
    }

    @Test
    fun `an ordinary day has no theme`() {
        assertNull(resolveHolidayTheme(UserPreferences(), weekday))
    }

    @Test
    fun `turning off holiday themes removes it`() {
        assertNull(resolveHolidayTheme(UserPreferences(dynamicHolidayTheme = false), chanukah))
    }

    @Test
    fun `the developer's forced theme wins on any day`() {
        val forced = UserPreferences(dynamicHolidayTheme = false, devForceHolidayTheme = HolidayTheme.PURIM.name)
        assertEquals(HolidayTheme.PURIM, resolveHolidayTheme(forced, weekday))
        assertEquals(HolidayTheme.PURIM, resolveHolidayTheme(forced.copy(dynamicHolidayTheme = true), chanukah))
    }

    @Test
    fun `the system mode applies until the preferences load`() {
        assertTrue(resolveDarkTheme(null, systemDark = true))
        assertFalse(resolveDarkTheme(null, systemDark = false))
    }

    @Test
    fun `a forced mode overrides the system`() {
        assertTrue(resolveDarkTheme(UserPreferences(themeMode = ThemeMode.DARK), systemDark = false))
        assertFalse(resolveDarkTheme(UserPreferences(themeMode = ThemeMode.LIGHT), systemDark = true))
        assertTrue(resolveDarkTheme(UserPreferences(), systemDark = true))
    }
}
