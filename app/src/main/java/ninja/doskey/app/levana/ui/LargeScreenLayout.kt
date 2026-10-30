package ninja.doskey.app.levana.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The widest a single column of content grows (TASK-52.2). Wider windows, such as a foldable's
 * inner display, centre it; the day panel's bottom sheet has the same Material cap. Phone
 * windows are narrower, so nothing on a phone changes.
 */
internal val ContentMaxWidth = 640.dp

/**
 * Centres the content in at most [max] of the available width. At or below [max] it measures
 * exactly as without this modifier. Put it after a `verticalScroll` so the gutters scroll too.
 */
internal fun Modifier.contentWidth(max: Dp = ContentMaxWidth): Modifier = layout { measurable, constraints ->
    val cap = max.roundToPx()
    if (!constraints.hasBoundedWidth || constraints.maxWidth <= cap) {
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    } else {
        val placeable = measurable.measure(constraints.copy(minWidth = constraints.minWidth.coerceAtMost(cap), maxWidth = cap))
        val width = constraints.maxWidth
        layout(width, placeable.height) { placeable.place((width - placeable.width) / 2, 0) }
    }
}

/**
 * [contentWidth] for lazy lists, which take the cap as content padding so the gutters scroll:
 * [content] receives the extra horizontal padding for this width, zero on a phone.
 */
@Composable
internal fun ContentWidthBox(modifier: Modifier = Modifier, content: @Composable BoxWithConstraintsScope.(gutter: Dp) -> Unit) {
    BoxWithConstraints(modifier) { content(((maxWidth - ContentMaxWidth) / 2).coerceAtLeast(0.dp)) }
}

/**
 * The largest font scale for text in fixed-size cells (the month grid, the date picker) and the
 * walkthrough's illustrations. Beyond it, such text overlaps or clips; the same dates and
 * labels are readable at full size elsewhere (day panel, content descriptions).
 */
internal const val CellMaxFontScale = 1.3f

/** Caps the font scale of [content] at [max]; at or below it, nothing changes. */
@Composable
internal fun CappedFontScale(max: Float = CellMaxFontScale, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    if (density.fontScale <= max) {
        content()
    } else {
        CompositionLocalProvider(LocalDensity provides Density(density.density, max), content = content)
    }
}
