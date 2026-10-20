package ninja.doskey.app.levana.ui.birthday

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import ninja.doskey.app.levana.data.loadContactPhoto

/**
 * A contact's thumbnail, decoded off the main thread; null until loaded or
 * when there is none. Loading is cancelled when the caller leaves composition.
 */
@Composable
fun rememberContactPhoto(photoUri: String?): State<ImageBitmap?> {
    val resolver = LocalContext.current.contentResolver
    return produceState<ImageBitmap?>(initialValue = null, photoUri) {
        value = photoUri?.let { loadContactPhoto(resolver, it)?.asImageBitmap() }
    }
}
