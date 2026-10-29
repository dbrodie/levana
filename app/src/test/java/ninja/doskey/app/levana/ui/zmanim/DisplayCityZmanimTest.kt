package ninja.doskey.app.levana.ui.zmanim

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.device.fixedClock
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.ui.otherClockCityName
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * TASK-5 case 9 and TASK-5.4: the selected city is chosen only in the sidebar and is the one
 * thing the Zmanim screen (and day panel) show, on the device's date and in the city's clock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DisplayCityZmanimTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val nyZone = ZoneId.of("America/New_York")
    private val newYork = Location(40.7128, -74.0060, 0.0, nyZone.id, "New York")
    // 23:30 Thursday in New York is already Friday in Jerusalem
    private val thursday = LocalDate.of(2026, 11, 12)
    private lateinit var preferences: PreferencesRepository
    private val store = ViewModelStore()

    @Before
    fun setUp() {
        preferences = PreferencesRepository(app, CityRepository(app))
        runBlocking {
            preferences.preferences.first().savedLocations.forEach { preferences.removeSavedLocation(it.id) }
            preferences.updateCurrentLocation(newYork)
            preferences.saveDevDayShift(0)
        }
    }

    @After
    fun tearDown() = runBlocking {
        store.clear()
        preferences.preferences.first().savedLocations.forEach { preferences.removeSavedLocation(it.id) }
        preferences.updateCurrentLocation(Location.JERUSALEM)
    }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (true) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))
            if (condition()) return
            assertTrue("timed out", System.currentTimeMillis() < deadline)
            Thread.sleep(10)
        }
    }

    private fun zmanimViewModel(): ZmanimViewModel {
        val device = DeviceContext(preferences.preferences) { fixedClock(thursday, LocalTime.of(23, 30), nyZone) }
        return ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V =
                ZmanimViewModel(ZmanimRepository(), preferences, device) as V
        })[ZmanimViewModel::class.java]
    }

    private fun selectJerusalem() = runBlocking {
        preferences.addSavedLocation(Location.JERUSALEM)
        preferences.setActiveLocation(preferences.preferences.first().savedLocations.single().id)
    }

    @Test
    fun addingACityDoesNotSelectIt() = runBlocking {
        preferences.addSavedLocation(Location.JERUSALEM)
        val prefs = preferences.preferences.first()
        assertEquals(1, prefs.savedLocations.size)
        assertNull(prefs.activeLocationId)
    }

    @Test
    fun removingTheSelectedCityFallsBackToTheCurrentLocation() = runBlocking {
        selectJerusalem()
        val id = preferences.preferences.first().activeLocationId!!
        preferences.removeSavedLocation(id)
        assertNull(preferences.preferences.first().activeLocationId)
    }

    @Test
    fun theZoneLabelAppearsOnlyWhenTheClocksDiffer() {
        assertEquals("Jerusalem", otherClockCityName(Location.JERUSALEM, nyZone))
        assertNull(otherClockCityName(newYork, nyZone))
        // An alias of the same zone is the same clock
        assertNull(otherClockCityName(Location.JERUSALEM.copy(timezoneId = "Asia/Tel_Aviv"), ZoneId.of("Asia/Jerusalem")))
    }

    @Test
    fun zmanimShowTheSelectedCityOnTheDeviceDateInItsOwnClock() {
        selectJerusalem()
        val vm = zmanimViewModel()
        vm.onIntent(ZmanimIntent.LoadDefault)
        awaitUntil { !vm.state.value.isLoading && vm.state.value.date != null }
        val state = vm.state.value
        // The device's date (Thursday), not Jerusalem's (already Friday)
        assertEquals(thursday, state.date)
        assertEquals("Jerusalem", state.locationName)
        assertEquals("Jerusalem", state.otherClockCityName)
        assertEquals(
            ZmanimRepository().getZmanim(thursday, Location.JERUSALEM, 18.0, false),
            state.zmanim
        )
    }

    @Test
    fun noLabelForTheCurrentLocation() {
        val vm = zmanimViewModel()
        vm.onIntent(ZmanimIntent.LoadDefault)
        awaitUntil { !vm.state.value.isLoading && vm.state.value.date != null }
        assertEquals("New York", vm.state.value.locationName)
        assertNull(vm.state.value.otherClockCityName)
    }

    @Test
    fun theLabelReadsInEnglish() {
        assertEquals("Jerusalem time", app.getString(R.string.zmanim_city_clock, "Jerusalem"))
    }

    @Test
    @Config(qualifiers = "iw")
    fun theLabelReadsInHebrew() {
        assertEquals("שעון ירושלים", app.getString(R.string.zmanim_city_clock, "ירושלים"))
    }
}
