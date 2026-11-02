package ninja.doskey.app.levana.ui.panes

import ninja.doskey.app.levana.domain.model.PaneSplit

/** [split] is the stored divider position, the default until the store has answered ([loaded]). */
data class PaneSplitState(val split: PaneSplit = PaneSplit(), val loaded: Boolean = false)
