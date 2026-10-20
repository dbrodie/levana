package ninja.doskey.app.levana.notifications

import android.content.Context
import android.util.Log
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.ObservanceEnd
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.activeLocation
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** The preferences that decide which reminder alarms are pending and when they fire. */
internal data class AlarmSettings(
    val notifyCandleLighting: Boolean,
    val candleLightingNotifyMode: String,
    val candleLightingMorningTime: Int,
    val candleLightingHoursBefore: Int,
    val candleLightingOffset: Double,
    val notifyOmer: Boolean,
    val notifyOmerTzait: Boolean,
    val notifyOmerMorning: Boolean,
    val notifyOmerMorningTime: Int,
    val location: Location,
    val isInIsrael: Boolean,
    val devDateOverride: LocalDate?
)

internal fun UserPreferences.alarmSettings() = AlarmSettings(
    notifyCandleLighting = notifyCandleLighting,
    candleLightingNotifyMode = candleLightingNotifyMode,
    candleLightingMorningTime = candleLightingMorningTime,
    candleLightingHoursBefore = candleLightingHoursBefore,
    candleLightingOffset = candleLightingOffset,
    notifyOmer = notifyOmer,
    notifyOmerTzait = notifyOmerTzait,
    notifyOmerMorning = notifyOmerMorning,
    notifyOmerMorningTime = notifyOmerMorningTime,
    location = activeLocation,
    isInIsrael = isInIsrael,
    devDateOverride = devDateOverride
)

/**
 * Calls [rebuild] each time a setting that affects reminder alarms changes. The value present
 * when collection starts is skipped; unrelated preference changes are ignored.
 */
internal suspend fun rebuildOnAlarmSettingsChange(
    preferences: Flow<UserPreferences>,
    rebuild: suspend () -> Unit
) {
    preferences
        .map { it.alarmSettings() }
        .distinctUntilChanged()
        .drop(1)
        .conflate()
        .collect { rebuild() }
}

/** Whether an alarm with [action] should still be delivered under [prefs]. */
internal fun isAlarmEnabled(action: String?, prefs: UserPreferences): Boolean = when (action) {
    NotificationAlarmScheduler.ACTION_CANDLE_LIGHTING -> prefs.notifyCandleLighting
    NotificationAlarmScheduler.ACTION_OMER -> prefs.notifyOmer && prefs.notifyOmerTzait
    NotificationAlarmScheduler.ACTION_OMER_MORNING -> prefs.notifyOmer && prefs.notifyOmerMorning
    else -> false
}

data class CandleLightingTimes(val candleLighting: LocalTime, val end: ObservanceEnd?)

/**
 * Owns the pending candle-lighting and Omer alarms: [rebuild] replaces them with the ones the
 * current preferences call for. It never posts a notification itself.
 */
class NotificationAlarmRebuilder(
    private val preferences: Flow<UserPreferences>,
    private val zmanimRepository: ZmanimRepository,
    private val calendarRepository: CalendarRepository,
    private val alarmScheduler: NotificationAlarmScheduler
) {
    private val mutex = Mutex()

    /** Cancels every pending reminder alarm and schedules today's for the enabled categories. */
    suspend fun rebuild() = mutex.withLock {
        val prefs = preferences.first()
        val today = prefs.devDateOverride ?: LocalDate.now()
        alarmScheduler.cancelAll()
        if (prefs.notifyCandleLighting) scheduleCandleLighting(today, prefs)
        if (prefs.notifyOmer) scheduleOmer(today, prefs)
    }

    /** Today's candle-lighting and end times, or null when no candles are lit [date]. */
    fun candleLightingTimes(date: LocalDate, prefs: UserPreferences): CandleLightingTimes? {
        if (!zmanimRepository.hasCandles(date, prefs.isInIsrael)) return null
        val location = prefs.activeLocation
        val candleLighting = zmanimRepository
            .getShabbatInfo(date, location, prefs.candleLightingOffset, prefs.isInIsrael)
            .candleLightingTime ?: return null
        val end = zmanimRepository.getObservanceEnd(date, location, prefs.isInIsrael)
        return CandleLightingTimes(candleLighting, end)
    }

    private fun scheduleCandleLighting(today: LocalDate, prefs: UserPreferences) {
        val times = candleLightingTimes(today, prefs) ?: return
        val triggerTime = when (prefs.candleLightingNotifyMode) {
            "morning" -> minutesToTime(prefs.candleLightingMorningTime)
            "hours_before" ->
                times.candleLighting.minusHours(prefs.candleLightingHoursBefore.toLong())
            else -> return
        }
        // The scheduler skips trigger times that have already passed
        alarmScheduler.scheduleCandleLighting(
            today,
            triggerTime,
            prefs.activeLocation,
            times.candleLighting,
            times.end
        )
    }

    private fun scheduleOmer(today: LocalDate, prefs: UserPreferences) {
        val location = prefs.activeLocation
        val plan = planOmerReminders(
            today, prefs.notifyOmerTzait, prefs.notifyOmerMorning, calendarRepository
        ) { zmanimRepository.getTzaitTime(today, location) }
        plan.evening?.let { evening ->
            alarmScheduler.scheduleOmerReminder(today, evening.tzaitTime, location, evening.omerDay)
        }
        plan.morningOmerDay?.let { omerDay ->
            alarmScheduler.scheduleOmerMorningReminder(
                today, minutesToTime(prefs.notifyOmerMorningTime), omerDay
            )
        }
    }

}

internal fun minutesToTime(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)

/**
 * Keeps pending reminder alarms, and the daily categories' notifications, in step with the
 * settings for the life of the process.
 */
object NotificationRescheduling : KoinComponent {
    private const val TAG = "NotificationRescheduling"

    private val preferencesRepository: PreferencesRepository by inject()
    private val alarmRebuilder: NotificationAlarmRebuilder by inject()
    private val context: Context by inject()

    fun start(scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) {
        scope.launch {
            rebuildOnAlarmSettingsChange(preferencesRepository.preferences) {
                logFailure("rebuild reminder alarms") { alarmRebuilder.rebuild() }
            }
        }
        scope.launch {
            refreshOnDailySettingsChange(
                preferencesRepository.preferences,
                refresh = { categories ->
                    logFailure("refresh $categories") {
                        DailyNotificationWorker.enqueueRefresh(context, categories)
                    }
                },
                disabled = { categories ->
                    logFailure("cancel $categories") {
                        DailyNotificationWorker.cancelRefresh(context, categories)
                    }
                }
            )
        }
    }

    // Keep observing after a failure; the next change or daily run tries again
    private suspend fun logFailure(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to $what", e)
        }
    }
}
