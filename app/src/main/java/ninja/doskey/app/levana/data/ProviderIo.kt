package ninja.doskey.app.levana.data

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.OperationCanceledException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Runs blocking provider or stream work on [dispatcher]. Cancelling the
 * caller cancels the [CancellationSignal] handed to [block], so a provider
 * that honours it stops early; an abort surfaces as [CancellationException],
 * never as an error. Work that ignores the signal still runs to completion
 * (off the caller's thread) before the cancelled caller completes.
 */
internal suspend fun <T> runCancellable(
    dispatcher: CoroutineDispatcher,
    block: (CancellationSignal) -> T
): T = coroutineScope {
    val signal = CancellationSignal()
    val work = async(dispatcher) {
        try {
            block(signal)
        } catch (e: OperationCanceledException) {
            throw CancellationException("Provider work cancelled").apply { initCause(e) }
        }
    }
    try {
        work.await()
    } catch (e: CancellationException) {
        signal.cancel()
        throw e
    }
}

/**
 * Queries on [dispatcher] and reads the cursor with [read], which should call
 * [CancellationSignal.throwIfCanceled] while iterating. Returns null when the
 * provider returns no cursor.
 */
internal suspend fun <T> ContentResolver.queryCancellable(
    dispatcher: CoroutineDispatcher,
    uri: Uri,
    projection: Array<String>?,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    read: (Cursor, CancellationSignal) -> T
): T? = runCancellable(dispatcher) { signal ->
    query(uri, projection, selection, selectionArgs, sortOrder, signal)
        ?.use { cursor -> read(cursor, signal) }
}
