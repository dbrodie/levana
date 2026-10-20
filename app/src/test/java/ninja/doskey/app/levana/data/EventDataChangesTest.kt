package ninja.doskey.app.levana.data

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.os.Looper
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.room.Room
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.db.LevanaDatabase
import ninja.doskey.app.levana.data.db.PersonalEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class EventDataChangesTest {

    private lateinit var resolver: ContentResolver
    private lateinit var scope: CoroutineScope
    private val personalEvents = MutableStateFlow<List<Any>>(emptyList())
    private var contactsGranted = false
    private var calendarGranted = false

    @Before
    fun setUp() {
        resolver = RuntimeEnvironment.getApplication().contentResolver
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun changes(events: Flow<List<Any>> = personalEvents) =
        AndroidEventDataChanges(resolver, events, { contactsGranted }, { calendarGranted }, scope)

    private class Counter(val job: Job) {
        var count = 0
    }

    private fun collect(source: EventDataChanges): Counter {
        lateinit var counter: Counter
        val job = scope.launch { source.changes.collect { counter.count++ } }
        counter = Counter(job)
        idle()
        return counter
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))

    private fun observers(uri: Uri) = shadowOf(resolver).getContentObservers(uri).size

    private val contactsUri = ContactsContract.AUTHORITY_URI
    private val calendarUri = CalendarContract.CONTENT_URI

    @Test
    fun `providers are not observed without permission`() {
        val counter = collect(changes())

        assertEquals(0, observers(contactsUri))
        assertEquals(0, observers(calendarUri))
        assertEquals(0, counter.count)
    }

    @Test
    fun `contact and calendar changes are signalled while permitted`() {
        contactsGranted = true
        calendarGranted = true
        val counter = collect(changes())
        assertEquals(1, observers(contactsUri))
        assertEquals(1, observers(calendarUri))
        assertEquals("subscribing is not a change", 0, counter.count)

        resolver.notifyChange(Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, "42"), null)
        idle()
        assertEquals(1, counter.count)

        resolver.notifyChange(Uri.withAppendedPath(CalendarContract.Instances.CONTENT_URI, "when"), null)
        idle()
        assertEquals(2, counter.count)
    }

    @Test
    fun `personal event writes are signalled but the initial rows are not`() {
        val counter = collect(changes())
        assertEquals(0, counter.count)

        personalEvents.value = listOf(event("Added"))
        idle()
        assertEquals(1, counter.count)

        personalEvents.value = emptyList()
        idle()
        assertEquals(2, counter.count)
    }

    @Test
    fun `room inserts updates and deletes are signalled`() {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LevanaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = PersonalEventRepository(db.personalEventDao())
            var roomEmissions = 0
            val counter = collect(changes(repository.getAll().onEach { roomEmissions++ }))
            // Room's first emission is the current rows, not a change.
            awaitUntil { roomEmissions == 1 }
            assertEquals(0, counter.count)

            val id = runBlocking { repository.insert(event("Added")) }
            awaitUntil { counter.count == 1 }

            runBlocking { repository.update(event("Edited").copy(id = id)) }
            awaitUntil { counter.count == 2 }

            runBlocking { repository.delete(event("Edited").copy(id = id)) }
            awaitUntil { counter.count == 3 }
        } finally {
            db.close()
        }
    }

    @Test
    fun `granting a permission on recheck starts observing and signals once`() {
        val source = changes()
        val counter = collect(source)

        source.recheckPermissions()
        idle()
        assertEquals("nothing changed", 0, counter.count)

        calendarGranted = true
        source.recheckPermissions()
        idle()
        assertEquals(1, counter.count)
        assertEquals(1, observers(calendarUri))
        assertEquals(0, observers(contactsUri))

        contactsGranted = true
        source.recheckPermissions()
        idle()
        assertEquals(2, counter.count)
        assertEquals("no duplicate observers", 1, observers(calendarUri))
        assertEquals(1, observers(contactsUri))
    }

    @Test
    fun `revoking a permission on recheck stops observing that provider`() {
        contactsGranted = true
        calendarGranted = true
        val source = changes()
        val counter = collect(source)

        calendarGranted = false
        source.recheckPermissions()
        idle()
        assertEquals(1, counter.count)
        assertEquals(0, observers(calendarUri))
        assertEquals(1, observers(contactsUri))

        resolver.notifyChange(CalendarContract.Events.CONTENT_URI, null)
        idle()
        assertEquals(1, counter.count)
    }

    @Test
    fun `collectors share one observer which is released when all stop`() {
        contactsGranted = true
        calendarGranted = true
        val source = changes()
        val first = collect(source)
        val second = collect(source)
        assertEquals(1, observers(contactsUri))
        assertEquals(1, observers(calendarUri))

        resolver.notifyChange(ContactsContract.Data.CONTENT_URI, null)
        idle()
        assertEquals(1, first.count)
        assertEquals(1, second.count)

        first.job.cancel()
        idle()
        assertEquals(1, observers(contactsUri))

        second.job.cancel()
        idle()
        assertEquals(0, observers(contactsUri))
        assertEquals(0, observers(calendarUri))

        // Observing again after everyone left registers afresh, exactly once.
        collect(source)
        assertEquals(1, observers(contactsUri))
    }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (true) {
            idle()
            if (condition()) return
            assertTrue("timed out", System.currentTimeMillis() < deadline)
            Thread.sleep(10)
        }
    }

    private fun event(title: String) =
        PersonalEvent(title = title, hebrewDay = 1, hebrewMonth = 7, hebrewYear = 5780)
}
