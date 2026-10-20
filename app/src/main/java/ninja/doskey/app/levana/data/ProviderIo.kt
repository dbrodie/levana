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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Runs blocking provider or stream work on [dispatcher]. Cancelling the
 * caller cancels the [CancellationSignal] handed to [block], so a provider
 * that honours it stops early; an abort surfaces as [CancellationException],
 * never as an error. Work that ignores the signal still runs to completion
 * (off the caller's thread) before the cancelled caller completes, and a
 * cancelled caller always sees [CancellationException], even if that work
 * later fails: a superseded request can never report a stale error.
 */
internal suspend fun <T> runCancellable(
    dispatcher: CoroutineDispatcher,
    block: (CancellationSignal) -> T
): T = try {
    runOnWorker(dispatcher, block)
} catch (e: Throwable) {
    // coroutineScope prefers a worker's late failure over the caller's own
    // cancellation; restore the cancellation
    currentCoroutineContext().ensureActive()
    throw e
}

private suspend fun <T> runOnWorker(
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
