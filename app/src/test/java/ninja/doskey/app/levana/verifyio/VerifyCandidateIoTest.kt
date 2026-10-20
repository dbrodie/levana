package ninja.doskey.app.levana.verifyio

import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventFileRepository
import ninja.doskey.app.levana.data.EventSerializer
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.data.TodayProvider
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.loadContactPhoto
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayIntent
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayViewModel
import ninja.doskey.app.levana.ui.calendarselection.CalendarSelectionViewModel
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.ui.events.EventsIntent
import ninja.doskey.app.levana.ui.events.EventsMessage
import ninja.doskey.app.levana.ui.events.EventsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class VerifyCandidateIoTest {

    private val contacts = ContactsWorld()
    private val calendar = CalendarWorld()
    private lateinit var contactsProvider: ScriptedProvider
    private lateinit var calendarProvider: ScriptedProvider
    private val dao = FakeDao()

    @Before
    fun setUp() {
        grantAll()
        contactsProvider = installContacts(contacts)
        calendarProvider = installCalendar(calendar)
    }

    private fun assertAllOffMain(p: ScriptedProvider) {
        assertTrue("provider never called", p.calls.isNotEmpty())
        p.calls.forEach { assertNotEquals("${it.kind} ${it.uri} ran on main", mainThread, it.thread) }
    }

    private fun timeMs(block: () -> Unit): Long {
        val s = System.nanoTime(); block(); return (System.nanoTime() - s) / 1_000_000
    }

    private fun eventsVm() = EventsViewModel(
        PersonalEventRepository(dao), ContactBirthdayRepository(app()), EventFileRepository(app())
    )

    /** Threads on which a view model's state changed (collector resumes on the emitter's thread). */
    private fun <T> recordEmitterThreads(flow: kotlinx.coroutines.flow.StateFlow<T>): Pair<Job, List<Thread>> {
        val threads = CopyOnWriteArrayList<Thread>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch { flow.collect { threads += Thread.currentThread() } }
        return job to threads
    }

    private fun isBirthdayList(uri: Uri, sel: String?) =
        uri == ContactsContract.Data.CONTENT_URI && sel?.contains(ContactsContract.Data.RAW_CONTACT_ID) != true &&
            sel?.contains(ContactsContract.Data.LOOKUP_KEY) != true

    // ── Query cancellation ───────────────────────────────────────────────

    @Test
    fun cancellingACallerCancelsTheProviderQueryThroughItsSignal() {
        calendar.calendars += CalendarWorld.Cal(1, "Home", "a", 1)
        calendarProvider.gate = CountDownLatch(1)
        val outcome = AtomicReference<Throwable?>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch {
            try {
                SystemCalendarRepository(app()).getDeviceCalendars()
            } catch (t: Throwable) {
                outcome.set(t); throw t
            }
        }
        awaitEntered(calendarProvider, 1)
        assertTrue("query received a CancellationSignal", calendarProvider.calls[0].hadSignal)
        job.cancel()
        idleUntil(what = "job completion") { job.isCompleted }
        assertTrue(job.isCancelled)
        assertTrue("provider saw the signal cancelled", calendarProvider.cancelObserved.get())
        assertTrue("caller sees cancellation, not an error: ${outcome.get()}", outcome.get() is CancellationException)
    }

    @Test
    fun repositoryReadsCalledFromMainRunOnAnotherThread() {
        val date = LocalDate.of(2026, 3, 10)
        val jd = com.kosherjava.zmanim.hebrewcalendar.JewishDate(java.util.GregorianCalendar(2026, 2, 10))
        contacts.add(7, "k7", "Rivka", Triple(jd.jewishDayOfMonth, jd.jewishMonth, 5750))
        val begin = date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        calendar.instances += CalendarWorld.Instance(1, "Dentist", begin, begin + 3_600_000, false, 1, 0xFF)
        calendar.calendars += CalendarWorld.Cal(1, "Home", "a", 1)
        val birthdays = ContactBirthdayRepository(app())
        val system = SystemCalendarRepository(app())
        assertEquals(mainThread, Thread.currentThread())
        runBlocking {
            assertEquals(1, birthdays.getBirthdaysForDate(date).size)
            assertTrue(date in birthdays.getBirthdayDatesForGregorianMonth(2026, 3))
            assertTrue(jd.jewishDayOfMonth in birthdays.getBirthdayDaysForHebrewMonth(jd.jewishYear, jd.jewishMonth))
            assertEquals(1, birthdays.getAll().size)
            assertEquals("Rivka", birthdays.getBirthdayForContact("k7")?.contactName)
            assertEquals(listOf("Dentist"), system.getEventsForDate(date, setOf(1L)).map { it.title })
            assertTrue(system.getEventColorsForDateRange(date, date, setOf(1L)).containsKey(date))
            assertEquals(1, system.getDeviceCalendars().size)
        }
        assertAllOffMain(contactsProvider)
        assertAllOffMain(calendarProvider)
    }

    @Test
    fun calendarSelectionPermissionRevokedMidQueryShowsNoPermission() {
        calendarProvider.failure = { SecurityException("revoked") }
        val vm = CalendarSelectionViewModel(SystemCalendarRepository(app()), PreferencesRepository(app(), CityRepository(app())))
        idleUntil(what = "load") { !vm.state.value.isLoading }
        assertFalse(vm.state.value.hasPermission)
    }

    // ── Events: birthdays list, delete, stale loads ─────────────────────

    @Test
    fun eventsBirthdayListAndDeleteRunOffMainAndStateChangesOnMain() {
        val c = contacts.add(7, "k7", "Rivka", Triple(12, 3, 5750))
        contactsProvider.gate = CountDownLatch(1)
        val vm = eventsVm()
        val (job, threads) = recordEmitterThreads(vm.state)
        awaitEntered(contactsProvider, 1)
        assertMainResponsive()
        contactsProvider.gate!!.countDown()
        contactsProvider.gate = null
        idleUntil(what = "birthdays") { vm.state.value.birthdays.size == 1 }

        contactsProvider.gate = CountDownLatch(1)
        val ms = timeMs { vm.onIntent(EventsIntent.DeleteBirthday("k7")) }
        assertTrue("DeleteBirthday blocked ${ms}ms", ms < 1_000)
        awaitEntered(contactsProvider, 2)
        assertMainResponsive()
        contactsProvider.gate!!.countDown()
        idleUntil(what = "delete") { vm.state.value.birthdays.isEmpty() }
        assertNull(c.birthday)
        assertAllOffMain(contactsProvider)
        job.cancel()
        threads.forEach { assertEquals("state emitted off main", mainThread, it) }
    }

    @Test
    fun deleteFailureIsShownAsAMessage() {
        contacts.add(7, "k7", "Rivka", Triple(12, 3, 5750))
        val vm = eventsVm()
        idleUntil(what = "birthdays") { vm.state.value.birthdays.size == 1 }
        contactsProvider.batchFailure = { IllegalStateException("nope") }
        vm.onIntent(EventsIntent.DeleteBirthday("k7"))
        idleUntil(what = "message") { vm.state.value.message != null }
        assertEquals(EventsMessage.RemoveBirthdayFailed("nope"), vm.state.value.message)
        assertEquals(1, vm.state.value.birthdays.size)
    }

    @Test
    fun aSlowEarlierBirthdayLoadCannotResurrectADeletedBirthday() {
        val c = contacts.add(7, "k7", "Rivka", Triple(12, 3, 5750))
        val firstList = CountDownLatch(1)
        val listCalls = AtomicInteger(0)
        val stale = AtomicReference<MatrixCursor>()
        contactsProvider.responder = { uri, proj, sel, args ->
            if (isBirthdayList(uri, sel) && listCalls.incrementAndGet() == 1) {
                // Snapshot before the delete, then ignore cancellation while blocked
                val snap = contacts.respond(uri, proj, sel, args)
                firstList.await(5, TimeUnit.SECONDS)
                snap
            } else contacts.respond(uri, proj, sel, args)
        }
        val vm = eventsVm()
        awaitEntered(contactsProvider, 1)
        vm.onIntent(EventsIntent.DeleteBirthday("k7"))
        idleUntil(what = "delete + reload") { c.birthday == null && listCalls.get() >= 2 }
        idleUntil(what = "settle") { true }
        firstList.countDown()
        Thread.sleep(100)
        idleUntil(what = "settle") { true }
        Thread.sleep(50)
        idleUntil(what = "settle") { true }
        assertTrue("deleted birthday came back: ${vm.state.value.birthdays}", vm.state.value.birthdays.isEmpty())
    }

    // ── Import / export ─────────────────────────────────────────────────

    private class GatedInput(val data: ByteArray, val gate: CountDownLatch?) : InputStream() {
        val readThreads = CopyOnWriteArrayList<Thread>()
        val closed = AtomicBoolean(false)
        private var pos = 0
        override fun read(): Int = throw UnsupportedOperationException()
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            readThreads += Thread.currentThread()
            gate?.await(5, TimeUnit.SECONDS)
            if (pos >= data.size) return -1
            val n = minOf(len, data.size - pos)
            System.arraycopy(data, pos, b, off, n); pos += n
            return n
        }
        override fun close() { closed.set(true) }
    }

    private class EndlessInput : InputStream() {
        val bytesServed = AtomicInteger(0)
        override fun read(): Int = 'a'.code
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            java.util.Arrays.fill(b, off, off + len, 'a'.code.toByte())
            bytesServed.addAndGet(len); return len
        }
    }

    private fun sampleEvents() = listOf(
        PersonalEvent(title = "Yahrzeit A", hebrewDay = 3, hebrewMonth = 5, hebrewYear = 5770),
        PersonalEvent(title = "Anniversary", hebrewDay = 9, hebrewMonth = 2, hebrewYear = 5780)
    )

    @Test
    fun importReadsOffMainAndInsertsTheEvents() {
        val uri = Uri.parse("content://verify.files/doc/events.json")
        val gate = CountDownLatch(1)
        val opened = CopyOnWriteArrayList<Thread>()
        val input = GatedInput(EventSerializer.toJson(sampleEvents()).toByteArray(), gate)
        shadowOf(app().contentResolver).registerInputStreamSupplier(uri) { opened += Thread.currentThread(); input }
        val vm = eventsVm()
        idleUntil(what = "init") { !vm.state.value.isLoading }

        val ms = timeMs { vm.onIntent(EventsIntent.ImportFromUri(uri)) }
        assertTrue("import blocked ${ms}ms", ms < 1_000)
        idleUntil(what = "read started") { input.readThreads.isNotEmpty() }
        assertMainResponsive()
        assertNull(vm.state.value.importResult)
        gate.countDown()
        idleUntil(what = "import") { vm.state.value.importResult != null }

        assertEquals(2, vm.state.value.importResult!!.imported)
        assertEquals(setOf("Yahrzeit A", "Anniversary"), dao.events.value.map { it.title }.toSet())
        (opened + input.readThreads).forEach { assertNotEquals("stream IO on main", mainThread, it) }
        assertTrue(input.closed.get())
    }

    @Test
    fun importOfAnEndlessFileStopsAtTheLimitAndSaysSo() {
        val uri = Uri.parse("content://verify.files/doc/huge.json")
        val input = EndlessInput()
        shadowOf(app().contentResolver).registerInputStreamSupplier(uri) { input }
        val vm = eventsVm()
        vm.onIntent(EventsIntent.ImportFromUri(uri))
        idleUntil(what = "too large") { vm.state.value.message != null }
        assertEquals(EventsMessage.ImportTooLarge(1024), vm.state.value.message)
        assertTrue("read ${input.bytesServed.get()} bytes", input.bytesServed.get() <= EventFileRepository.MAX_IMPORT_BYTES + 16 * 1024)
        assertTrue(dao.events.value.isEmpty())
    }

    @Test
    fun importOpenFailureIsShownAsAMessage() {
        val uri = Uri.parse("content://verify.files/doc/missing.json")
        shadowOf(app().contentResolver).registerInputStreamSupplier(uri) { throw RuntimeException(FileNotFoundException("gone")) }
        val vm = eventsVm()
        vm.onIntent(EventsIntent.ImportFromUri(uri))
        idleUntil(what = "message") { vm.state.value.message != null }
        assertTrue(vm.state.value.message is EventsMessage.ImportFailed)
    }

    @Test
    fun importIsAbandonedWhenTheViewModelIsCleared() {
        val uri = Uri.parse("content://verify.files/doc/events.json")
        val gate = CountDownLatch(1)
        // Larger than one chunk so a cancellation check happens after the blocked read
        val big = EventSerializer.toJson((1..400).map {
            PersonalEvent(title = "Event $it", hebrewDay = 1 + it % 28, hebrewMonth = 1 + it % 12, hebrewYear = 5700 + it)
        }).toByteArray()
        assertTrue(big.size > 16 * 1024)
        val input = GatedInput(big, gate)
        shadowOf(app().contentResolver).registerInputStreamSupplier(uri) { input }
        val store = ViewModelStore()
        val vm = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = eventsVm() as T
        })[EventsViewModel::class.java]
        vm.onIntent(EventsIntent.ImportFromUri(uri))
        idleUntil(what = "read started") { input.readThreads.isNotEmpty() }
        store.clear()
        gate.countDown()
        idleUntil(what = "stream closed") { input.closed.get() }
        Thread.sleep(50)
        idleUntil(what = "settle") { true }
        assertTrue("cancelled import still inserted events", dao.events.value.isEmpty())
        assertTrue("kept reading after cancel: ${input.readThreads.size} reads", input.readThreads.size <= 2)
        assertNull(vm.state.value.importResult)
        assertNull(vm.state.value.message)
    }

    private class GatedOutput(val gate: CountDownLatch?, val failWith: IOException? = null) : OutputStream() {
        val bytes = ByteArrayOutputStream()
        val threads = CopyOnWriteArrayList<Thread>()
        val closed = AtomicBoolean(false)
        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)
        override fun write(b: ByteArray, off: Int, len: Int) {
            threads += Thread.currentThread()
            gate?.await(5, TimeUnit.SECONDS)
            failWith?.let { throw it }
            bytes.write(b, off, len)
        }
        override fun close() { closed.set(true) }
    }

    @Test
    fun exportWritesOffMainAndReportsTheCount() {
        runBlocking { sampleEvents().forEach { dao.insert(it) } }
        val vm = eventsVm()
        idleUntil(what = "events") { vm.state.value.customEvents.size == 2 }
        for (csv in listOf(false, true)) {
            val uri = Uri.parse("content://verify.files/out/$csv")
            val gate = CountDownLatch(1)
            val out = GatedOutput(gate)
            shadowOf(app().contentResolver).registerOutputStreamSupplier(uri) { out }
            val ms = timeMs { vm.onIntent(EventsIntent.ExportEvents(uri, csv)) }
            assertTrue("export blocked ${ms}ms", ms < 1_000)
            idleUntil(what = "write started") { out.threads.isNotEmpty() }
            assertMainResponsive()
            assertNull(vm.state.value.message)
            gate.countDown()
            idleUntil(what = "exported") { vm.state.value.message != null }
            assertEquals(EventsMessage.Exported(2), vm.state.value.message)
            val events = vm.state.value.customEvents
            val expected = if (csv) EventSerializer.toCsv(events) else EventSerializer.toJson(events)
            assertEquals(expected, out.bytes.toString(Charsets.UTF_8.name()))
            assertTrue(out.closed.get())
            out.threads.forEach { assertNotEquals(mainThread, it) }
            vm.onIntent(EventsIntent.DismissMessage)
        }
    }

    @Test
    fun exportWriteFailureIsShownAsAMessage() {
        val uri = Uri.parse("content://verify.files/out/fail")
        shadowOf(app().contentResolver).registerOutputStreamSupplier(uri) { GatedOutput(null, IOException("disk full")) }
        val vm = eventsVm()
        vm.onIntent(EventsIntent.ExportEvents(uri, false))
        idleUntil(what = "message") { vm.state.value.message != null }
        assertEquals(EventsMessage.ExportFailed("disk full"), vm.state.value.message)
    }

    // ── Contact picker and photos ───────────────────────────────────────

    @Test
    fun pickedContactIsResolvedOffMain() {
        contacts.add(9, "k9", "Leah")
        contactsProvider.gate = CountDownLatch(1)
        val vm = ContactBirthdayViewModel(ContactBirthdayRepository(app()), TodayProvider { LocalDate.of(2026, 1, 1) })
        val ms = timeMs { vm.onIntent(ContactBirthdayIntent.ContactPicked(Uri.parse("content://com.android.contacts/contacts/9"))) }
        assertTrue(ms < 1_000)
        awaitEntered(contactsProvider, 1)
        assertMainResponsive()
        contactsProvider.gate!!.countDown()
        idleUntil(what = "picked") { vm.state.value.contactName == "Leah" }
        assertEquals("k9", vm.state.value.contactLookupKey)
        assertAllOffMain(contactsProvider)
    }

    @Test
    fun contactPhotoIsDecodedOffMainAndBadDataIsNull() {
        val uri = Uri.parse("content://com.android.contacts/photo/1")
        val threads = CopyOnWriteArrayList<Thread>()
        shadowOf(app().contentResolver).registerInputStreamSupplier(uri) {
            threads += Thread.currentThread(); "not an image".byteInputStream()
        }
        val result = runBlocking { loadContactPhoto(app().contentResolver, uri.toString()) }
        assertNull(result)
        assertTrue(threads.isNotEmpty())
        threads.forEach { assertNotEquals(mainThread, it) }
    }

    // ── Stale results racing user input (observations) ──────────────────

    @Test
    fun aSlowFirstPickDoesNotOverrideALaterPick() {
        contacts.add(1, "kA", "Avi")
        contacts.add(2, "kB", "Batya")
        val slowA = CountDownLatch(1)
        contactsProvider.responder = { uri, proj, sel, args ->
            if (uri.lastPathSegment == "1") slowA.await(5, TimeUnit.SECONDS)
            contacts.respond(uri, proj, sel, args)
        }
        val vm = ContactBirthdayViewModel(ContactBirthdayRepository(app()), TodayProvider { LocalDate.of(2026, 1, 1) })
        vm.onIntent(ContactBirthdayIntent.ContactPicked(Uri.parse("content://com.android.contacts/contacts/1")))
        awaitEntered(contactsProvider, 1)
        vm.onIntent(ContactBirthdayIntent.ContactPicked(Uri.parse("content://com.android.contacts/contacts/2")))
        idleUntil(what = "B") { vm.state.value.contactName == "Batya" }
        slowA.countDown()
        Thread.sleep(100)
        idleUntil(what = "settle") { true }
        assertEquals("latest pick wins", "Batya", vm.state.value.contactName)
    }

    @Test
    fun aSlowBirthdayLoadDoesNotDiscardAnEditMadeWhileLoading() {
        contacts.add(7, "k7", "Rivka", Triple(12, 3, 5750))
        contactsProvider.gate = CountDownLatch(1)
        val vm = ContactBirthdayViewModel(ContactBirthdayRepository(app()), TodayProvider { LocalDate.of(2026, 1, 1) })
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(contactsProvider, 1)
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(20))
        contactsProvider.gate!!.countDown()
        idleUntil(what = "load") { vm.state.value.isEditing }
        assertEquals("user's edit kept", 20, vm.state.value.hebrewDay)
    }
}
