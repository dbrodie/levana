package ninja.doskey.app.levana.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

/**
 * A preferences DataStore over its own file. The app's DataStores are process-wide singletons
 * that outlive one Robolectric test, so tests that depend on their contents use these.
 */
internal class TestDataStore(val file: File = newFile("store")) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })

    /** Ends this instance, as a process exit would; reopen the file with [reopen]. */
    fun close() {
        scope.cancel()
        // A cancelled DataStore releases its file once its scope has completed
        runBlocking { scope.coroutineContext[kotlinx.coroutines.Job]!!.join() }
    }

    fun reopen(): TestDataStore {
        close()
        return TestDataStore(file)
    }

    private companion object {
        fun newFile(name: String): File =
            File(Files.createTempDirectory("levana-test").toFile(), "$name.preferences_pb")
    }
}

internal fun testDataStore(): DataStore<Preferences> = TestDataStore().store
