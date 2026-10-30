package ninja.doskey.app.levana.ui.theme

import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import ninja.doskey.app.levana.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Theme mode → dark, and the scheme precedence: holiday, then wallpaper colors, then Levana's. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppearanceThemeTest {

    private val wallpaper = lightColorScheme(primary = Color(0xFF00FF00))
    private val context get() = RuntimeEnvironment.getApplication()

    /** ColorScheme compares by identity; its accent and surface identify a palette. */
    private fun assertScheme(expected: ColorScheme?, actual: ColorScheme) {
        assertEquals(expected?.primary, actual.primary)
        assertEquals(expected?.surface, actual.surface)
    }

    @Test
    fun `system follows the device and light or dark ignore it`() {
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        for (systemDark in listOf(false, true)) {
            assertFalse(ThemeMode.LIGHT.isDark(systemDark))
            assertTrue(ThemeMode.DARK.isDark(systemDark))
        }
    }

    @Test
    fun `a holiday theme wins over wallpaper colors in both modes`() {
        assertScheme(HolidayTheme.PURIM.lightScheme(), levanaColorScheme(false, HolidayTheme.PURIM, wallpaper))
        assertScheme(HolidayTheme.PURIM.darkScheme(), levanaColorScheme(true, HolidayTheme.PURIM, wallpaper))
    }

    @Test
    fun `wallpaper colors win over Levana's without a holiday`() {
        assertScheme(wallpaper, levanaColorScheme(false, null, wallpaper))
    }

    @Test
    fun `without either Levana's colors follow the mode`() {
        assertScheme(LightColorScheme, levanaColorScheme(false, null))
        assertScheme(DarkColorScheme, levanaColorScheme(true, null))
    }

    @Test
    fun `material you off keeps Levana's colors`() {
        assertScheme(LightColorScheme, levanaColorScheme(context, false, null, materialYou = false))
        assertScheme(DarkColorScheme, levanaColorScheme(context, true, null, materialYou = false))
    }

    @Test
    fun `material you on uses the wallpaper's colors for the mode`() {
        assertTrue(supportsMaterialYou)
        assertScheme(wallpaperColorScheme(context, false), levanaColorScheme(context, false, null, materialYou = true))
        assertScheme(wallpaperColorScheme(context, true), levanaColorScheme(context, true, null, materialYou = true))
        assertNotEquals(LightColorScheme.primary, levanaColorScheme(context, false, null, materialYou = true).primary)
    }

    @Test
    fun `material you on still yields to a holiday theme`() {
        assertScheme(HolidayTheme.SUKKOT.lightScheme(), levanaColorScheme(context, false, HolidayTheme.SUKKOT, materialYou = true))
        assertScheme(HolidayTheme.SUKKOT.darkScheme(), levanaColorScheme(context, true, HolidayTheme.SUKKOT, materialYou = true))
    }
}
