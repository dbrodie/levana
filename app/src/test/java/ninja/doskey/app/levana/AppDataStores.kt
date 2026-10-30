package ninja.doskey.app.levana

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.runBlocking

/**
 * The app's own DataStores. Each is a process-wide singleton behind a private `Context`
 * delegate, so every Robolectric test in one JVM shares it, and its contents outlive the test
 * that wrote them. Tests that go through the production stores reset them here instead of
 * depending on what earlier tests left behind.
 *
 * The delegates are private, so they are reached by reflection; a renamed delegate fails here
 * loudly rather than leaving a store uncleared.
 */
internal object AppDataStores {

    /** The user preferences behind PreferencesRepository. */
    fun preferences(context: Context): DataStore<Preferences> =
        store(context, "ninja.doskey.app.levana.data.PreferencesRepositoryKt", "getDataStore")

    /** What NotificationDeliveryLog has recorded as posted. */
    fun deliveryLog(context: Context): DataStore<Preferences> = store(
        context, "ninja.doskey.app.levana.notifications.NotificationDeliveryLogKt", "getDeliveryDataStore"
    )

    /** Empties the user preferences. Clearing does not rerun the store's migrations. */
    fun clearPreferences(context: Context) = clear(preferences(context))

    /** Empties the notification delivery log. */
    fun clearDeliveryLog(context: Context) = clear(deliveryLog(context))

    private fun clear(store: DataStore<Preferences>) {
        runBlocking { store.edit { it.clear() } }
    }

    @Suppress("UNCHECKED_CAST")
    private fun store(context: Context, fileClass: String, getter: String): DataStore<Preferences> =
        Class.forName(fileClass)
            .getDeclaredMethod(getter, Context::class.java)
            .apply { isAccessible = true }
            .invoke(null, context.applicationContext) as DataStore<Preferences>
}
