package ninja.doskey.app.levana.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.provider.ContactsContract
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A Contacts provider holding at most one Hebrew birthday for contact "key".
 * Records the thread of every call, and can hold queries until released or
 * until their [CancellationSignal] is cancelled.
 */
class FakeContactsProvider : ContentProvider() {

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor = query(uri, projection, selection, selectionArgs, sortOrder, null)

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
        cancellationSignal: CancellationSignal?
    ): Cursor {
        calls += Call("query", Thread.currentThread(), cancellationSignal != null)
        hold?.let { latch ->
            if (!ignoreSignal) {
                cancellationSignal?.setOnCancelListener {
                    signalCancelled.countDown()
                    latch.countDown()
                }
            }
            queryStarted.countDown()
            latch.await(5, TimeUnit.SECONDS)
            failAfterHold?.let { throw IllegalStateException(it) }
            cancellationSignal?.throwIfCanceled()
        }
        val columns = projection!!
        val cursor = MatrixCursor(columns)
        val path = uri.pathSegments
        val row: Map<String, Any?>? = when {
            path.first() == "data" && selection.orEmpty().contains(ContactsContract.Data.RAW_CONTACT_ID) ->
                stored?.let { mapOf(ContactsContract.Data._ID to DATA_ID) }
            path.first() == "data" -> stored?.let {
                mapOf(
                    ContactsContract.Data.LOOKUP_KEY to KEY,
                    ContactsContract.Data.DISPLAY_NAME_PRIMARY to NAME,
                    ContactsContract.Data.PHOTO_THUMBNAIL_URI to null,
                    ContactsContract.Data.DATA1 to it.first.toString(),
                    ContactsContract.Data.DATA2 to it.second.toString(),
                    ContactsContract.Data.DATA3 to it.third.toString()
                )
            }
            path.first() == "raw_contacts" -> mapOf(ContactsContract.RawContacts._ID to RAW_ID)
            path.first() == "contacts" -> mapOf(
                ContactsContract.Contacts._ID to CONTACT_ID,
                ContactsContract.Contacts.LOOKUP_KEY to KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY to NAME,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI to PHOTO
            )
            else -> null
        }
        row?.let { values -> cursor.addRow(columns.map { values[it] }) }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        calls += Call("insert", Thread.currentThread())
        stored = Triple(
            values!!.getAsString(ContactsContract.Data.DATA1).toInt(),
            values.getAsString(ContactsContract.Data.DATA2).toInt(),
            values.getAsString(ContactsContract.Data.DATA3).toInt()
        )
        return Uri.withAppendedPath(uri, DATA_ID.toString())
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        calls += Call("delete", Thread.currentThread())
        stored = null
        return 1
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        calls += Call("update", Thread.currentThread())
        return 1
    }

    override fun getType(uri: Uri): String? = null

    data class Call(val kind: String, val thread: Thread, val hadSignal: Boolean = false)

    companion object {
        const val KEY = "key"
        const val NAME = "Rivka"
        const val PHOTO = "content://com.android.contacts/contacts/1/photo"
        private const val CONTACT_ID = 1L
        private const val RAW_ID = 10L
        private const val DATA_ID = 100L

        val calls = CopyOnWriteArrayList<Call>()

        /** Stored birthday as (day, month, year), or null. */
        @Volatile var stored: Triple<Int, Int, Int>? = null

        /** When set, queries wait on it (or on cancellation). */
        @Volatile var hold: CountDownLatch? = null
        @Volatile var queryStarted = CountDownLatch(1)
        @Volatile var signalCancelled = CountDownLatch(1)

        /** Held queries ignore their signal, as a provider may. */
        @Volatile var ignoreSignal = false

        /** Held queries fail with this message once released. */
        @Volatile var failAfterHold: String? = null

        fun reset() {
            ignoreSignal = false
            failAfterHold = null
            calls.clear()
            stored = null
            hold = null
            queryStarted = CountDownLatch(1)
            signalCancelled = CountDownLatch(1)
        }
    }
}
