package ninja.doskey.app.levana.domain.model

/** The divider's stops, as the list pane's share of the window width (TASK-52.6). */
enum class PaneStop(val listFraction: Float) {
    LIST_40(0.40f),
    LIST_50(0.50f),
    LIST_65(0.65f);

    /** The stop a double tap moves to: 40 -> 50 -> 65 -> 40. */
    fun next(): PaneStop = entries[(ordinal + 1) % entries.size]
}

/**
 * The two-pane divider: at [stop], or [collapsed] with the day panel hidden and the list at
 * full width. A collapsed divider still remembers the [stop] it restores.
 */
data class PaneSplit(val stop: PaneStop = PaneStop.LIST_50, val collapsed: Boolean = false)
