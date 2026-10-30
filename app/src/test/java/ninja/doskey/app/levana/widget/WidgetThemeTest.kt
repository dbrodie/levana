package ninja.doskey.app.levana.widget

import java.time.LocalDate
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.ui.theme.HolidayTheme
import ninja.doskey.app.levana.ui.theme.activeHolidayTheme
import ninja.doskey.app.levana.ui.theme.levanaColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The widgets color through the app's own theme functions. */
class WidgetThemeTest {

    private val purim = LocalDate.of(2026, 3, 3)

    @Test
    fun `holiday scheme is the holiday theme's scheme`() {
        val light = levanaColorScheme(false, HolidayTheme.PURIM)
        val dark = levanaColorScheme(true, HolidayTheme.PURIM)
        assertEquals(HolidayTheme.PURIM.lightPrimary, light.primary)
        assertEquals(HolidayTheme.PURIM.lightContainer, light.primaryContainer)
        assertEquals(HolidayTheme.PURIM.darkPrimary, dark.primary)
        assertEquals(HolidayTheme.PURIM.darkContainer, dark.primaryContainer)
    }

    @Test
    fun `active holiday theme follows the date, the setting and the forced theme`() {
        assertEquals(HolidayTheme.PURIM, activeHolidayTheme(UserPreferences(), purim))
        assertNull(activeHolidayTheme(UserPreferences(dynamicHolidayTheme = false), purim))
        assertEquals(
            HolidayTheme.SUKKOT,
            activeHolidayTheme(UserPreferences(dynamicHolidayTheme = false, devForceHolidayTheme = "SUKKOT"), purim),
        )
        assertNull(activeHolidayTheme(UserPreferences(), LocalDate.of(2026, 10, 29)))
    }

    @Test
    fun `widget settings ignore unrelated preference changes`() {
        val base = UserPreferences()
        assertEquals(base.widgetSettings(), base.copy(notifyOmer = true).widgetSettings())
        assert(base.widgetSettings() != base.copy(selectedZmanim = setOf("Sunrise")).widgetSettings())
        // The widget shows the device position (TASK-5): selecting a city changes nothing
        val newYork = ninja.doskey.app.levana.domain.model.Location(40.7128, -74.006, 0.0, "America/New_York", "New York")
        val nySelected = base.copy(
            savedLocations = listOf(ninja.doskey.app.levana.domain.model.SavedLocation("ny", newYork)),
            activeLocationId = "ny"
        )
        assertEquals(base.widgetSettings(), nySelected.widgetSettings())
        assert(base.widgetSettings() != base.copy(currentLocation = newYork).widgetSettings())
        // Widgets take wallpaper colors but keep the launcher's light/dark (TASK-13)
        assert(base.widgetSettings() != base.copy(materialYou = true).widgetSettings())
        assertEquals(base.widgetSettings(), base.copy(themeMode = ninja.doskey.app.levana.domain.model.ThemeMode.DARK).widgetSettings())
    }
}
