package ninja.doskey.app.levana.ui.events

import android.app.Application
import android.net.Uri
import android.os.Looper
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventFileRepository
import ninja.doskey.app.levana.data.EventSerializer
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Import/export streams are opened, read and written off the main thread, with bounded input. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class EventsFileIoTest {

    private val app get() = RuntimeEnvironment.getApplication()
    private val resolver get() = shadowOf(app.contentResolver)
    private val mainThread get() = Looper.getMainLooper().thread
    private val uri = Uri.parse("content://test.documents/levana_events.json")

    private val streamThreads = CopyOnWriteArrayList<Thread>()

    private fun recordingInput(bytes: ByteArray) = object : InputStream() {
        private val source = bytes.inputStream()
        var bytesRead = 0
            private set
        override fun read(): Int = read(ByteArray(1)).let { if (it < 0) it else 0 }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            streamThreads += Thread.currentThread()
            return source.read(b, off, len).also { if (it > 0) bytesRead += it }
        }
    }

    private class RecordingOutput(val threads: MutableList<Thread>) : ByteArrayOutputStream() {
        override fun write(b: ByteArray, off: Int, len: Int) {
            threads += Thread.currentThread()
            super.write(b, off, len)
        }
    }

    private val events = listOf(
        PersonalEvent(title = "Siyum", hebrewDay = 3, hebrewMonth = 5, hebrewYear = 5786),
        PersonalEvent(title = "Yahrzeit", hebrewDay = 30, hebrewMonth = 9, hebrewYear = 5750, useYahrzeitRules = true)
    )

    private class FakeDao(var events: List<PersonalEvent>) : PersonalEventDao {
        val inserted = mutableListOf<PersonalEvent>()
        override fun getAll(): Flow<List<PersonalEvent>> = flowOf(events)
        override suspend fun getAllOnce(): List<PersonalEvent> = events
        override suspend fun getById(id: Long): PersonalEvent? = null
        override suspend fun getByHebrewDate(month: Int, day: Int): List<PersonalEvent> = emptyList()
        override suspend fun insert(event: PersonalEvent): Long = 0L.also { inserted += event }
        override suspend fun update(event: PersonalEvent) = Unit
        override suspend fun delete(event: PersonalEvent) = Unit
    }

    private fun viewModel(
        dao: FakeDao,
        birthdays: ContactBirthdayRepository = ContactBirthdayRepository(app)
    ) = EventsViewModel(PersonalEventRepository(dao), birthdays, EventFileRepository(app))

    private fun awaitOnMain(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }

    // ── Repository ──────────────────────────────────────────────────────────

    @Test
    fun repositoryStreamsOnItsIoDispatcher() {
        val executor = Executors.newSingleThreadExecutor { Thread(it, "file-io") }
        executor.asCoroutineDispatcher().use { io ->
            val repository = EventFileRepository(app, io)
            val output = RecordingOutput(streamThreads)
            resolver.registerOutputStream(uri, output)
            resolver.registerInputStream(uri, recordingInput("hello".toByteArray()))

            runBlocking {
                repository.writeText(uri, "שלום, world")
                assertEquals("hello", repository.readText(uri))
            }

            assertEquals("שלום, world", output.toString(Charsets.UTF_8.name()))
            assertTrue(streamThreads.isNotEmpty())
            streamThreads.forEach { assertEquals("file-io", it.name) }
        }
    }

    @Test
    fun oversizedImportStopsReadingAtTheLimit() {
        val input = recordingInput(ByteArray(EventFileRepository.MAX_IMPORT_BYTES * 3))
        resolver.registerInputStream(uri, input)

        try {
            runBlocking { EventFileRepository(app).readText(uri) }
            fail("Expected FileTooLargeException")
        } catch (e: EventFileRepository.FileTooLargeException) {
            assertEquals(EventFileRepository.MAX_IMPORT_BYTES, e.maxBytes)
        }
        assertTrue(input.bytesRead <= EventFileRepository.MAX_IMPORT_BYTES + 8 * 1024)
    }

    // ── View model ──────────────────────────────────────────────────────────

    @Test
    fun exportWritesOffTheMainThreadAndReportsTheCount() {
        val output = RecordingOutput(streamThreads)
        resolver.registerOutputStream(uri, output)
        val vm = viewModel(FakeDao(events))
        awaitOnMain { vm.state.value.customEvents.size == 2 }

        vm.onIntent(EventsIntent.ExportEvents(uri, isCsv = false))
        awaitOnMain { vm.state.value.message != null }

        assertEquals(EventsMessage.Exported(2), vm.state.value.message)
        assertEquals(EventSerializer.toJson(events), output.toString(Charsets.UTF_8.name()))
        streamThreads.forEach { assertNotSame(mainThread, it) }
    }

    @Test
    fun exportFailureIsReportedWithItsReason() {
        resolver.registerOutputStream(uri, object : OutputStream() {
            override fun write(b: Int) = throw IOException("disk full")
            override fun write(b: ByteArray, off: Int, len: Int) = throw IOException("disk full")
        })
        val vm = viewModel(FakeDao(events))

        vm.onIntent(EventsIntent.ExportEvents(uri, isCsv = true))
        awaitOnMain { vm.state.value.message != null }

        assertEquals(EventsMessage.ExportFailed("disk full"), vm.state.value.message)
    }

    @Test
    fun importReadsOffTheMainThreadAndImports() {
        resolver.registerInputStream(uri, recordingInput(EventSerializer.toJson(events).toByteArray()))
        val dao = FakeDao(emptyList())
        val vm = viewModel(dao)

        vm.onIntent(EventsIntent.ImportFromUri(uri))
        awaitOnMain { vm.state.value.importResult != null }

        assertEquals(ImportResult(imported = 2, skipped = 0), vm.state.value.importResult)
        assertEquals(events.map { it.title }, dao.inserted.map { it.title })
        assertTrue(streamThreads.isNotEmpty())
        streamThreads.forEach { assertNotSame(mainThread, it) }
    }

    @Test
    fun oversizedImportIsRejectedWithTheLimit() {
        resolver.registerInputStream(uri, recordingInput(ByteArray(EventFileRepository.MAX_IMPORT_BYTES + 1)))
        val dao = FakeDao(emptyList())
        val vm = viewModel(dao)

        vm.onIntent(EventsIntent.ImportFromUri(uri))
        awaitOnMain { vm.state.value.message != null }

        assertEquals(EventsMessage.ImportTooLarge(1024), vm.state.value.message)
        assertNull(vm.state.value.importResult)
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun unreadableImportReportsTheReason() {
        resolver.registerInputStream(uri, object : InputStream() {
            override fun read(): Int = throw IOException("gone")
            override fun read(b: ByteArray, off: Int, len: Int): Int = throw IOException("gone")
        })
        val vm = viewModel(FakeDao(emptyList()))

        vm.onIntent(EventsIntent.ImportFromUri(uri))
        awaitOnMain { vm.state.value.message != null }

        assertEquals(EventsMessage.ImportFailed("gone"), vm.state.value.message)
    }

    @Test
    fun failedBirthdayRemovalIsReported() {
        val birthdays = object : ContactBirthdayRepository(app) {
            override suspend fun removeBirthday(contactLookupKey: String) =
                throw SecurityException("Permission denied")
        }
        val vm = viewModel(FakeDao(emptyList()), birthdays)

        vm.onIntent(EventsIntent.DeleteBirthday("key"))
        awaitOnMain { vm.state.value.message != null }

        assertEquals(EventsMessage.RemoveBirthdayFailed("Permission denied"), vm.state.value.message)
    }
}
