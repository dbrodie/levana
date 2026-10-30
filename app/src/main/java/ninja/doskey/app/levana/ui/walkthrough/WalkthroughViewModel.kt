package ninja.doskey.app.levana.ui.walkthrough

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ninja.doskey.app.levana.data.LocationService
import ninja.doskey.app.levana.data.PreferencesRepository
import org.koin.androidx.compose.koinViewModel

/**
 * The feature walkthrough (TASK-43), scoped to the activity: the app shell starts it and
 * About replays it. A screen counts as seen once displayed; Skip marks the whole run seen.
 */
class WalkthroughViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val locationService: LocationService,
    private val gate: WalkthroughGate,
    private val device: WalkthroughDevice,
    private val registry: List<FeatureScreen> = WalkthroughRegistry.screens,
    // DataStore runs an edit in the caller's context; off the main thread, a write never waits
    // for it (a test blocking the main thread on its own edit would otherwise deadlock)
    private val writeDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    // Without the gate there is nothing to decide, so the shell shows from the first frame
    private val _state = MutableStateFlow(WalkthroughState(decided = !gate.showOnLaunch))
    val state: StateFlow<WalkthroughState> = _state.asStateFlow()

    private var started = false

    fun onIntent(intent: WalkthroughIntent) {
        when (intent) {
            is WalkthroughIntent.Start -> start(intent.deepLink)
            WalkthroughIntent.Replay -> replay()
            is WalkthroughIntent.PageShown -> showPage(intent.index)
            WalkthroughIntent.Next, WalkthroughIntent.NotNow -> next()
            WalkthroughIntent.Back -> showPage(_state.value.page - 1)
            WalkthroughIntent.Skip, WalkthroughIntent.Finish -> finish()
            is WalkthroughIntent.PermissionResult -> permissionResult(intent.permission, intent.granted)
            WalkthroughIntent.RefreshPermissions -> _state.update { it.copy(permissions = permissions(it.permissions)) }
            WalkthroughIntent.AddCity -> _state.update { it.copy(addingCity = true) }
            WalkthroughIntent.CloseCityPicker -> _state.update { it.copy(addingCity = false) }
        }
    }

    /** Once per activity: a configuration change keeps the run and its page. */
    private fun start(deepLink: Boolean) {
        if (started) return
        started = true
        if (!gate.showOnLaunch) return
        viewModelScope.launch {
            val seen = preferencesRepository.walkthroughSeenIds.first()
            val run = WalkthroughSelection.onLaunch(registry, seen, deepLink, device::isAvailable)
            _state.value = WalkthroughState(
                decided = true,
                screens = run,
                permissions = permissions(emptyMap()),
                shownOnLaunch = run.isNotEmpty(),
            )
            markSeen(run.take(1))
        }
    }

    private fun replay() {
        _state.update {
            it.copy(
                screens = WalkthroughSelection.replay(registry, device::isAvailable),
                replay = true,
                page = 0,
                permissions = permissions(it.permissions),
                addingCity = false,
            )
        }
    }

    private fun showPage(index: Int) {
        val current = _state.value
        if (index !in current.screens.indices || index == current.page) return
        _state.value = current.copy(page = index)
        markSeen(listOf(current.screens[index]))
    }

    private fun next() {
        val current = _state.value
        if (current.isLastPage) finish() else showPage(current.page + 1)
    }

    private fun finish() {
        val current = _state.value
        if (!current.showing) return
        markSeen(current.screens)
        _state.value = current.copy(screens = emptyList(), replay = false, page = 0, addingCity = false)
    }

    private fun markSeen(screens: List<FeatureScreen>) {
        if (_state.value.replay || screens.isEmpty()) return
        val ids = screens.map { it.id }
        viewModelScope.launch(writeDispatcher) { preferencesRepository.markWalkthroughSeen(ids) }
    }

    private fun permissionResult(permission: WalkthroughPermission, granted: Boolean) {
        val status = if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
        _state.update { it.copy(permissions = it.permissions + (permission to status)) }
        // As in Settings → Locations: a grant brings in the current location right away
        if (granted && permission == WalkthroughPermission.LOCATION) {
            viewModelScope.launch {
                locationService.getCurrentLocationOrNull()?.let { preferencesRepository.updateCurrentLocation(it) }
            }
        }
    }

    /** The device's grants; a refusal in this run stays shown as refused until granted. */
    private fun permissions(previous: Map<WalkthroughPermission, PermissionStatus>) =
        WalkthroughPermission.entries.associateWith { permission ->
            when {
                device.isGranted(permission) -> PermissionStatus.GRANTED
                previous[permission] == PermissionStatus.DENIED -> PermissionStatus.DENIED
                else -> PermissionStatus.NOT_GRANTED
            }
        }
}

/**
 * The activity's walkthrough, also from screens inside the navigation graph (About's replay),
 * whose own view-model owner is their back-stack entry.
 */
@Composable
fun activityWalkthroughViewModel(): WalkthroughViewModel {
    var context = LocalContext.current
    while (context !is ComponentActivity && context is ContextWrapper) context = context.baseContext
    return koinViewModel(viewModelStoreOwner = context as ComponentActivity)
}
