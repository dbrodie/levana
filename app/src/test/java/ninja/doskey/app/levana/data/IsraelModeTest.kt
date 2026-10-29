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

    @After
    fun tearDown() = runBlocking {
        // DataStore is shared by the whole JVM
        repository.saveIsraelMode(IsraelMode.AUTO)
        repository.preferences.first().savedLocations.forEach { repository.removeSavedLocation(it.id) }
        repository.updateCurrentLocation(Location.JERUSALEM)
    }

    @Test
    fun autoFollowsTheDevicePositionOnly() {
        assertTrue(resolveIsInIsrael(IsraelMode.AUTO, Location.JERUSALEM))
        assertFalse(resolveIsInIsrael(IsraelMode.AUTO, newYork))
        assertTrue(resolveIsInIsrael(IsraelMode.ISRAEL, newYork))
        assertFalse(resolveIsInIsrael(IsraelMode.DIASPORA, Location.JERUSALEM))
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

        // The device moving to Jerusalem is what makes it Israel
        repository.updateCurrentLocation(Location.JERUSALEM)
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
