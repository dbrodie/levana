package ninja.doskey.app.levana.ui.panes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ninja.doskey.app.levana.data.PaneLayoutRepository
import ninja.doskey.app.levana.domain.model.PaneSplit

/**
 * The divider between the list and the day panel on wide windows (TASK-52.6). Stored, so it
 * is the same on Month and Agenda and survives folding and process death.
 */
class PaneSplitViewModel(private val repository: PaneLayoutRepository) : ViewModel() {

    private val _state = MutableStateFlow(PaneSplitState())
    val state: StateFlow<PaneSplitState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.split.collect { _state.value = PaneSplitState(it, loaded = true) }
        }
    }

    fun onIntent(intent: PaneSplitIntent) {
        val split = _state.value.split
        val next = when (intent) {
            is PaneSplitIntent.SetStop -> PaneSplit(intent.stop, collapsed = false)
            is PaneSplitIntent.Collapse -> split.copy(collapsed = true)
            is PaneSplitIntent.Expand -> split.copy(collapsed = false)
            is PaneSplitIntent.Cycle -> if (split.collapsed) split.copy(collapsed = false) else PaneSplit(split.stop.next())
        }
        if (next == split) return
        // Shown at once; the store follows
        _state.value = PaneSplitState(next, loaded = _state.value.loaded)
        viewModelScope.launch { repository.save(next) }
    }
}
