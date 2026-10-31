package ninja.doskey.app.levana.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.UserPreferences

/**
 * The app shell's state that outlives a configuration change (TASK-52.3), scoped to the
 * activity: folding, unfolding, rotating or resizing recreates MainActivity but keeps this.
 *
 * A recreated activity draws its first frame with the [preferences] and [today] it had, rather
 * than an empty shell in the system theme until DataStore answers.
 */
class ShellViewModel(
    preferencesRepository: PreferencesRepository,
    deviceContext: DeviceContext,
) : ViewModel() {

    /** Null until first read; then the latest, also on a recreated activity's first frame. */
    val preferences: StateFlow<UserPreferences?> =
        preferencesRepository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** The device's today (midnight, clock and zone changes), kept the same way. */
    val today: StateFlow<LocalDate?> =
        deviceContext.todayUpdates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private var lastLocationRefresh: Long? = null
    private var stoppedForConfigurationChange = false

    /**
     * Whether the activity resuming at [nowMillis] (elapsed realtime) should refresh the current
     * location: on opening and on a return to the app, but not when a configuration change
     * recreated it, and not again within [LOCATION_REFRESH_INTERVAL_MS]. A fold can also stop
     * the activity behind the "Swipe up to continue" screen before it is recreated.
     */
    fun shouldRefreshLocation(nowMillis: Long): Boolean {
        val last = lastLocationRefresh
        val refresh = last == null ||
            (!stoppedForConfigurationChange && nowMillis - last >= LOCATION_REFRESH_INTERVAL_MS)
        stoppedForConfigurationChange = false
        if (refresh) lastLocationRefresh = nowMillis
        return refresh
    }

    /** The activity stopped; [changingConfigurations] when it is about to be recreated. */
    fun onStop(changingConfigurations: Boolean) {
        stoppedForConfigurationChange = changingConfigurations
    }

    companion object {
        /** Keeps the flows across a recreation, which resubscribes within a frame or two. */
        private const val STOP_TIMEOUT_MS = 5_000L
        const val LOCATION_REFRESH_INTERVAL_MS = 60_000L
    }
}
