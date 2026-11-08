package ninja.doskey.app.levana.data

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.displayCity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * TASK-44: the current location's name is read in the app language, from the names stored with
 * the fix or the localized fallback, and follows a language switch made after it was stored.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CurrentLocationNameTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val localeChanges = MutableSharedFlow<Unit>()
    private val repository by lazy {
        PreferencesRepository(app, CityRepository(app), systemTimeChanges = MutableSharedFlow(), localeChanges = localeChanges)
    }

    private val jerusalemFix = Location(
        31.7683, 35.2137, 754.0, "Asia/Jerusalem", "Jerusalem",
        localizedNames = mapOf("en" to "Jerusalem", "he" to "ירושלים")
    )
    private val unnamedFix = Location(31.7683, 35.2137, 754.0, "Asia/Jerusalem", "")

    @Before
    fun setUp() = runBlocking { clearPreferences() }

    @After
    fun tearDown() = runBlocking {
        setAppLanguage(null)
        clearPreferences()
    }

    private fun setAppLanguage(tag: String?) {
        app.getSystemService(LocaleManager::class.java).applicationLocales =
            tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
    }

    private suspend fun name() = repository.preferences.first().currentLocation.name

    @Test
    fun englishUiShowsTheEnglishName() = runBlocking {
        repository.updateCurrentLocation(jerusalemFix)
        assertEquals("Jerusalem", name())
        assertEquals("Jerusalem", repository.preferences.first().displayCity.name)
    }

    @Test
    fun hebrewUiShowsTheHebrewName() = runBlocking {
        setAppLanguage("iw")
        repository.updateCurrentLocation(jerusalemFix)
        assertEquals("ירושלים", name())
    }

    @Test
    fun unnamedFixShowsTheLocalizedFallback() = runBlocking {
        repository.updateCurrentLocation(unnamedFix)
        assertEquals("Current Location", name())
        setAppLanguage("iw")
        assertEquals("מיקום נוכחי", name())
    }

    @Test
    fun switchingTheLanguageAfterTheFixRenamesItWithoutANewLookup() = runBlocking {
        withTimeout(10_000) { switchLanguage() }
    }

    private suspend fun switchLanguage() = coroutineScope {
        repository.updateCurrentLocation(jerusalemFix)
        val names = Channel<String>(Channel.UNLIMITED)
        val collector = launch { repository.preferences.collect { names.send(it.currentLocation.name) } }
        assertEquals("Jerusalem", names.receive())
        setAppLanguage("iw")
        localeChanges.subscriptionCount.first { it > 0 }
        localeChanges.emit(Unit)
        assertEquals("ירושלים", names.receive())
        collector.cancel()
    }

    @Test
    fun legacyStoredNamesAreReplacedWhenTheyDontFit() = runBlocking {
        storeRaw("""{"latitude":31.7683,"longitude":35.2137,"timezoneId":"Asia/Jerusalem","name":"Current Location"}""")
        setAppLanguage("iw")
        assertEquals("מיקום נוכחי", name())
        storeRaw("""{"latitude":31.7683,"longitude":35.2137,"timezoneId":"Asia/Jerusalem","name":"القدس"}""")
        setAppLanguage(null)
        assertEquals("Current Location", name())
    }

    private suspend fun storeRaw(json: String) {
        dataStore().edit { it[stringPreferencesKey("gps_location")] = json }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dataStore(): DataStore<Preferences> {
        val accessor = Class.forName("ninja.doskey.app.levana.data.PreferencesRepositoryKt")
            .getDeclaredMethod("getDataStore", Context::class.java)
            .apply { isAccessible = true }
        return accessor.invoke(null, app) as DataStore<Preferences>
    }

    private suspend fun clearPreferences() {
        dataStore().edit { it.clear() }
    }
}
