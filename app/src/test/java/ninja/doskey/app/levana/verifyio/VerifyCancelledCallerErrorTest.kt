package ninja.doskey.app.levana.verifyio

import android.net.Uri
import android.os.OperationCanceledException
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventFileRepository
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.device.deviceContextOn
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayIntent
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayViewModel
import ninja.doskey.app.levana.ui.events.EventsIntent
import ninja.doskey.app.levana.ui.events.EventsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Required: a superseded/cancelled caller never sees a late provider error
 * (and state is not changed by it); an active caller still gets the real error.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class VerifyCancelledCallerErrorTest {

    private val world = CalendarWorld()
    private val contacts = ContactsWorld()
    private lateinit var calendarProvider: ScriptedProvider
    private lateinit var contactsProvider: ScriptedProvider

    @Before
    fun setUp() {
        grantAll()
        calendarProvider = installCalendar(world)
        contactsProvider = installContacts(contacts)
    }

    private val contexts: List<Pair<String, CoroutineContext>> = listOf(
        "Main.immediate" to Dispatchers.Main.immediate,
        "Main" to Dispatchers.Main,
        "Unconfined" to Dispatchers.Unconfined,
        "Default" to Dispatchers.Default,
        "IO" to Dispatchers.IO
    )

    /** Calls getDeviceCalendars from [ctx]; the provider waits on a latch then fails with [late]. */
    private fun outcome(ctx: CoroutineContext, cancel: Boolean, late: () -> Throwable?, wrap: suspend (suspend () -> Unit) -> Unit = { it() }): Throwable? {
        val hold = CountDownLatch(1)
        calendarProvider.entered.set(0)
        calendarProvider.responder = { uri, proj, sel, args ->
            hold.await(5, TimeUnit.SECONDS) // ignores the signal
            late()?.let { throw it }
            world.respond(uri, proj, sel, args)
        }
        val seen = AtomicReference<Throwable?>()
        val job = CoroutineScope(ctx).launch {
            wrap {
                try {
                    SystemCalendarRepository(app()).getDeviceCalendars()
                } catch (t: Throwable) {
                    seen.set(t)
                }
            }
        }
        awaitEntered(calendarProvider, 1)
        if (cancel) {
            job.cancel()
            Thread.sleep(30)
            shadowOf(android.os.Looper.getMainLooper()).idle()
        }
        hold.countDown()
        idleUntil(what = "caller done") { job.isCompleted }
        return seen.get()
    }

    @Test
    fun cancelledCallersSeeCancellationForEveryLateFailureInEveryContext() {
        val lates = listOf<Pair<String, () -> Throwable?>>(
            "SecurityException" to { SecurityException("revoked") },
            "IllegalStateException" to { IllegalStateException("provider died") },
            "OperationCanceledException" to { OperationCanceledException() },
            "late success" to { null }
        )
        val bad = mutableListOf<String>()
        for ((cName, ctx) in contexts) for ((lName, late) in lates) {
            val t = outcome(ctx, cancel = true, late = late)
            println("VERIFY-CTX cancelled $cName/$lName -> $t")
            if (t !is CancellationException) bad += "$cName/$lName -> $t"
        }
        assertTrue("cancelled callers saw: $bad", bad.isEmpty())
    }

    @Test
    fun activeCallersStillGetTheRealProviderErrorInEveryContext() {
        val bad = mutableListOf<String>()
        for ((cName, ctx) in contexts) {
            val t = outcome(ctx, cancel = false, late = { SecurityException("real") })
            println("VERIFY-CTX active $cName -> $t")
            if (!(t is SecurityException && t.message == "real")) bad += "$cName -> $t"
        }
        assertTrue("active callers saw: $bad", bad.isEmpty())
    }

    @Test
    fun activeRunBlockingCallerOnMainGetsTheRealError() {
        calendarProvider.failure = { SecurityException("real") }
        val t = runCatching { runBlocking { SystemCalendarRepository(app()).getDeviceCalendars() } }.exceptionOrNull()
        assertTrue("got $t", t is SecurityException && t.message == "real")
    }

    @Test
    fun aNonCancellableSectionStillReceivesTheRealErrorAfterOuterCancel() {
        // Work that opted out of cancellation is still "active": the real error is correct
        val t = outcome(Dispatchers.Main.immediate, cancel = true, late = { SecurityException("real") },
            wrap = { body -> withContext(NonCancellable) { body() } })
        println("VERIFY-CTX NonCancellable -> $t")
        assertTrue("got $t", t is SecurityException)
    }

    @Test
    fun cancelledFileReadSeesCancellationNotTheLateIOException() {
        val uri = Uri.parse("content://verify.files/late-fail")
        val hold = CountDownLatch(1)
        val reading = CountDownLatch(1)
        shadowOf(app().contentResolver).registerInputStreamSupplier(uri) {
            object : InputStream() {
                override fun read(): Int = throw UnsupportedOperationException()
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    reading.countDown(); hold.await(5, TimeUnit.SECONDS); throw IOException("disk gone")
                }
            }
        }
        val seen = AtomicReference<Throwable?>()
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            try { EventFileRepository(app()).readText(uri) } catch (t: Throwable) { seen.set(t) }
        }
        idleUntil(what = "reading") { reading.count == 0L }
        job.cancel()
        hold.countDown()
        idleUntil(what = "done") { job.isCompleted }
        assertTrue("got ${seen.get()}", seen.get() is CancellationException)
    }

    // ── View models: superseded calls with late errors ──────────────────

    @Test
    fun supersededPickWithLateFailureLeavesNoErrorButAnActivePickFailureIsShown() {
        contacts.add(2, "kB", "Batya")
        val hold = CountDownLatch(1)
        contactsProvider.responder = { uri, proj, sel, args ->
            if (uri.pathSegments == listOf("contacts", "7")) {
                hold.await(5, TimeUnit.SECONDS); throw IllegalStateException("stale pick failed")
            }
            if (uri.pathSegments == listOf("contacts", "9")) throw IllegalStateException("active pick failed")
            contacts.respond(uri, proj, sel, args)
        }
        val vm = ContactBirthdayViewModel(ContactBirthdayRepository(app()), deviceContextOn(LocalDate.of(2026, 1, 1)))
        vm.onIntent(ContactBirthdayIntent.ContactPicked(Uri.parse("content://com.android.contacts/contacts/7")))
        awaitEntered(contactsProvider, 1)
        vm.onIntent(ContactBirthdayIntent.ContactPicked(Uri.parse("content://com.android.contacts/contacts/2")))
        idleUntil(what = "B") { vm.state.value.contactName == "Batya" }
        hold.countDown()
        repeat(3) { Thread.sleep(40); idleUntil { true } }
        assertNull(vm.state.value.errorMessage)
        assertEquals("kB", vm.state.value.contactLookupKey)

        vm.onIntent(ContactBirthdayIntent.ContactPicked(Uri.parse("content://com.android.contacts/contacts/9")))
        idleUntil(what = "active error") { vm.state.value.errorMessage != null }
        assertEquals("active pick failed", vm.state.value.errorMessage)
    }

    @Test
    fun supersededStoredBirthdayLoadWithLateFailureDoesNotCrashOrChangeState() {
        contacts.add(7, "k7", "Rivka", Triple(12, 1, 5750))
        val hold = CountDownLatch(1)
        contactsProvider.responder = { uri, proj, sel, args ->
            if (args?.getOrNull(1) == "k7") { hold.await(5, TimeUnit.SECONDS); throw IllegalStateException("late") }
            contacts.respond(uri, proj, sel, args)
        }
        val vm = ContactBirthdayViewModel(ContactBirthdayRepository(app()), deviceContextOn(LocalDate.of(2026, 1, 1)))
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(contactsProvider, 1)
        vm.onIntent(ContactBirthdayIntent.ContactSelected("kB", "Batya", null))
        // Batya's own birthday lookup (TASK-51) completes first
        idleUntil(what = "kB checked") { !vm.state.value.storedBirthdayUnchecked }
        val before = vm.state.value
        hold.countDown()
        repeat(3) { Thread.sleep(40); idleUntil { true } }
        assertEquals(before, vm.state.value)
    }

    @Test
    fun supersededEventsBirthdayLoadWithLateFailureDoesNotCrash() {
        contacts.add(7, "k7", "Rivka", Triple(12, 1, 5750))
        val hold = CountDownLatch(1)
        var first = true
        contactsProvider.responder = { uri, proj, sel, args ->
            val isList = uri == android.provider.ContactsContract.Data.CONTENT_URI && args?.size == 1
            if (isList && first) { first = false; hold.await(5, TimeUnit.SECONDS); throw IllegalStateException("late") }
            contacts.respond(uri, proj, sel, args)
        }
        val vm = EventsViewModel(PersonalEventRepository(FakeDao()), ContactBirthdayRepository(app()), EventFileRepository(app()))
        awaitEntered(contactsProvider, 1)
        vm.onIntent(EventsIntent.ContactsPermissionGranted)
        idleUntil(what = "reload") { vm.state.value.birthdays.size == 1 }
        hold.countDown()
        repeat(3) { Thread.sleep(40); idleUntil { true } }
        assertEquals(1, vm.state.value.birthdays.size)
        assertNull(vm.state.value.message)
    }
}
