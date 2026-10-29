package ninja.doskey.app.levana.data

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.device.resolveIsInIsrael
import ninja.doskey.app.levana.domain.model.IsraelMode
import ninja.doskey.app.levana.domain.model.Location
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Before
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** TASK-5 case 6: Auto-detect (default) / Israel / Diaspora; Auto follows the device. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class IsraelModeTest {

    private val newYork = Location(40.7128, -74.0060, 0.0, "America/New_York", "New York")
    private val inIsrael = booleanPreferencesKey("is_in_israel")
    private val manual = booleanPreferencesKey("is_in_israel_manual")
    private val mode = stringPreferencesKey("israel_mode")

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val repository by lazy { PreferencesRepository(app, CityRepository(app)) }

    private val savedZone = TimeZone.getDefault()

    // Auto-detect reads the device zone; start every test in New York
    @Before
    fun setUp() = TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))

    @After
    fun tearDown() = runBlocking {
        TimeZone.setDefault(savedZone)
        // DataStore is shared by the whole JVM
        repository.saveIsraelMode(IsraelMode.AUTO)
        repository.preferences.first().savedLocations.forEach { repository.removeSavedLocation(it.id) }
        repository.updateCurrentLocation(Location.JERUSALEM)
    }

    @Test
    fun autoFollowsTheDeviceZoneOnly() {
        for (israel in listOf("Asia/Jerusalem", "Asia/Tel_Aviv", "Israel")) {
            assertTrue(israel, resolveIsInIsrael(IsraelMode.AUTO, ZoneId.of(israel)))
        }
        for (zone in listOf("America/New_York", "Europe/Dublin", "Australia/Perth", "Asia/Dubai", "Asia/Amman")) {
            assertFalse(zone, resolveIsInIsrael(IsraelMode.AUTO, ZoneId.of(zone)))
        }
        assertTrue(resolveIsInIsrael(IsraelMode.ISRAEL, ZoneId.of("America/New_York")))
        assertFalse(resolveIsInIsrael(IsraelMode.DIASPORA, ZoneId.of("Asia/Jerusalem")))
    }

    // Without a location fix the position is a bundled stand-in city; it must be a sensible
    // one and must never make the device Israeli
    @Test
    fun zonesWithoutABundledCityGetTheClosestOffsetAndStayDiaspora() {
        val cities = CityRepository(app)
        val now = java.time.Instant.parse("2026-10-29T12:00:00Z")
        fun fallback(zone: String) = cities.defaultForTimeZone(ZoneId.of(zone), now)
        assertEquals("London", fallback("Europe/Dublin").name)
        assertEquals("Hong Kong", fallback("Australia/Perth").name)
        assertEquals("Moscow", fallback("Asia/Dubai").name)
        // A zone shared with Jerusalem's offset prefers a city outside Israel
        assertFalse(fallback("Africa/Cairo").timezoneId == "Asia/Jerusalem")
        // Aliases of a bundled zone use that zone's city
        assertEquals("Europe/Kyiv", fallback("Europe/Kiev").timezoneId)
        assertEquals("Asia/Jerusalem", fallback("Asia/Tel_Aviv").timezoneId)
        assertEquals("New York", fallback("America/New_York").name)
        for (zone in listOf("Europe/Dublin", "Australia/Perth", "Asia/Dubai")) {
            assertFalse(zone, resolveIsInIsrael(IsraelMode.AUTO, ZoneId.of(zone)))
        }
    }

    @Test
    fun aNewYorkDeviceWithJerusalemSelectedIsDiasporaUnderAuto() = runBlocking {
        repository.saveIsraelMode(IsraelMode.AUTO)
        repository.updateCurrentLocation(newYork)
        repository.addSavedLocation(Location.JERUSALEM)
        val id = repository.preferences.first().savedLocations.single().id
        repository.setActiveLocation(id)
        val prefs = repository.preferences.first()
        assertEquals(IsraelMode.AUTO, prefs.israelMode)
        assertFalse(prefs.isInIsrael)

        // A Jerusalem position alone does not (the zone decides); the device zone does
        repository.updateCurrentLocation(Location.JERUSALEM)
        assertFalse(repository.preferences.first().isInIsrael)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jerusalem"))
        assertTrue(repository.preferences.first().isInIsrael)
    }

    @Test
    fun theChoicePersists() = runBlocking {
        repository.updateCurrentLocation(newYork)
        repository.saveIsraelMode(IsraelMode.ISRAEL)
        assertEquals(IsraelMode.ISRAEL, repository.preferences.first().israelMode)
        assertTrue(repository.preferences.first().isInIsrael)
        repository.saveIsraelMode(IsraelMode.DIASPORA)
        assertFalse(repository.preferences.first().isInIsrael)
        repository.saveIsraelMode(IsraelMode.AUTO)
        assertEquals(IsraelMode.AUTO, repository.preferences.first().israelMode)
        assertFalse(repository.preferences.first().isInIsrael)
    }

    @Test
    fun manualChoicesMigrateAsTheyWere() = runBlocking {
        val israel = IsraelModeMigration.migrate(mutablePreferencesOf(inIsrael to true, manual to true))
        assertEquals("ISRAEL", israel[mode])
        val diaspora = IsraelModeMigration.migrate(mutablePreferencesOf(inIsrael to false, manual to true))
        assertEquals("DIASPORA", diaspora[mode])
        for (migrated in listOf(israel, diaspora)) {
            assertNull(migrated[inIsrael])
            assertNull(migrated[manual])
            assertFalse(IsraelModeMigration.shouldMigrate(migrated))
        }
    }

    @Test
    fun untouchedUsersGetAutoDetect() = runBlocking {
        // An automatic value was stored, but never chosen by the user
        val automatic = IsraelModeMigration.migrate(mutablePreferencesOf(inIsrael to true, manual to false))
        assertNull(automatic[mode])
        assertNull(automatic[inIsrael])
        val onlyValue = IsraelModeMigration.migrate(mutablePreferencesOf(inIsrael to true))
        assertNull(onlyValue[mode])
        assertFalse(IsraelModeMigration.shouldMigrate(mutablePreferencesOf()))
    }
}
