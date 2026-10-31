package ninja.doskey.app.levana.ui.navigation

import android.app.Application
import java.time.LocalDate
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.deviceContextOn
import ninja.doskey.app.levana.ui.navigation.ShellViewModel.Companion.LOCATION_REFRESH_INTERVAL_MS
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** When a resume refreshes the current location (TASK-52.3): not for a fold, rotation or resize. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ShellViewModelTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val shell = ShellViewModel(PreferencesRepository(app, CityRepository(app)), deviceContextOn(LocalDate.of(2026, 10, 30)))
    private val later = LOCATION_REFRESH_INTERVAL_MS

    @Test
    fun `opening the app refreshes`() {
        assertTrue(shell.shouldRefreshLocation(0))
    }

    @Test
    fun `a configuration change does not, however long it has been`() {
        shell.shouldRefreshLocation(0)
        repeat(3) { i ->
            shell.onStop(changingConfigurations = true)
            assertFalse(shell.shouldRefreshLocation(later * (i + 2)))
        }
    }

    @Test
    fun `returning to the app refreshes, but not twice within the interval`() {
        shell.shouldRefreshLocation(0)
        // The "Swipe up to continue" screen of a fold stops the activity before recreating it
        shell.onStop(changingConfigurations = false)
        assertFalse(shell.shouldRefreshLocation(later - 1))
        shell.onStop(changingConfigurations = false)
        assertTrue(shell.shouldRefreshLocation(later * 2))
    }
}
