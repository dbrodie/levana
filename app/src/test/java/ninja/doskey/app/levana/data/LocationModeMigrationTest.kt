package ninja.doskey.app.levana.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.activeLocation
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
    fun onlyRunsWhenTheOldKeyExists() = runBlocking {
        assertFalse(LocationModeMigration.shouldMigrate(preferencesOf()))
    }

    @Test
    fun activeLocationFallsBackToTheCurrentLocation() {
        val telAviv = SavedLocation("ta", Location.JERUSALEM.copy(name = "Tel Aviv"))
        val prefs = UserPreferences(savedLocations = listOf(telAviv))
        assertEquals(Location.JERUSALEM, prefs.activeLocation)
        assertEquals(telAviv.location, prefs.copy(activeLocationId = "ta").activeLocation)
        assertEquals(Location.JERUSALEM, prefs.copy(activeLocationId = "deleted").activeLocation)
    }
}
