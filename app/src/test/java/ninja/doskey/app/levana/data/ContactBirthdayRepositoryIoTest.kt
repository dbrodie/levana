package ninja.doskey.app.levana.data

import android.Manifest
import android.app.Application
import android.net.Uri
import android.provider.ContactsContract
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Contact birthday provider work runs on the repository's IO dispatcher and honours cancellation. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ContactBirthdayRepositoryIoTest {

    private lateinit var io: ExecutorCoroutineDispatcher
    private lateinit var ioThread: Thread
    private lateinit var repository: ContactBirthdayRepository

    @Before
    fun setUp() {
        FakeContactsProvider.reset()
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
        Robolectric.setupContentProvider(FakeContactsProvider::class.java, ContactsContract.AUTHORITY)
        val threadReady = CountDownLatch(1)
        io = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "provider-io").also { ioThread = it; threadReady.countDown() }
        }.asCoroutineDispatcher()
        io.executor.execute { }
        threadReady.await()
        repository = ContactBirthdayRepository(app, io)
    }

    @After
    fun tearDown() {
        FakeContactsProvider.hold?.countDown()
        io.close()
    }

    private fun assertAllCallsOnIoThread() {
        assertTrue(FakeContactsProvider.calls.isNotEmpty())
        FakeContactsProvider.calls.forEach {
            assertEquals("${it.kind} ran on ${it.thread.name}", ioThread, it.thread)
        }
    }

    @Test
    fun readsQueryOnTheIoThreadNotTheCaller() = runBlocking<Unit> {
        FakeContactsProvider.stored = Triple(12, 3, 5785)

        val all = repository.getAll()
        val one = repository.getBirthdayForContact(FakeContactsProvider.KEY)
        val picked = repository.resolveContact(Uri.parse("content://com.android.contacts/contacts/1"))

        assertEquals(listOf(12 to 3), all.map { it.hebrewDay to it.hebrewMonth })
        assertEquals(5785, one?.hebrewYear)
        assertEquals(PickedContact(FakeContactsProvider.KEY, FakeContactsProvider.NAME, FakeContactsProvider.PHOTO), picked)
        assertAllCallsOnIoThread()
        assertTrue("queries carry a CancellationSignal", FakeContactsProvider.calls.all { it.hadSignal })
    }

    @Test
    fun setBirthdayLooksUpAndWritesOnTheIoThread() = runBlocking<Unit> {
        repository.setBirthday(FakeContactsProvider.KEY, day = 5, month = 8, year = 5790)

        assertEquals(Triple(5, 8, 5790), FakeContactsProvider.stored)
        assertTrue(FakeContactsProvider.calls.any { it.kind == "insert" })
        assertAllCallsOnIoThread()
    }

    @Test
    fun removeBirthdayDeletesOnTheIoThread() = runBlocking<Unit> {
        FakeContactsProvider.stored = Triple(5, 8, 5790)

        repository.removeBirthday(FakeContactsProvider.KEY)

        assertEquals(null, FakeContactsProvider.stored)
        assertTrue(FakeContactsProvider.calls.any { it.kind == "delete" })
        assertAllCallsOnIoThread()
    }

    @Test
    fun cancellingASlowQueryCancelsTheProviderAndReleasesTheCaller() {
        FakeContactsProvider.stored = Triple(12, 3, 5785)
        FakeContactsProvider.hold = CountDownLatch(1) // never released by the test

        // runBlocking would rethrow a non-cancellation failure (e.g. a leaked
        // OperationCanceledException) from the cancelled child
        runBlocking {
            val load = async(start = CoroutineStart.UNDISPATCHED) { repository.getAll() }
            assertTrue(FakeContactsProvider.queryStarted.await(5, TimeUnit.SECONDS))

            val started = System.nanoTime()
            load.cancel()
            withTimeout(2_000) { load.join() }
            val elapsedMs = (System.nanoTime() - started) / 1_000_000

            assertTrue(load.isCancelled)
            assertTrue("well before the provider's 5 s hold: $elapsedMs ms", elapsedMs < 2_000)
        }
        assertTrue(
            "provider saw its CancellationSignal cancelled",
            FakeContactsProvider.signalCancelled.await(0, TimeUnit.SECONDS)
        )
        assertFalse(FakeContactsProvider.calls.any { it.thread != ioThread })
    }
}
