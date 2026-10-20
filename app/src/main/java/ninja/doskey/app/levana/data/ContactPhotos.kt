package ninja.doskey.app.levana.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Decodes a contact thumbnail on [dispatcher]. A missing or unreadable photo
 * is null (callers show a placeholder); cancellation is rethrown.
 */
suspend fun loadContactPhoto(
    resolver: ContentResolver,
    photoUri: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Bitmap? = try {
    runCancellable(dispatcher) { signal ->
        signal.throwIfCanceled()
        resolver.openInputStream(Uri.parse(photoUri))?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}
