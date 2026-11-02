package ninja.doskey.app.levana.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ninja.doskey.app.levana.domain.model.PaneSplit
import ninja.doskey.app.levana.domain.model.PaneStop

/**
 * A store of its own, so moving the divider does not re-emit the user preferences that most
 * screens and the notification scheduler follow.
 */
private val Context.paneLayoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "pane_layout")

/** Where the two-pane divider of Month and Agenda sits (TASK-52.6); one position for both. */
class PaneLayoutRepository(private val context: Context) {

    private object Keys {
        val STOP = stringPreferencesKey("divider_stop")
        val COLLAPSED = booleanPreferencesKey("panel_collapsed")
    }

    val split: Flow<PaneSplit> = context.paneLayoutDataStore.data.map { prefs ->
        PaneSplit(
            stop = PaneStop.entries.firstOrNull { it.name == prefs[Keys.STOP] } ?: PaneSplit().stop,
            collapsed = prefs[Keys.COLLAPSED] ?: false,
        )
    }.distinctUntilChanged()

    suspend fun save(split: PaneSplit) {
        context.paneLayoutDataStore.edit { prefs ->
            prefs[Keys.STOP] = split.stop.name
            prefs[Keys.COLLAPSED] = split.collapsed
        }
    }
}
