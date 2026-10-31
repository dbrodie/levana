package ninja.doskey.app.levana.data

import android.location.LocationManager.FUSED_PROVIDER
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
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
 * [LocationFixer] picks fused → network → GPS among enabled providers, uses a recent
 * last-known fix, falls through providers that fail and gives up at the timeout.
 */
class LocationFixerTest {

    private val now = 1_000_000_000_000L

    private fun fix(lat: Double, ageMillis: Long = 0) =
        Fix(lat, 35.0, 800.0, now - ageMillis * 1_000_000)

    private class FakeSource(
        val present: List<String> = listOf(GPS_PROVIDER, NETWORK_PROVIDER, FUSED_PROVIDER),
        val disabled: Set<String> = emptySet(),
        val lastKnown: Map<String, Fix> = emptyMap(),
        val now: Long,
        val current: suspend (String) -> Fix? = { null }
    ) : LocationSource {
        val asked = mutableListOf<String>()
        override fun providers() = present
        override fun isEnabled(provider: String) = provider !in disabled
        override fun lastKnown(provider: String) = lastKnown[provider]
        override fun elapsedRealtimeNanos() = now
        override suspend fun current(provider: String): Fix? {
            asked += provider
            return current.invoke(provider)
        }
    }

    private fun fixer(source: FakeSource, timeoutMillis: Long = 5_000L) =
        LocationFixer(source, timeoutMillis)

    @Test
    fun `providers are ordered fused, network, gps`() {
        assertEquals(
            listOf(FUSED_PROVIDER, NETWORK_PROVIDER, GPS_PROVIDER),
            fixer(FakeSource(now = now)).usableProviders()
        )
    }

    @Test
    fun `missing or disabled providers are skipped`() {
        val noFused = FakeSource(present = listOf(GPS_PROVIDER, NETWORK_PROVIDER, "passive"), now = now)
        assertEquals(listOf(NETWORK_PROVIDER, GPS_PROVIDER), fixer(noFused).usableProviders())
        val networkOff = FakeSource(disabled = setOf(NETWORK_PROVIDER, FUSED_PROVIDER), now = now)
        assertEquals(listOf(GPS_PROVIDER), fixer(networkOff).usableProviders())
    }

    @Test
    fun `location off gives no fix without asking`() = runBlocking {
        val source = FakeSource(disabled = setOf(FUSED_PROVIDER, NETWORK_PROVIDER, GPS_PROVIDER), now = now) {
            fix(1.0)
        }
        assertNull(fixer(source).fixOrNull())
        assertTrue(source.asked.isEmpty())
    }

    @Test
    fun `fresh fix comes from the first provider`() = runBlocking {
        val source = FakeSource(now = now) { fix(if (it == FUSED_PROVIDER) 1.0 else 2.0) }
        assertEquals(1.0, fixer(source).fixOrNull()!!.latitude, 0.0)
        assertEquals(listOf(FUSED_PROVIDER), source.asked)
    }

    @Test
    fun `recent last known fix is used without a request, newest first`() = runBlocking {
        val source = FakeSource(
            lastKnown = mapOf(GPS_PROVIDER to fix(3.0, ageMillis = 10_000), NETWORK_PROVIDER to fix(2.0, ageMillis = 60_000)),
            now = now
        ) { fix(1.0) }
        assertEquals(3.0, fixer(source).fixOrNull()!!.latitude, 0.0)
        assertTrue(source.asked.isEmpty())
    }

    @Test
    fun `stale last known fix is ignored`() = runBlocking {
        val source = FakeSource(lastKnown = mapOf(FUSED_PROVIDER to fix(3.0, ageMillis = 10 * 60_000)), now = now) {
            fix(1.0)
        }
        assertEquals(1.0, fixer(source).fixOrNull()!!.latitude, 0.0)
    }

    @Test
    fun `last known fix of a disabled provider is ignored`() = runBlocking {
        val source = FakeSource(
            disabled = setOf(GPS_PROVIDER),
            lastKnown = mapOf(GPS_PROVIDER to fix(3.0)),
            now = now
        ) { fix(1.0) }
        assertEquals(1.0, fixer(source).fixOrNull()!!.latitude, 0.0)
    }

    @Test
    fun `null or failing provider falls through to the next`() = runBlocking {
        val source = FakeSource(now = now) {
            when (it) {
                FUSED_PROVIDER -> null
                NETWORK_PROVIDER -> throw SecurityException("no permission")
                else -> fix(9.0)
            }
        }
        assertEquals(9.0, fixer(source).fixOrNull()!!.latitude, 0.0)
        assertEquals(listOf(FUSED_PROVIDER, NETWORK_PROVIDER, GPS_PROVIDER), source.asked)
    }

    @Test
    fun `no provider gives a fix`() = runBlocking {
        assertNull(fixer(FakeSource(now = now)).fixOrNull())
    }

    @Test
    fun `throwing provider list gives no fix`() = runBlocking {
        val source = object : LocationSource {
            override fun providers(): List<String> = throw IllegalStateException()
            override fun isEnabled(provider: String) = true
            override fun lastKnown(provider: String): Fix? = null
            override fun elapsedRealtimeNanos() = now
            override suspend fun current(provider: String): Fix? = fix(1.0)
        }
        assertNull(LocationFixer(source).fixOrNull())
    }

    @Test
    fun `hung provider times out to null`() = runBlocking {
        val cancelled = CompletableDeferred<Unit>()
        val source = FakeSource(now = now) {
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        assertNull(fixer(source, timeoutMillis = 50L).fixOrNull())
        assertTrue(cancelled.isCompleted)
        assertEquals(listOf(FUSED_PROVIDER), source.asked)
    }

    @Test
    fun `caller cancellation propagates and cancels the request`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val source = FakeSource(now = now) {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val request = async { fixer(source).fixOrNull() }
        started.await()
        request.cancel()
        val thrown = runCatching { request.await() }.exceptionOrNull()
        assertTrue(thrown is CancellationException)
        assertTrue(cancelled.isCompleted)
    }
}
