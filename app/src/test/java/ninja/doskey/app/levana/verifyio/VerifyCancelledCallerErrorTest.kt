package ninja.doskey.app.levana.verifyio

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ninja.doskey.app.levana.data.SystemCalendarRepository
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** What a caller cancelled mid-query sees when the provider then fails. */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class VerifyCancelledCallerErrorTest {

    private val world = CalendarWorld()
    private lateinit var provider: ScriptedProvider

    @Before
    fun setUp() {
        grantAll()
        provider = installCalendar(world)
    }

    @Test
    fun aCancelledCallerSeesCancellationNotTheProvidersLaterError() {
        val hold = CountDownLatch(1)
        provider.responder = { _, _, _, _ ->
            hold.await() // ignores the cancellation signal, as a provider may
            throw SecurityException("permission revoked")
        }
        val outcome = AtomicReference<Throwable?>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch {
            try {
                SystemCalendarRepository(app()).getDeviceCalendars()
            } catch (t: Throwable) {
                outcome.set(t)
            }
        }
        awaitEntered(provider, 1)
        job.cancel()
        hold.countDown()
        idleUntil(what = "caller done") { job.isCompleted }
        println("VERIFY-CANCELLED-CALLER saw ${outcome.get()}")
        assertTrue("cancelled caller saw ${outcome.get()}", outcome.get() is CancellationException)
    }

    @Test
    fun onMainImmediateACancelledCallerSeesCancellation() {
        val hold = CountDownLatch(1)
        provider.responder = { _, _, _, _ ->
            hold.await() // ignores the cancellation signal, as a provider may
            throw SecurityException("permission revoked")
        }
        val outcome = AtomicReference<Throwable?>()
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                SystemCalendarRepository(app()).getDeviceCalendars()
            } catch (t: Throwable) {
                outcome.set(t)
            }
        }
        awaitEntered(provider, 1)
        job.cancel()
        hold.countDown()
        idleUntil(what = "caller done") { job.isCompleted }
        println("VERIFY-CANCELLED-CALLER-MAIN saw ${outcome.get()}")
        assertTrue("cancelled caller saw ${outcome.get()}", outcome.get() is CancellationException)
    }

    @Test
    fun onMainImmediateCancelledAfterAWhile() {
        val hold = CountDownLatch(1)
        provider.responder = { _, _, _, _ ->
            hold.await() // ignores the cancellation signal, as a provider may
            throw SecurityException("permission revoked")
        }
        val outcome = AtomicReference<Throwable?>()
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                SystemCalendarRepository(app()).getDeviceCalendars()
            } catch (t: Throwable) {
                outcome.set(t)
            }
        }
        awaitEntered(provider, 1)
        Thread.sleep(100)
        job.cancel()
        Thread.sleep(100)
        hold.countDown()
        idleUntil(what = "caller done") { job.isCompleted }
        println("VERIFY-CANCELLED-CALLER-MAIN-DELAY saw ${outcome.get()}")
        assertTrue("cancelled caller saw ${outcome.get()}", outcome.get() is CancellationException)
    }
}
