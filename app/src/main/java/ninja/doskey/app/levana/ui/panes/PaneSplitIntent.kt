package ninja.doskey.app.levana.ui.panes

import ninja.doskey.app.levana.domain.model.PaneStop

sealed interface PaneSplitIntent {
    /** A drag released near [stop]; it also shows a hidden panel. */
    data class SetStop(val stop: PaneStop) : PaneSplitIntent
    /** A drag released at the edge, or the TalkBack action: hides the day panel. */
    data object Collapse : PaneSplitIntent
    /** Shows the hidden panel at its last stop. */
    data object Expand : PaneSplitIntent
    /** Double tap: shows a hidden panel, otherwise moves to the next stop. */
    data object Cycle : PaneSplitIntent
}
