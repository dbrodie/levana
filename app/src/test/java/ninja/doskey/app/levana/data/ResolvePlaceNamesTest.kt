package ninja.doskey.app.levana.data

import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TASK-44: a fix is named in English and Hebrew, each only in a script that fits the language;
 * a language without a name is left out (and falls back when read); only caller cancellation
 * escapes.
 */
class ResolvePlaceNamesTest {

    /**
     * Jerusalem as the emulator's geocoder answers it: the nearest address has only Arabic data
     * in every locale; the next ones are named in the requested language.
     */
    private val jerusalem = ReverseGeocoder { _, _, locale ->
        when (locale.language) {
            "iw", "he" -> listOf("القدس", "ירושלים", "ירושלים", "منطقة القدس", "מחוז ירושלים")
            else -> listOf("القدس", "Jerusalem", "Jerusalem", "منطقة القدس", "Jerusalem District")
        }
    }

    private fun resolve(timeoutMillis: Long = 5_000L, geocoder: ReverseGeocoder) =
        runBlocking { resolvePlaceNames(geocoder, 31.77, 35.21, timeoutMillis) }

    @Test
    fun `each language gets its own name, skipping an unrelated script`() {
        assertEquals(mapOf("en" to "Jerusalem", "he" to "ירושלים"), resolve(geocoder = jerusalem))
    }

    @Test
    fun `each language is looked up in its locale`() {
        val asked = mutableListOf<Locale>()
        resolve { _, _, locale -> synchronized(asked) { asked += locale }; emptyList() }
        // Hebrew is "iw" or "he" depending on the runtime
        assertEquals(setOf("en", "he"), asked.map { if (it.language == "iw") "he" else it.language }.toSet())
    }

    @Test
    fun `a Latin name serves the Hebrew UI when there is no Hebrew one`() {
        assertEquals(mapOf("en" to "Ukiah", "he" to "Ukiah"), resolve { _, _, _ -> listOf("Ukiah") })
    }

    @Test
    fun `only names in another script leave the language out`() {
        assertEquals(emptyMap<String, String>(), resolve { _, _, _ -> listOf("القدس", "Москва") })
    }

    @Test
    fun `missing or blank result leaves the language out`() {
        assertEquals(emptyMap<String, String>(), resolve { _, _, _ -> listOf(" ", "") })
        assertEquals(emptyMap<String, String>(), resolve { _, _, _ -> emptyList() })
    }

    @Test
    fun `offline failure leaves the language out`() {
        assertEquals(emptyMap<String, String>(), resolve { _, _, _ -> throw java.io.IOException("offline") })
    }

    @Test
    fun `one language failing keeps the other`() {
        val names = resolve { _, _, locale ->
            if (locale.language == "en") listOf("Ukiah") else throw java.io.IOException("offline")
        }
        assertEquals(mapOf("en" to "Ukiah"), names)
    }

    @Test
    fun `hung geocoder times out`() {
        assertEquals(emptyMap<String, String>(), resolve(timeoutMillis = 50L) { _, _, _ -> awaitCancellation() })
    }

    @Test
    fun `caller cancellation propagates`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val lookup = async {
            resolvePlaceNames({ _, _, _ -> started.complete(Unit); awaitCancellation() }, 0.0, 0.0)
        }
        started.await()
        lookup.cancel()
        val thrown = runCatching { lookup.await() }.exceptionOrNull()
        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `script check ignores digits and punctuation and accepts accents`() {
        assertEquals("São Paulo", pickPlaceName(listOf("São Paulo"), PlaceNameLanguage.ENGLISH))
        assertEquals("Tel Aviv-Yafo", pickPlaceName(listOf("Tel Aviv-Yafo"), PlaceNameLanguage.ENGLISH))
        assertEquals("תל אביב-יפו", pickPlaceName(listOf("תל אביב-יפו"), PlaceNameLanguage.HEBREW))
        assertNull(pickPlaceName(listOf("תל אביב-יפו", "123"), PlaceNameLanguage.ENGLISH))
    }

    private val fallbackEn = "Current Location"
    private val fallbackHe = "מיקום נוכחי"
    private val both = mapOf("en" to "Jerusalem", "he" to "ירושלים")

    @Test
    fun `shown name follows the app language`() {
        assertEquals("Jerusalem", currentLocationName(both, "Jerusalem", hebrewUi = false, fallback = fallbackEn))
        assertEquals("ירושלים", currentLocationName(both, "Jerusalem", hebrewUi = true, fallback = fallbackHe))
    }

    @Test
    fun `Hebrew UI falls back to the English name, English UI to the fallback`() {
        assertEquals("Ukiah", currentLocationName(mapOf("en" to "Ukiah"), "Ukiah", hebrewUi = true, fallback = fallbackHe))
        assertEquals(fallbackEn, currentLocationName(mapOf("he" to "ירושלים"), "", hebrewUi = false, fallback = fallbackEn))
    }

    @Test
    fun `unnamed fix shows the localized fallback`() {
        assertEquals(fallbackEn, currentLocationName(emptyMap(), "", hebrewUi = false, fallback = fallbackEn))
        assertEquals(fallbackHe, currentLocationName(emptyMap(), "", hebrewUi = true, fallback = fallbackHe))
    }

    @Test
    fun `fix stored before TASK-44 keeps a fitting name and replaces the rest`() {
        assertEquals("Ukiah", currentLocationName(emptyMap(), "Ukiah", hebrewUi = false, fallback = fallbackEn))
        assertEquals(fallbackEn, currentLocationName(emptyMap(), "القدس", hebrewUi = false, fallback = fallbackEn))
        assertEquals(fallbackHe, currentLocationName(emptyMap(), "القدس", hebrewUi = true, fallback = fallbackHe))
        assertEquals(fallbackHe, currentLocationName(emptyMap(), "Current Location", hebrewUi = true, fallback = fallbackHe))
    }
}
