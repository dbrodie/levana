package ninja.doskey.app.levana.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [resolvePlaceName] names a fix when the geocoder answers and falls back to
 * [FALLBACK_PLACE_NAME] otherwise; only caller cancellation escapes.
 */
class ResolvePlaceNameTest {

    private fun resolve(timeoutMillis: Long = 5_000L, geocoder: ReverseGeocoder) =
        runBlocking { resolvePlaceName(geocoder, 31.77, 35.21, timeoutMillis) }

    @Test
    fun `geocoded name is used`() {
        assertEquals("Jerusalem", resolve { _, _ -> "Jerusalem" })
    }

    @Test
    fun `missing result falls back`() {
        assertEquals(FALLBACK_PLACE_NAME, resolve { _, _ -> null })
    }

    @Test
    fun `blank result falls back`() {
        assertEquals(FALLBACK_PLACE_NAME, resolve { _, _ -> " " })
    }

    @Test
    fun `failure falls back`() {
        assertEquals(FALLBACK_PLACE_NAME, resolve { _, _ -> throw java.io.IOException("offline") })
    }

    @Test
    fun `hung geocoder times out to the fallback`() {
        assertEquals(FALLBACK_PLACE_NAME, resolve(timeoutMillis = 50L) { _, _ -> awaitCancellation() })
    }

    @Test
    fun `caller cancellation propagates`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val lookup = async {
            resolvePlaceName({ _, _ -> started.complete(Unit); awaitCancellation() }, 0.0, 0.0)
        }
        started.await()
        lookup.cancel()
        val thrown = runCatching { lookup.await() }.exceptionOrNull()
        assertTrue(thrown is CancellationException)
    }
}
