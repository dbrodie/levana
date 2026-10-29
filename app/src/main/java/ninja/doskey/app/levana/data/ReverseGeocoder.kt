package ninja.doskey.app.levana.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Looks up a display name for coordinates; null when there is none. */
fun interface ReverseGeocoder {
    suspend fun placeName(latitude: Double, longitude: Double): String?
}

/**
 * Uses the asynchronous [Geocoder] API, so the lookup never blocks the calling
 * thread. A missing geocoder, empty result or error is null.
 */
class AndroidReverseGeocoder(private val context: Context) : ReverseGeocoder {

    override suspend fun placeName(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        return suspendCancellableCoroutine { cont ->
            Geocoder(context, Locale.getDefault()).getFromLocation(
                latitude,
                longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val addr = addresses.firstOrNull()
                        cont.resume(addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea)
                    }

                    override fun onError(errorMessage: String?) {
                        cont.resume(null)
                    }
                }
            )
        }
    }
}

const val FALLBACK_PLACE_NAME = "Current Location"

/**
 * The geocoded name for the coordinates, or [FALLBACK_PLACE_NAME] when the
 * lookup has no result, fails or exceeds [timeoutMillis]. Caller cancellation
 * is rethrown.
 */
suspend fun resolvePlaceName(
    geocoder: ReverseGeocoder,
    latitude: Double,
    longitude: Double,
    timeoutMillis: Long = 5_000L
): String = try {
    withTimeoutOrNull(timeoutMillis) { geocoder.placeName(latitude, longitude) }
        ?.takeIf { it.isNotBlank() }
        ?: FALLBACK_PLACE_NAME
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    FALLBACK_PLACE_NAME
}
