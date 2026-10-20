package ninja.doskey.app.levana.data

import android.app.Application
import android.os.Looper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [runCancellable] as seen by its caller: a cancelled caller sees cancellation
 * even when the worker ignores the signal and then fails; a live caller sees
 * the worker's error.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ProviderIoTest {

    private val worker = Executors.newSingleThreadExecutor { Thread(it, "provider-io") }
    private val io = worker.asCoroutineDispatcher()

    @After
    fun tearDown() = io.close()

    /** Runs a caller on [callerDispatcher] whose work blocks, then fails. */
    private fun outcome(callerDispatcher: CoroutineDispatcher, cancelCaller: Boolean): Throwable? {
        val release = CountDownLatch(1)
        val started = CountDownLatch(1)
        val seen = AtomicReference<Throwable?>()
        val scope = CoroutineScope(SupervisorJob() + callerDispatcher)
        val job = scope.launch {
            try {
                runCancellable(io) {
                    started.countDown()
                    release.await(5, TimeUnit.SECONDS) // ignores the signal
                    throw SecurityException("permission revoked")
                }
            } catch (t: Throwable) {
                seen.set(t)
            }
        }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        if (cancelCaller) job.cancel()
        release.countDown()
        val deadline = System.currentTimeMillis() + 5_000
        while (!job.isCompleted) {
            check(System.currentTimeMillis() < deadline) { "caller never completed" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        scope.cancel()
        return seen.get()
    }

    @Test
    fun cancelledMainImmediateCallerSeesCancellationNotTheLateError() {
        val seen = outcome(Dispatchers.Main.immediate, cancelCaller = true)
        assertTrue("saw $seen", seen is CancellationException)
    }

    @Test
    fun cancelledUnconfinedCallerSeesCancellationNotTheLateError() {
        val seen = outcome(Dispatchers.Unconfined, cancelCaller = true)
        assertTrue("saw $seen", seen is CancellationException)
    }

    @Test
    fun cancelledBackgroundCallerSeesCancellationNotTheLateError() {
        val seen = outcome(Dispatchers.IO, cancelCaller = true)
        assertTrue("saw $seen", seen is CancellationException)
    }

    @Test
    fun liveMainCallerStillSeesTheWorkersError() {
        val seen = outcome(Dispatchers.Main.immediate, cancelCaller = false)
        assertTrue("saw $seen", seen is SecurityException)
        assertEquals("permission revoked", seen?.message)
    }
}
