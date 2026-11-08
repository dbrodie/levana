package ninja.doskey.app.levana.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Looks up candidate display names for coordinates in [locale], best first; empty when there are none. */
fun interface ReverseGeocoder {
    suspend fun placeNames(latitude: Double, longitude: Double, locale: Locale): List<String>
}

/**
 * Uses the asynchronous [Geocoder] API, so the lookup never blocks the calling
 * thread. A missing geocoder, empty result or error is an empty list.
 *
 * The nearest address can carry only another language's data (for Jerusalem it is
 * Arabic-only whatever the locale, TASK-44), so several addresses are read and all their
 * localities come before any district.
 */
class AndroidReverseGeocoder(private val context: Context) : ReverseGeocoder {

    override suspend fun placeNames(latitude: Double, longitude: Double, locale: Locale): List<String> {
        if (!Geocoder.isPresent()) return emptyList()
        return suspendCancellableCoroutine { cont ->
            Geocoder(context, locale).getFromLocation(
                latitude,
                longitude,
                MAX_RESULTS,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        cont.resume(
                            addresses.mapNotNull { it.locality } +
                                addresses.mapNotNull { it.subAdminArea } +
                                addresses.mapNotNull { it.adminArea }
                        )
                    }

                    override fun onError(errorMessage: String?) {
                        cont.resume(emptyList())
                    }
                }
            )
        }
    }

    private companion object {
        const val MAX_RESULTS = 5
    }
}

/** The app languages a fix is named in, keyed as in [ninja.doskey.app.levana.domain.model.Location.localizedNames]. */
enum class PlaceNameLanguage(val key: String, val locale: Locale) {
    ENGLISH("en", Locale.ENGLISH),
    HEBREW("he", Locale.forLanguageTag("iw"))
}

/**
 * The first of [candidates] written in a script that fits [language], or null. English takes
 * Latin; Hebrew takes Hebrew or Latin, as the city list is in English in the Hebrew UI too.
 * Digits, spaces and punctuation are ignored.
 */
fun pickPlaceName(candidates: List<String>, language: PlaceNameLanguage): String? {
    val scripts = when (language) {
        PlaceNameLanguage.ENGLISH -> setOf(Character.UnicodeScript.LATIN)
        PlaceNameLanguage.HEBREW -> setOf(Character.UnicodeScript.LATIN, Character.UnicodeScript.HEBREW)
    }
    return candidates.map { it.trim() }.firstOrNull { it.isNotEmpty() && fitsScripts(it, scripts) }
}

private fun fitsScripts(name: String, scripts: Set<Character.UnicodeScript>): Boolean {
    val letters = name.codePoints().filter { Character.isLetter(it) }.toArray()
    return letters.isNotEmpty() && letters.all { Character.UnicodeScript.of(it) in scripts }
}

/**
 * The fix's name in each [PlaceNameLanguage], looked up in parallel. A language is left out
 * when its lookup has no fitting name, fails or exceeds [timeoutMillis] (e.g. offline); the
 * name shown then falls back at read time. Caller cancellation is rethrown.
 */
suspend fun resolvePlaceNames(
    geocoder: ReverseGeocoder,
    latitude: Double,
    longitude: Double,
    timeoutMillis: Long = 5_000L
): Map<String, String> = coroutineScope {
    PlaceNameLanguage.entries
        .map { language ->
            async { language.key to resolvePlaceName(geocoder, latitude, longitude, language, timeoutMillis) }
        }
        .mapNotNull { lookup -> lookup.await().let { (key, name) -> name?.let { key to it } } }
        .toMap()
}

private suspend fun resolvePlaceName(
    geocoder: ReverseGeocoder,
    latitude: Double,
    longitude: Double,
    language: PlaceNameLanguage,
    timeoutMillis: Long
): String? = try {
    withTimeoutOrNull(timeoutMillis) { geocoder.placeNames(latitude, longitude, language.locale) }
        ?.let { pickPlaceName(it, language) }
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}

/**
 * The name for the current location in the app language: its looked-up name, or [fallback]
 * (the localized "Current Location") when there is none. A fix stored before TASK-44 has no
 * [localizedNames]; its single stored name is kept only if it fits the language.
 */
fun currentLocationName(
    localizedNames: Map<String, String>,
    storedName: String,
    hebrewUi: Boolean,
    fallback: String
): String {
    val language = if (hebrewUi) PlaceNameLanguage.HEBREW else PlaceNameLanguage.ENGLISH
    if (localizedNames.isEmpty()) {
        return storedName.takeIf { it != LEGACY_FALLBACK_PLACE_NAME }
            ?.let { pickPlaceName(listOf(it), language) }
            ?: fallback
    }
    return when (language) {
        PlaceNameLanguage.HEBREW -> localizedNames[PlaceNameLanguage.HEBREW.key] ?: localizedNames[PlaceNameLanguage.ENGLISH.key]
        PlaceNameLanguage.ENGLISH -> localizedNames[PlaceNameLanguage.ENGLISH.key]
    } ?: fallback
}

/** The English name stored for an unnamed fix before TASK-44; read only to replace it. */
internal const val LEGACY_FALLBACK_PLACE_NAME = "Current Location"
