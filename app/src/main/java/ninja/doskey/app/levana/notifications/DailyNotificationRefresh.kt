package ninja.doskey.app.levana.notifications

import androidx.work.Data
import ninja.doskey.app.levana.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Notification categories posted directly by [DailyNotificationWorker] rather than by alarms. */
enum class DailyCategory {
    HOLIDAYS,
    FASTS,
    PERSONAL_EVENTS;

    fun isEnabled(prefs: UserPreferences): Boolean = when (this) {
        HOLIDAYS -> prefs.notifyHolidays
        FASTS -> prefs.notifyFasts
        PERSONAL_EVENTS -> prefs.notifyPersonalEvents
    }

    /** The settings this category's notifications are calculated from, or null while it is off. */
    internal fun settings(prefs: UserPreferences): Any? {
        if (!isEnabled(prefs)) return null
        return when (this) {
            HOLIDAYS -> listOf(prefs.holidayNotifyDaysBefore, prefs.isInIsrael, prefs.devDateOverride)
            // Fast times are at the device position; the selected city does not matter
            FASTS -> listOf(prefs.isInIsrael, prefs.currentLocation, prefs.devDateOverride)
            PERSONAL_EVENTS -> listOf(prefs.yahrzeitRules, prefs.devDateOverride)
        }
    }

    companion object {
        private const val KEY_CATEGORIES = "categories"

        fun toInputData(categories: Set<DailyCategory>): Data = Data.Builder()
            .putStringArray(KEY_CATEGORIES, categories.map { it.name }.toTypedArray())
            .build()

        /** The categories a worker run was asked to refresh; null for a full daily run. */
        fun fromInputData(data: Data): Set<DailyCategory>? =
            data.getStringArray(KEY_CATEGORIES)
                ?.mapNotNull { name -> entries.firstOrNull { it.name == name } }
                ?.toSet()
    }
}

/**
 * Watches each [DailyCategory]'s settings. When an enabled category's settings change (switching
 * it on included) [refresh] gets just those categories; categories switched off go to [disabled].
 * The value present when collection starts is skipped; unrelated changes are ignored.
 */
internal suspend fun refreshOnDailySettingsChange(
    preferences: Flow<UserPreferences>,
    refresh: suspend (Set<DailyCategory>) -> Unit,
    disabled: suspend (Set<DailyCategory>) -> Unit = {}
) {
    var previous: Map<DailyCategory, Any?>? = null
    preferences
        .map { prefs -> DailyCategory.entries.associateWith { it.settings(prefs) } }
        .distinctUntilChanged()
        .conflate()
        .collect { current ->
            val before = previous
            previous = current
            if (before == null) return@collect
            val changed = DailyCategory.entries.filter { current[it] != before[it] }
            val (on, off) = changed.partition { current[it] != null }
            if (off.isNotEmpty()) disabled(off.toSet())
            if (on.isNotEmpty()) refresh(on.toSet())
        }
}
