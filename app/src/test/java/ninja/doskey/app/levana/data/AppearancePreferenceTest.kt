package ninja.doskey.app.levana.data

import android.app.Application
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.AppDataStores
import ninja.doskey.app.levana.domain.model.ThemeMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Settings → Theme and Wallpaper colors persist in the preferences DataStore. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppearancePreferenceTest {

    private lateinit var preferences: PreferencesRepository

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        AppDataStores.clearPreferences(app)
        preferences = PreferencesRepository(app, CityRepository(app))
    }

    @After
    fun tearDown() = AppDataStores.clearPreferences(RuntimeEnvironment.getApplication())

    @Test
    fun `defaults follow the system without wallpaper colors`() {
        assertEquals(ThemeMode.SYSTEM, parseThemeMode(null))
        val prefs = runBlocking { preferences.preferences.first() }
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        assertFalse(prefs.materialYou)
    }

    @Test
    fun `an unrecognised stored mode follows the system`() {
        assertEquals(ThemeMode.SYSTEM, parseThemeMode("SEPIA"))
    }

    @Test
    fun `every theme mode and the wallpaper switch round trip`() = runBlocking {
        for (mode in ThemeMode.entries) {
            preferences.saveThemeMode(mode)
            assertEquals(mode, preferences.preferences.first().themeMode)
        }
        preferences.saveMaterialYou(true)
        assertTrue(preferences.preferences.first().materialYou)
        preferences.saveMaterialYou(false)
        assertFalse(preferences.preferences.first().materialYou)
    }
}
