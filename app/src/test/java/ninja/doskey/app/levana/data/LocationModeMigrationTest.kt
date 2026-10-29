package ninja.doskey.app.levana.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.displayCity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LocationModeMigrationTest {

    private val locationMode = stringPreferencesKey("location_mode")
    private val activeLocationId = stringPreferencesKey("active_location_id")

    private fun migrate(prefs: Preferences) = runBlocking { LocationModeMigration.migrate(prefs) }

    @Test
    fun savedModeBecomesTheActiveId() {
        val migrated = migrate(preferencesOf(locationMode to """{"type":"saved","id":"abc"}"""))
        assertEquals("abc", migrated[activeLocationId])
        assertFalse(locationMode in migrated)
    }

    @Test
    fun gpsModeAndGarbageMeanTheCurrentLocation() {
        for (stored in listOf("""{"type":"gps"}""", "not json")) {
            val migrated = migrate(preferencesOf(locationMode to stored))
            assertNull(migrated[activeLocationId])
            assertFalse(locationMode in migrated)
        }
    }

    @Test
    fun savedLocationsAndSettingsSurviveTheMigration() {
        val savedLocations = stringPreferencesKey("saved_locations")
        val minhag = stringPreferencesKey("minhag")
        val stored = """[{"id":"abc","location":{"name":"Tel Aviv"}}]"""
        val migrated = migrate(
            preferencesOf(
                locationMode to """{"type":"saved","id":"abc"}""",
                savedLocations to stored,
                minhag to "SEPHARDI"
            )
        )
        assertEquals(stored, migrated[savedLocations])
        assertEquals("SEPHARDI", migrated[minhag])
    }

    @Test
    fun onlyRunsWhenTheOldKeyExists() = runBlocking {
        assertFalse(LocationModeMigration.shouldMigrate(preferencesOf()))
    }

    @Test
    fun displayCityFallsBackToTheCurrentLocation() {
        val telAviv = SavedLocation("ta", Location.JERUSALEM.copy(name = "Tel Aviv"))
        val prefs = UserPreferences(savedLocations = listOf(telAviv))
        assertEquals(Location.JERUSALEM, prefs.displayCity)
        assertEquals(telAviv.location, prefs.copy(activeLocationId = "ta").displayCity)
        assertEquals(Location.JERUSALEM, prefs.copy(activeLocationId = "deleted").displayCity)
    }
}
