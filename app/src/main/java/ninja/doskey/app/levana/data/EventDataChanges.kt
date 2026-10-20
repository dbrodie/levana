package ninja.doskey.app.levana.data

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.withIndex

/** Signals that event data shown in the calendar (personal events, birthdays, device calendars) may have changed. */
interface EventDataChanges {
    val changes: Flow<Unit>

    /** Re-reads the contacts/calendar permissions; a change starts or stops observing that source. */
    fun recheckPermissions()
}

class AndroidEventDataChanges(
    private val contentResolver: ContentResolver,
    personalEvents: Flow<List<Any>>,
    private val hasContactsPermission: () -> Boolean,
    private val hasCalendarPermission: () -> Boolean,
    scope: CoroutineScope
) : EventDataChanges {

    private data class Permissions(val contacts: Boolean, val calendar: Boolean)

    private val permissions = MutableStateFlow(currentPermissions())

    // Room emits the current rows on subscription and again after every write to the table.
    private val personalEventChanges: Flow<Unit> =
        personalEvents.distinctUntilChanged().drop(1).map { }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val changes: Flow<Unit> = permissions.withIndex()
        .flatMapLatest { (index, granted) ->
            val sources = merge(
                personalEventChanges,
                if (granted.contacts) observe(ContactsContract.AUTHORITY_URI) else emptyFlow(),
                if (granted.calendar) observe(CalendarContract.CONTENT_URI) else emptyFlow()
            )
            // A permission change makes previously hidden (or now unreadable) data relevant.
            if (index == 0) sources else sources.onStart { emit(Unit) }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed())

    override fun recheckPermissions() {
        permissions.value = currentPermissions()
    }

    private fun currentPermissions() =
        Permissions(hasContactsPermission(), hasCalendarPermission())

    private fun observe(uri: Uri): Flow<Unit> = callbackFlow {
        // Called on a binder thread; only hands the signal to the collector.
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val registered = try {
            contentResolver.registerContentObserver(uri, true, observer)
            true
        } catch (_: SecurityException) {
            false
        }
        awaitClose {
            if (registered) contentResolver.unregisterContentObserver(observer)
        }
    }.buffer(Channel.CONFLATED)
}
