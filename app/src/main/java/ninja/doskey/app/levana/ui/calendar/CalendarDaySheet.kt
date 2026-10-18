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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.coroutines.launch
import ninja.doskey.app.levana.ui.daydetail.DayDetailContent
import ninja.doskey.app.levana.ui.daydetail.DayDetailState

/** Native M3 anchors, drag semantics and nested scrolling keep the day sheet predictable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarDaySheet(
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
    LaunchedEffect(selectedDate) {
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
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val gridHeight = maxOf(maxHeight, (320 * fontScale.coerceAtLeast(1f)).dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                calendar(Modifier.fillMaxWidth().height(gridHeight))
            }
        }
    }
}
