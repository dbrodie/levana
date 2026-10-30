package ninja.doskey.app.levana.ui.navigation

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.Location
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

/**
 * TASK-46: the notice for a location fix that doesn't fit the device's time zone shows once
 * per mismatch, and again only after the mismatch has cleared and come back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ZoneMismatchNoticeTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val savedZone = TimeZone.getDefault()
    private val timeChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val repository by lazy { PreferencesRepository(app, CityRepository(app), timeChanges) }
    private val ukiah = Location(39.1502, -123.2078, 190.0, "Asia/Jerusalem", "Ukiah")
    private val title get() = app.getString(R.string.zone_mismatch_title)

    private var shown by mutableStateOf(true)

    @Before
    fun setUp() {
        setDeviceZone("Asia/Jerusalem")
        runBlocking { clearPreferences() }
        startKoin {
            modules(module {
                single { repository }
                single {
                    DeviceContext(repository.preferences) {
                        Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneId.systemDefault())
                    }
                }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        runBlocking { clearPreferences() }
        TimeZone.setDefault(savedZone)
    }

    private fun setDeviceZone(zone: String) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
        timeChanges.tryEmit(Unit)
    }

    private fun showApp() = compose.setContent { if (shown) ZoneMismatchNotice() }

    private fun awaitDialog(visible: Boolean) = compose.waitUntil(5_000) {
        compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() == visible
    }

    @Test
    fun showsOnceForAMismatchAndAgainOnlyAfterItClears() {
        runBlocking { repository.updateCurrentLocation(ukiah) }
        showApp()
        awaitDialog(visible = true)
        assertNotNull(runBlocking { repository.getZoneMismatchNotice() })

        compose.onNodeWithText(app.getString(R.string.zone_mismatch_dismiss)).performClick()
        awaitDialog(visible = false)

        // Leaving and re-opening the app with the same mismatch shows nothing
        shown = false
        compose.waitForIdle()
        shown = true
        compose.waitForIdle()
        Thread.sleep(300)
        compose.waitForIdle()
        assertEquals(0, compose.onAllNodesWithText(title).fetchSemanticsNodes().size)

        // A matching zone clears it; a mismatch coming back shows it again
        setDeviceZone("America/Los_Angeles")
        compose.waitUntil(5_000) { runBlocking { repository.getZoneMismatchNotice() } == null }
        awaitDialog(visible = false)
        setDeviceZone("Asia/Jerusalem")
        awaitDialog(visible = true)
    }

    @Test
    fun theButtonOpensTheDateAndTimeSettings() {
        runBlocking { repository.updateCurrentLocation(ukiah) }
        showApp()
        awaitDialog(visible = true)

        compose.onNodeWithText(app.getString(R.string.zone_mismatch_open_settings)).performClick()

        awaitDialog(visible = false)
        assertEquals(Settings.ACTION_DATE_SETTINGS, shadowOf(app).nextStartedActivity.action)
    }

    @Test
    fun aFixThatFitsOrNoFixShowsNothing() {
        showApp()
        // No fix: the zone's fallback city
        Thread.sleep(300)
        compose.waitForIdle()
        assertEquals(0, compose.onAllNodesWithText(title).fetchSemanticsNodes().size)

        runBlocking { repository.updateCurrentLocation(Location.JERUSALEM) }
        Thread.sleep(300)
        compose.waitForIdle()
        assertEquals(0, compose.onAllNodesWithText(title).fetchSemanticsNodes().size)
        assertNull(runBlocking { repository.getZoneMismatchNotice() })
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun clearPreferences() {
        val getter = Class.forName("ninja.doskey.app.levana.data.PreferencesRepositoryKt")
            .getDeclaredMethod("getDataStore", Context::class.java)
            .apply { isAccessible = true }
        (getter.invoke(null, app) as DataStore<Preferences>).edit { it.clear() }
    }
}
