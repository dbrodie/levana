package ninja.doskey.app.levana.verifyio

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.Looper
import android.os.OperationCanceledException
import android.provider.CalendarContract
import android.provider.ContactsContract
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** A provider call as observed by the provider itself. */
data class ProviderCall(
    val kind: String,
    val uri: Uri,
    val thread: Thread,
    val hadSignal: Boolean
)

/**
 * A content provider that records the thread of every call, can be held
 * blocked on [gate] (honouring a CancellationSignal while blocked), and
 * answers from [responder] / [batchHandler].
 */
class ScriptedProvider : ContentProvider() {
    val calls = CopyOnWriteArrayList<ProviderCall>()
    val entered = AtomicInteger(0)
    val cancelObserved = AtomicBoolean(false)

    @Volatile var gate: CountDownLatch? = null
    @Volatile var gateTimeoutMs: Long = 3_000
    @Volatile var failure: (() -> RuntimeException)? = null
    @Volatile var batchFailure: (() -> RuntimeException)? = null
    @Volatile var responder: (Uri, Array<String>?, String?, Array<String>?) -> Cursor? =
        { _, _, _, _ -> null }
    @Volatile var batchHandler: (List<ContentProviderOperation>) -> Unit = {}

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
        cancellationSignal: CancellationSignal?
    ): Cursor? {
        calls += ProviderCall("query", uri, Thread.currentThread(), cancellationSignal != null)
        entered.incrementAndGet()
        waitGate(cancellationSignal)
        failure?.let { throw it() }
        return responder(uri, projection, selection, selectionArgs)
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = query(uri, projection, selection, selectionArgs, sortOrder, null)

    override fun applyBatch(
        operations: ArrayList<ContentProviderOperation>
    ): Array<ContentProviderResult> {
        calls += ProviderCall("batch", Uri.EMPTY, Thread.currentThread(), false)
        entered.incrementAndGet()
        waitGate(null)
        batchFailure?.let { throw it() }
        batchHandler(operations)
        return Array(operations.size) { ContentProviderResult(1) }
    }

    private fun waitGate(signal: CancellationSignal?) {
        val g = gate ?: return
        val deadline = System.currentTimeMillis() + gateTimeoutMs
        while (!g.await(5, TimeUnit.MILLISECONDS)) {
            if (signal?.isCanceled == true) {
                cancelObserved.set(true)
                throw OperationCanceledException()
            }
            if (System.currentTimeMillis() > deadline) {
                throw IllegalStateException("provider gate timed out on ${Thread.currentThread().name}")
            }
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ) = 0
}

const val BIRTHDAY_MIME = "vnd.android.cursor.item/com.levana.hebrew_birthday"

/** A small in-memory Contacts provider world served by a [ScriptedProvider]. */
class ContactsWorld {
    data class Contact(
        val id: Long,
        val lookupKey: String,
        val name: String,
        var birthday: Triple<Int, Int, Int>? = null
    ) {
        val rawId get() = id + 100
        val dataId get() = id + 1000
    }

    val contacts = CopyOnWriteArrayList<Contact>()

    fun add(id: Long, key: String, name: String, birthday: Triple<Int, Int, Int>? = null) =
        Contact(id, key, name, birthday).also { contacts += it }

    private fun value(c: Contact, column: String): Any? = when (column) {
        ContactsContract.Contacts._ID -> c.id
        ContactsContract.Contacts.LOOKUP_KEY -> c.lookupKey
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY -> c.name
        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI -> null
        ContactsContract.Data.DATA1 -> c.birthday?.first?.toString()
        ContactsContract.Data.DATA2 -> c.birthday?.second?.toString()
        ContactsContract.Data.DATA3 -> c.birthday?.third?.toString()
        else -> null
    }

    private fun cursor(projection: Array<String>?, rows: List<Contact>, idOverride: (Contact) -> Long? = { null }): Cursor {
        val cols = projection ?: arrayOf(ContactsContract.Contacts._ID)
        val mc = MatrixCursor(cols)
        rows.forEach { c ->
            mc.addRow(cols.map { col ->
                if (col == ContactsContract.Contacts._ID) idOverride(c) ?: c.id else value(c, col)
            }.toTypedArray())
        }
        return mc
    }

    fun respond(uri: Uri, projection: Array<String>?, selection: String?, args: Array<String>?): Cursor? {
        val seg = uri.pathSegments
        return when {
            seg.size >= 3 && seg[0] == "contacts" && seg[1] == "lookup" ->
                cursor(projection, contacts.filter { it.lookupKey == seg[2] })
            seg.size == 2 && seg[0] == "contacts" ->
                cursor(projection, contacts.filter { it.id == seg[1].toLong() })
            seg.firstOrNull() == "raw_contacts" ->
                cursor(projection, contacts.filter { it.id.toString() == args?.get(0) }) { it.rawId }
            seg.firstOrNull() == "data" && selection?.contains(ContactsContract.Data.RAW_CONTACT_ID) == true ->
                cursor(projection, contacts.filter { it.rawId.toString() == args?.get(0) && it.birthday != null }) { it.dataId }
            seg.firstOrNull() == "data" -> {
                require(args?.get(0) == BIRTHDAY_MIME) { "unexpected data query $selection" }
                val key = args.getOrNull(1)
                cursor(projection, contacts.filter { it.birthday != null && (key == null || it.lookupKey == key) }.sortedBy { it.name })
            }
            else -> error("unexpected contacts uri $uri")
        }
    }

    fun apply(ops: List<ContentProviderOperation>) {
        for ((i, op) in ops.withIndex()) {
            val values by lazy { op.resolveValueBackReferences(emptyArray(), i)!! }
            when {
                op.isInsert -> {
                    val raw = values.getAsLong(ContactsContract.Data.RAW_CONTACT_ID)
                    contacts.first { it.rawId == raw }.birthday = triple(values)
                }
                op.isUpdate -> {
                    val dataId = op.resolveSelectionArgsBackReferences(emptyArray(), i)!![0].toLong()
                    contacts.first { it.dataId == dataId }.birthday = triple(values)
                }
                op.isDelete -> {
                    val dataId = op.resolveSelectionArgsBackReferences(emptyArray(), i)!![0].toLong()
                    contacts.first { it.dataId == dataId }.birthday = null
                }
            }
        }
    }

    private fun triple(v: ContentValues) = Triple(
        v.getAsString(ContactsContract.Data.DATA1).toInt(),
        v.getAsString(ContactsContract.Data.DATA2).toInt(),
        v.getAsString(ContactsContract.Data.DATA3).toInt()
    )
}

/** Device calendars and instances served by a [ScriptedProvider]. */
class CalendarWorld {
    data class Cal(val id: Long, val name: String, val account: String, val color: Int)
    data class Instance(val eventId: Long, val title: String, val begin: Long, val end: Long, val allDay: Boolean, val calId: Long, val color: Int)

    val calendars = CopyOnWriteArrayList<Cal>()
    val instances = CopyOnWriteArrayList<Instance>()

    fun respond(uri: Uri, projection: Array<String>?, selection: String?, args: Array<String>?): Cursor? {
        val cols = projection!!
        val mc = MatrixCursor(cols)
        when (uri.pathSegments.first()) {
            "calendars" -> calendars.forEach { c ->
                mc.addRow(cols.map {
                    when (it) {
                        CalendarContract.Calendars._ID -> c.id
                        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME -> c.name
                        CalendarContract.Calendars.ACCOUNT_NAME -> c.account
                        CalendarContract.Calendars.CALENDAR_COLOR -> c.color
                        else -> null
                    }
                }.toTypedArray())
            }
            "instances" -> instances.forEach { e ->
                mc.addRow(cols.map {
                    when (it) {
                        CalendarContract.Instances.EVENT_ID -> e.eventId
                        CalendarContract.Instances.TITLE -> e.title
                        CalendarContract.Instances.BEGIN -> e.begin
                        CalendarContract.Instances.END -> e.end
                        CalendarContract.Instances.ALL_DAY -> if (e.allDay) 1 else 0
                        CalendarContract.Instances.CALENDAR_ID -> e.calId
                        CalendarContract.Instances.CALENDAR_COLOR -> e.color
                        else -> null
                    }
                }.toTypedArray())
            }
            else -> error("unexpected calendar uri $uri")
        }
        return mc
    }
}

fun app(): Application = RuntimeEnvironment.getApplication()

fun grantAll() {
    shadowOf(app()).grantPermissions(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CALENDAR
    )
}

fun installContacts(world: ContactsWorld): ScriptedProvider =
    Robolectric.setupContentProvider(ScriptedProvider::class.java, ContactsContract.AUTHORITY).apply {
        responder = world::respond
        batchHandler = world::apply
    }

fun installCalendar(world: CalendarWorld): ScriptedProvider =
    Robolectric.setupContentProvider(ScriptedProvider::class.java, CalendarContract.AUTHORITY).apply {
        responder = world::respond
    }

val mainThread: Thread get() = Looper.getMainLooper().thread

/** Runs main-looper work until [cond] holds, letting background threads progress. */
fun idleUntil(timeoutMs: Long = 5_000, what: String = "condition", cond: () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMs
    while (true) {
        shadowOf(Looper.getMainLooper()).idle()
        if (cond()) return
        check(System.currentTimeMillis() < end) { "timed out waiting for $what" }
        Thread.sleep(5)
    }
}

/** Waits (without running main) until the provider has been entered [n] times. */
fun awaitEntered(p: ScriptedProvider, n: Int, timeoutMs: Long = 5_000) {
    val end = System.currentTimeMillis() + timeoutMs
    while (p.entered.get() < n) {
        // Run main work too: the call may still be queued behind it
        shadowOf(Looper.getMainLooper()).idle()
        check(System.currentTimeMillis() < end) { "provider not entered $n times (got ${p.entered.get()})" }
        Thread.sleep(5)
    }
}

/** Proves the main looper is free: a posted runnable runs promptly. */
fun assertMainResponsive() {
    val ran = AtomicBoolean(false)
    android.os.Handler(Looper.getMainLooper()).post { ran.set(true) }
    val start = System.nanoTime()
    shadowOf(Looper.getMainLooper()).idle()
    val ms = (System.nanoTime() - start) / 1_000_000
    check(ran.get()) { "main runnable did not run" }
    check(ms < 500) { "main looper stalled ${ms}ms" }
}

class FakeDao : PersonalEventDao {
    val events = MutableStateFlow<List<PersonalEvent>>(emptyList())
    private var nextId = 1L
    override fun getAll(): Flow<List<PersonalEvent>> = events
    override suspend fun getById(id: Long) = events.value.firstOrNull { it.id == id }
    override suspend fun getByHebrewDate(month: Int, day: Int) =
        events.value.filter { it.hebrewMonth == month && it.hebrewDay == day }
    override suspend fun getAllOnce() = events.value
    override suspend fun insert(event: PersonalEvent): Long {
        val id = nextId++
        events.value = events.value + event.copy(id = id)
        return id
    }
    override suspend fun update(event: PersonalEvent) {
        events.value = events.value.map { if (it.id == event.id) event else it }
    }
    override suspend fun delete(event: PersonalEvent) {
        events.value = events.value.filter { it.id != event.id }
    }
}
