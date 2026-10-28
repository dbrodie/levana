package ninja.doskey.app.levana.data

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ninja.doskey.app.levana.domain.model.YahrzeitFirstYear
import ninja.doskey.app.levana.domain.model.YahrzeitLeapYearAdar
import ninja.doskey.app.levana.domain.model.YahrzeitMissingDay
import ninja.doskey.app.levana.domain.model.YahrzeitRules
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * The removed Minhag selection stored "minhag" in the preference store. Stores that still carry
 * it, with a former or unknown value, must open normally and keep every other setting.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LegacyMinhagPreferenceTest {

    private val app: Context get() = RuntimeEnvironment.getApplication()

    private val minhag = stringPreferencesKey("minhag")
    private val candleOffset = doublePreferencesKey("candle_lighting_offset")
    private val inIsrael = booleanPreferencesKey("is_in_israel")
    private val inIsraelManual = booleanPreferencesKey("is_in_israel_manual")
    private val leapYearAdar = stringPreferencesKey("yahrzeit_leap_year_adar")
    private val firstYear = stringPreferencesKey("yahrzeit_first_year")
    private val nisan1 = booleanPreferencesKey("yahrzeit_adar_i_30_nisan_1")
    private val missingDay = stringPreferencesKey("yahrzeit_missing_day")
    private val written = listOf(minhag, candleOffset, inIsrael, inIsraelManual, leapYearAdar, firstYear, nisan1, missingDay)

    /** The app's real preference store (one per JVM), which the repository has no raw access to. */
    @Suppress("UNCHECKED_CAST")
    private val store: DataStore<Preferences>
        get() = Class.forName("ninja.doskey.app.levana.data.PreferencesRepositoryKt")
            .getDeclaredMethod("getDataStore", Context::class.java)
            .apply { isAccessible = true }
            .invoke(null, app) as DataStore<Preferences>

    @After
    fun tearDown() = runBlocking {
        store.edit { prefs -> written.forEach { prefs.remove(it) } }
        Unit
    }

    @Test
    fun legacyMinhagValuesAreIgnoredAndOtherSettingsSurvive() = runBlocking {
        val repo = PreferencesRepository(app, CityRepository(app))
        val expectedRules = YahrzeitRules(
            YahrzeitLeapYearAdar.ADAR_II,
            YahrzeitFirstYear.ADAR_I,
            adarI30AlsoNisan1 = true,
            missingDay = YahrzeitMissingDay.ROSH_CHODESH
        )

        for (legacy in listOf("SEPHARDI", "CHABAD", "NOT_A_MINHAG", "")) {
            store.edit { prefs ->
                prefs[minhag] = legacy
                prefs[candleOffset] = 40.0
                prefs[inIsraelManual] = true
                prefs[inIsrael] = true
                prefs[leapYearAdar] = expectedRules.leapYearAdar.name
                prefs[firstYear] = expectedRules.firstYear.name
                prefs[nisan1] = expectedRules.adarI30AlsoNisan1
                prefs[missingDay] = expectedRules.missingDay.name
            }

            val loaded = repo.preferences.first()
            assertEquals(legacy, 40.0, loaded.candleLightingOffset, 0.0)
            assertTrue(legacy, loaded.isInIsrael)
            assertEquals(legacy, expectedRules, loaded.yahrzeitRules)

            // Saving through the repository rewrites the store but keeps the inert legacy key
            repo.saveCandleLightingOffset(30.0)
            val stored = store.data.first()
            assertEquals(legacy, stored[minhag])
            assertEquals(30.0, repo.preferences.first().candleLightingOffset, 0.0)
            assertEquals(expectedRules, repo.preferences.first().yahrzeitRules)
        }
    }
}
