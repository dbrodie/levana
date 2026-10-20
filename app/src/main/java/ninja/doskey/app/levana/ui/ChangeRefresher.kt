package ninja.doskey.app.levana.ui

import ninja.doskey.app.levana.data.EventDataChanges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Runs [refresh] when [EventDataChanges] signals a change: immediately while the screen is
 * resumed, otherwise once on its next resume. Collection lives in [scope] (a viewModelScope),
 * so the underlying observers are released when the view model is cleared.
 */
class ChangeRefresher(
    scope: CoroutineScope,
    private val eventDataChanges: EventDataChanges,
    debounceMillis: Long = DEBOUNCE_MILLIS,
    private val refresh: () -> Unit
) {
    // Only touched from the collecting coroutine and lifecycle callbacks, both on the main thread.
    private var resumed = true
    private var pending = false

    init {
        @OptIn(FlowPreview::class)
        scope.launch {
            eventDataChanges.changes.debounce(debounceMillis).collect {
                if (resumed) refresh() else pending = true
            }
        }
    }

    fun onResume() {
        resumed = true
        // A permission change is delivered through `changes`, refreshing shortly after.
        eventDataChanges.recheckPermissions()
        if (pending) {
            pending = false
            refresh()
        }
    }

    fun onPause() {
        resumed = false
    }

    companion object {
        const val DEBOUNCE_MILLIS = 300L
    }
}
