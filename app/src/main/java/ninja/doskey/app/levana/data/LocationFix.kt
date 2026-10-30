package ninja.doskey.app.levana.data

import android.location.LocationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** A position from a location provider; [elapsedRealtimeNanos] is when it was taken. */
data class Fix(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val elapsedRealtimeNanos: Long
)

/** The platform location providers, behind an interface so [LocationFixer] is testable. */
interface LocationSource {
    fun providers(): List<String>
    fun isEnabled(provider: String): Boolean
    fun lastKnown(provider: String): Fix?
    fun elapsedRealtimeNanos(): Long

    /** A fresh fix from [provider], or null. Cancelling the caller cancels the request. */
    suspend fun current(provider: String): Fix?
}

/**
 * Finds the device position with the platform providers only (no Play services), so the
 * same APK works with and without Google Play services.
 */
class LocationFixer(
    private val source: LocationSource,
    private val timeoutMillis: Long = TIMEOUT_MILLIS,
    private val maxLastKnownAgeMillis: Long = MAX_LAST_KNOWN_AGE_MILLIS
) {

    /** A recent last-known fix, else a fresh one; null when none arrives within the timeout. */
    suspend fun fixOrNull(): Fix? = withTimeoutOrNull(timeoutMillis) {
        val providers = usableProviders()
        recentLastKnown(providers) ?: providers.firstNotNullOfOrNull { attempt { source.current(it) } }
    }

    /** Fused (Play services or the OEM's) when present, then network, then GPS; only enabled ones. */
    fun usableProviders(): List<String> {
        val present = attempt { source.providers() }.orEmpty()
        return PROVIDER_ORDER.filter { it in present && attempt { source.isEnabled(it) } == true }
    }

    private fun recentLastKnown(providers: List<String>): Fix? {
        val now = source.elapsedRealtimeNanos()
        return providers
            .mapNotNull { attempt { source.lastKnown(it) } }
            .filter { now - it.elapsedRealtimeNanos in 0..maxLastKnownAgeMillis * 1_000_000 }
            .maxByOrNull { it.elapsedRealtimeNanos }
    }

    // A provider that throws (e.g. SecurityException, a disabled provider) is skipped
    private inline fun <T> attempt(block: () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    companion object {
        const val TIMEOUT_MILLIS = 15_000L
        const val MAX_LAST_KNOWN_AGE_MILLIS = 2 * 60_000L
        val PROVIDER_ORDER = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER
        )
    }
}
