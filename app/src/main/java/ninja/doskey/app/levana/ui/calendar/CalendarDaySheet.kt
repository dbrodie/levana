package ninja.doskey.app.levana.ui.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import ninja.doskey.app.levana.R
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.ui.panes.DayPanel
import ninja.doskey.app.levana.ui.panes.ListDetailPanes
import ninja.doskey.app.levana.ui.panes.PaneLayout
import java.time.LocalDate
import kotlinx.coroutines.launch
import ninja.doskey.app.levana.ui.daydetail.DayDetailContent
import ninja.doskey.app.levana.ui.daydetail.DayDetailState

/**
 * The month and the selected day's details: on a phone-sized window the day sheet under the
 * grid ([CalendarDaySheet]); with [panes], the grid and the day panel side by side under one
 * full-width [header] (TASK-52.6).
 */
@Composable
internal fun CalendarDayLayout(
    state: DayDetailState,
    selectedDate: LocalDate,
    onShowZmanim: (LocalDate) -> Unit,
    panes: PaneLayout?,
    modifier: Modifier,
    header: @Composable () -> Unit,
    calendar: @Composable (Modifier) -> Unit,
) {
    if (panes == null) {
        CalendarDaySheet(state, selectedDate, onShowZmanim, modifier, header, calendar)
        return
    }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        header()
        ListDetailPanes(
            panes = panes,
            modifier = Modifier.weight(1f),
            list = { ScrollingGrid(Modifier.fillMaxSize(), calendar) },
            detail = { DayPanel(state, selectedDate, onShowZmanim) },
        )
    }
}

/** The grid fills [modifier]'s height, but never below a readable minimum; then it scrolls. */
@Composable
private fun ScrollingGrid(modifier: Modifier, calendar: @Composable (Modifier) -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier) {
        val gridHeight = maxOf(maxHeight, (320 * fontScale.coerceAtLeast(1f)).dp)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            calendar(Modifier.fillMaxWidth().height(gridHeight))
        }
    }
}

/** Native M3 anchors, drag semantics and nested scrolling keep the day sheet predictable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDaySheet(
    state: DayDetailState,
    selectedDate: LocalDate,
    onShowZmanim: (LocalDate) -> Unit,
    modifier: Modifier,
    header: @Composable () -> Unit,
    calendar: @Composable (Modifier) -> Unit,
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val fontScale = LocalDensity.current.fontScale
    val hasBadges = state.dayInfo?.let { it.holidays.isNotEmpty() || it.shabbatInfo?.isShabbat == true } == true
    val peekHeight = (136 + (if (hasBadges) 38 else 0) + (fontScale - 1).coerceAtLeast(0f) * 100).dp
    // A newly selected day starts collapsed at the top; a fold or rotation keeps the sheet as it was
    var sheetDate by rememberSaveable { mutableStateOf(selectedDate) }
    LaunchedEffect(selectedDate) {
        if (selectedDate == sheetDate) return@LaunchedEffect
        sheetDate = selectedDate
        scrollState.scrollTo(0)
        scaffoldState.bottomSheetState.partialExpand()
    }
    BackHandler(scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
    }
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        topBar = header,
        containerColor = MaterialTheme.colorScheme.background,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetPeekHeight = peekHeight,
        // The full window width: below 840dp (e.g. the Fold8 inner display upright) rather than
        // Material's 640dp cap with gutters; phones are narrower than the cap anyway (TASK-52.6)
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = {
            val expanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
            val actionLabel = stringResource(if (expanded) R.string.sheet_collapse else R.string.sheet_expand)
            Box(
                Modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = actionLabel }.clickable(role = Role.Button, onClickLabel = actionLabel) {
                    scope.launch {
                        if (expanded) scaffoldState.bottomSheetState.partialExpand()
                        else scaffoldState.bottomSheetState.expand()
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(36.dp, 4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(2.dp)))
            }
        },
        sheetShadowElevation = 4.dp,
        sheetTonalElevation = 0.dp,
        sheetContent = {
            DayDetailContent(
                state = state,
                onShowAllZmanim = onShowZmanim,
                scrollState = scrollState,
                modifier = Modifier.fillMaxHeight(.92f),
            )
        },
    ) { padding ->
        ScrollingGrid(Modifier.fillMaxSize().padding(padding), calendar)
    }
}
