package ninja.doskey.app.levana.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Reads and writes the documents chosen for event import/export. All stream
 * work runs on [ioDispatcher] in chunks, checking for cancellation between
 * them.
 */
open class EventFileRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        /** Largest import accepted; real exports are a few kilobytes. */
        const val MAX_IMPORT_BYTES = 1024 * 1024
        private const val CHUNK_BYTES = 8 * 1024
    }

    class FileTooLargeException(val maxBytes: Int) :
        IOException("File is larger than $maxBytes bytes")

    open suspend fun writeText(uri: Uri, content: String) {
        runCancellable(ioDispatcher) { signal ->
            val bytes = content.toByteArray(Charsets.UTF_8)
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw IOException("Could not open output file")
            stream.use {
                var offset = 0
                while (offset < bytes.size) {
                    signal.throwIfCanceled()
                    val count = minOf(CHUNK_BYTES, bytes.size - offset)
                    it.write(bytes, offset, count)
                    offset += count
                }
            }
        }
    }

    /**
     * The document's UTF-8 text, or null when it cannot be opened. Throws
     * [FileTooLargeException] past [maxBytes] without reading the rest.
     */
    open suspend fun readText(uri: Uri, maxBytes: Int = MAX_IMPORT_BYTES): String? =
        runCancellable(ioDispatcher) { signal ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(CHUNK_BYTES)
                while (true) {
                    signal.throwIfCanceled()
                    val read = stream.read(buffer)
                    if (read < 0) break
                    if (out.size() + read > maxBytes) throw FileTooLargeException(maxBytes)
                    out.write(buffer, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            }
        }
}
