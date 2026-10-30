package ninja.doskey.app.levana.data

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.displayCity
import ninja.doskey.app.levana.widget.TodayWidgetSnapshots
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * TASK-46: a stored location fix is always in the device's current time zone, whatever zone it
 * was stored with (a fix stamped in another zone, a zone change since the fix, or a stale fix
 * already stored by an older version).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CurrentLocationZoneTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val repository by lazy { PreferencesRepository(app, CityRepository(app)) }
    private val savedZone = TimeZone.getDefault()

    private val la = ZoneId.of("America/Los_Angeles")
    private val kiritimati = ZoneId.of("Pacific/Kiritimati")

    /** The emulator fix of the TASK-48 walkthrough: Ukiah, stamped in Jerusalem's zone. */
    private val ukiahInJerusalem = Location(39.1502, -123.2078, 190.0, "Asia/Jerusalem", "Ukiah")

    /** Friday 30 Oct 2026 19:52 in Kiritimati; Thursday 22:52 in California. */
    private val evening: Instant = Instant.parse("2026-10-30T05:52:00Z")

    @Before
    fun setUp() = runBlocking {
        setDeviceZone(la)
        clearPreferences()
    }

    @After
    fun tearDown() = runBlocking {
        clearPreferences()
        TimeZone.setDefault(savedZone)
    }

    private fun setDeviceZone(zone: ZoneId) = TimeZone.setDefault(TimeZone.getTimeZone(zone))

    private suspend fun prefs() = repository.preferences.first()

    private fun device() = DeviceContext(repository.preferences) {
        Clock.fixed(evening, ZoneId.systemDefault())
    }

    @Test
    fun aFixStoredInAnotherZoneIsReadInTheDeviceZone() = runBlocking {
        repository.updateCurrentLocation(ukiahInJerusalem)

        val prefs = prefs()
        assertTrue(prefs.currentLocationIsFix)
        assertEquals(ukiahInJerusalem.copy(timezoneId = la.id), prefs.currentLocation)
        // With no city selected, the day panel and Zmanim screen show the position on the device clock
        assertEquals(la.id, prefs.displayCity.timezoneId)
        assertEquals(la.id, device().now().position.timezoneId)

        // Ukiah's sunrise on 30 Oct is 7:38 AM on the California clock, not 4:38 PM (Jerusalem)
        val sunrise = ZmanimRepository().getZmanim(LocalDate.of(2026, 10, 30), prefs.displayCity)
            .single { it.name == "Sunrise" }.time!!
        assertEquals(7, sunrise.hour)
    }

    @Test
    fun aZoneChangeReZonesTheSameFixWithoutANewOne() = runBlocking {
        repository.updateCurrentLocation(ukiahInJerusalem.copy(timezoneId = la.id))
        assertEquals(la.id, prefs().currentLocation.timezoneId)

        setDeviceZone(kiritimati)

        val prefs = prefs()
        assertEquals(kiritimati.id, prefs.currentLocation.timezoneId)
        assertEquals(ukiahInJerusalem.latitude, prefs.currentLocation.latitude, 0.0)
        assertEquals(kiritimati.id, prefs.displayCity.timezoneId)
        assertEquals(kiritimati.id, device().now().position.timezoneId)
    }

    @Test
    fun theTodayWidgetFollowsAZoneChangeToKiritimati() = runBlocking {
        // TASK-9.2/9.3 evidence: the widget showed "Sunrise 7:38 AM" on the old California clock
        repository.updateCurrentLocation(ukiahInJerusalem.copy(timezoneId = la.id))
        setDeviceZone(kiritimati)

        val now = device().now()
        val snapshot = TodayWidgetSnapshots(ZmanimRepository(), CalendarRepository()).build(prefs(), now)

        assertEquals(LocalDate.of(2026, 10, 30), snapshot.today)
        val next = snapshot.nextZman!!
        assertEquals("Sunrise", next.name)
        assertEquals(kiritimati, next.at.zone)
        // Friday's 7:38 AM sunrise in California is Saturday 4:38 AM on the device clock
        assertEquals(LocalDate.of(2026, 10, 31), next.at.toLocalDate())
        assertEquals(4, next.at.hour)
        val candles = snapshot.shabbat!!.candleLighting!!
        assertEquals(kiritimati, candles.zone)
        assertEquals(LocalDate.of(2026, 10, 30), candles.toLocalDate())
        assertEquals(14, candles.hour)
    }

    @Test
    fun theFallbackCityIsNotAFixAndKeepsItsOwnZone() = runBlocking {
        val prefs = prefs()
        assertFalse(prefs.currentLocationIsFix)
        assertEquals(la.id, prefs.currentLocation.timezoneId)

        // No bundled city is in Kiritimati's zone; the closest offset's city keeps its own zone
        setDeviceZone(kiritimati)
        val fallback = prefs().currentLocation
        assertFalse(prefs().currentLocationIsFix)
        assertEquals(
            CityRepository(app).defaultForTimeZone(kiritimati, Instant.now()).timezoneId,
            fallback.timezoneId
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun clearPreferences() {
        val getter = Class.forName("ninja.doskey.app.levana.data.PreferencesRepositoryKt")
            .getDeclaredMethod("getDataStore", Context::class.java)
            .apply { isAccessible = true }
        (getter.invoke(null, app) as DataStore<Preferences>).edit { it.clear() }
    }
}
