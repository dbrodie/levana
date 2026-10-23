package ninja.doskey.app.levana.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.first

// Kept apart from the user preferences so alarm bookkeeping never looks like a settings change
private val Context.scheduledAlarmDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "scheduled_alarms"
)

/** A reminder alarm's identity: its action and source date (see [NotificationAlarmScheduler.alarmUri]). */
internal data class AlarmId(val action: String, val date: LocalDate)

/**
 * The reminder alarms that may still be pending. AlarmManager cannot list pending alarms, so
 * each identity is recorded when scheduled and removed once cancelled; reconciliation can then
 * cancel unwanted alarms on any date, not only in the current planning window. Entries for
 * alarms that already fired, or were cleared by a reboot, are harmless: cancelling them is a
 * no-op, and they are removed when no longer wanted.
 */
internal class ScheduledAlarmRegistry(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.scheduledAlarmDataStore)

    suspend fun ids(): Set<AlarmId> =
        dataStore.data.first()[IDS].orEmpty().mapNotNull(::parse).toSet()

    suspend fun add(ids: Collection<AlarmId>) {
        if (ids.isEmpty()) return
        dataStore.edit { it[IDS] = it[IDS].orEmpty() + ids.map(::format) }
    }

    suspend fun remove(ids: Collection<AlarmId>) {
        if (ids.isEmpty()) return
        dataStore.edit { it[IDS] = it[IDS].orEmpty() - ids.map(::format).toSet() }
    }

    /** Whether alarms set before this registry existed have already been looked for. */
    suspend fun isSwept(): Boolean = dataStore.data.first()[SWEPT] == true

    suspend fun markSwept() {
        dataStore.edit { it[SWEPT] = true }
    }

    private companion object {
        val IDS = stringSetPreferencesKey("alarm_ids")
        val SWEPT = booleanPreferencesKey("legacy_alarms_swept")

        fun format(id: AlarmId) = "${id.action}|${id.date.toEpochDay()}"

        fun parse(value: String): AlarmId? {
            val action = value.substringBeforeLast('|', "")
            val epochDay = value.substringAfterLast('|').toLongOrNull()
            if (action.isEmpty() || epochDay == null) return null
            return AlarmId(action, LocalDate.ofEpochDay(epochDay))
        }
    }
}
