package ninja.doskey.app.levana.ui.panes

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.PaneSplit
import ninja.doskey.app.levana.domain.model.PaneStop

/** The gutter between the panes, which holds the divider's handle. */
internal val PaneGutter = 16.dp
/** The list or month grid never gets narrower than a phone. */
internal val MinListWidth = 360.dp
/** Nor the day panel narrower than this, unless it is hidden. */
internal val MinPanelWidth = 300.dp
/** The divider also takes drags this far over the panel's edge, inside its padding. */
private val DividerReach = 16.dp

/** The height around the handle where a drag moves the divider rather than going back. */
private val HandleExclusionHeight = 160.dp

/** The divider's test tag. */
const val PANE_DIVIDER_TAG = "pane_divider"

/**
 * Whether the window is wide enough for the list and the day panel side by side: Material's
 * "expanded" width, 840dp and up (TASK-52.6). Narrower windows keep the phone layout.
 */
@Composable
internal fun isTwoPaneWindow(): Boolean =
    currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

/**
 * The divider's state and its intents, for a screen showing two panes. Until [loaded], [split]
 * is the default and the stored one replaces it without animating.
 */
class PaneLayout(val split: PaneSplit, val loaded: Boolean, val onIntent: (PaneSplitIntent) -> Unit)

/** The list's width at [stop] in a window [total] wide, within both panes' minimum sizes. */
internal fun listWidth(stop: PaneStop, total: Dp): Dp =
    (total * stop.listFraction).coerceIn(MinListWidth, (total - PaneGutter - MinPanelWidth).coerceAtLeast(MinListWidth))

/**
 * The list and the day panel side by side, with a draggable divider between them (Option E,
 * TASK-52.6). The divider snaps to the [PaneStop]s, or hides the panel when dragged to the edge;
 * a double tap moves it to the next stop or shows a hidden panel, and TalkBack has the same as
 * actions. Only the gutter (and the panel's padding beside it) takes drags, so a swipe in the
 * list, such as to another month, never moves it. Mirrored right to left.
 */
@Composable
internal fun ListDetailPanes(
    panes: PaneLayout,
    modifier: Modifier = Modifier,
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val split = panes.split
        val total = maxWidth
        val stopWidths = PaneStop.entries.associateWith { with(density) { listWidth(it, total).toPx() } }
        val minPx = with(density) { MinListWidth.toPx() }
        // Collapsed: the list takes everything but the gutter, which stays at the edge
        val fullPx = with(density) { (total - PaneGutter).toPx() }
        val minPanelPx = with(density) { MinPanelWidth.toPx() }
        val targetPx = if (split.collapsed) fullPx else stopWidths.getValue(split.stop)

        val width = remember { Animatable(targetPx) }
        var dragPx by remember { mutableStateOf<Float?>(null) }
        var shownStored by remember { mutableStateOf(false) }
        LaunchedEffect(targetPx, panes.loaded) {
            when {
                dragPx != null -> Unit
                shownStored -> width.animateTo(targetPx)
                else -> {
                    width.snapTo(targetPx)
                    shownStored = panes.loaded
                }
            }
        }
        val scope = rememberCoroutineScope()
        val onIntent by rememberUpdatedState(panes.onIntent)

        val listPx = dragPx ?: width.value
        val listDp = with(density) { listPx.toDp() }
        val panelDp = (total - PaneGutter - listDp).coerceAtLeast(0.dp)

        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(listDp).fillMaxHeight()) { list() }
            Box(Modifier.width(PaneGutter).fillMaxHeight())
            Box(Modifier.width(panelDp).fillMaxHeight().clipToBounds()) {
                if (panelDp > 0.dp) {
                    // Never squeezed below its minimum: while dragged to the edge it slides out
                    Box(
                        Modifier.wrapContentWidth(Alignment.Start, unbounded = true)
                            .requiredWidth(maxOf(panelDp, MinPanelWidth)).fillMaxHeight()
                    ) { detail() }
                }
            }
        }

        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val draggableState = rememberDraggableState { delta ->
            dragPx = ((dragPx ?: width.value) + delta).coerceIn(minPx, fullPx)
        }
        val dividerLabel = stringResource(R.string.pane_divider)
        val percent = (targetPx / with(density) { total.toPx() } * 100).roundToInt()
        val stateLabel = if (split.collapsed) stringResource(R.string.pane_divider_hidden) else stringResource(R.string.pane_divider_state, percent)
        val clickLabel = stringResource(if (split.collapsed) R.string.pane_divider_show else R.string.pane_divider_next)
        val toggleLabel = stringResource(if (split.collapsed) R.string.pane_divider_show else R.string.pane_divider_hide)
        val reach = minOf(DividerReach, panelDp)
        val exclusionPx = with(density) { HandleExclusionHeight.toPx() }
        Box(
            Modifier
                .offset { IntOffset(listPx.roundToInt(), 0) }
                .width(PaneGutter + reach)
                .fillMaxHeight()
                .testTag(PANE_DIVIDER_TAG)
                // Hidden panel: the gutter is at the screen's edge, where a drag would be the
                // system's back gesture. Android exempts at most 200dp per edge, so only the
                // stretch around the handle.
                .systemGestureExclusion { coordinates ->
                    val reachY = exclusionPx / 2
                    val centerY = coordinates.size.height / 2f
                    Rect(0f, centerY - reachY, coordinates.size.width.toFloat(), centerY + reachY)
                }
                .semantics {
                    contentDescription = dividerLabel
                    stateDescription = stateLabel
                    onClick(label = clickLabel) { onIntent(PaneSplitIntent.Cycle); true }
                    customActions = listOf(
                        CustomAccessibilityAction(toggleLabel) {
                            onIntent(if (split.collapsed) PaneSplitIntent.Expand else PaneSplitIntent.Collapse); true
                        }
                    )
                }
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onIntent(PaneSplitIntent.Cycle) }) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    reverseDirection = rtl,
                    onDragStarted = { dragPx = width.value },
                    onDragStopped = {
                        val released = dragPx ?: return@draggable
                        // Released with less than half a panel showing: hide it; otherwise the nearest stop
                        val stop = if (fullPx - released < minPanelPx / 2) null
                        else stopWidths.entries.minBy { abs(it.value - released) }.key
                        val settled = stop?.let(stopWidths::getValue) ?: fullPx
                        onIntent(if (stop == null) PaneSplitIntent.Collapse else PaneSplitIntent.SetStop(stop))
                        scope.launch {
                            width.snapTo(released)
                            dragPx = null
                            width.animateTo(settled)
                        }
                    },
                ),
        ) {
            Box(
                Modifier.width(PaneGutter).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(4.dp, 48.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)))
            }
        }
    }
}
