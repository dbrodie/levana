package ninja.doskey.app.levana.ui.panes

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import ninja.doskey.app.levana.ui.daydetail.DayDetailContent
import ninja.doskey.app.levana.ui.daydetail.DayDetailState
import org.koin.androidx.compose.koinViewModel

/**
 * The divider's state when the window shows two panes (TASK-52.6), or null for the phone
 * layout. Month and Agenda each have a [PaneSplitViewModel]; both follow the same stored
 * position.
 */
@Composable
internal fun rememberPaneLayout(viewModel: PaneSplitViewModel = koinViewModel()): PaneLayout? {
    if (!isTwoPaneWindow()) return null
    val state by viewModel.state.collectAsStateWithLifecycle()
    return PaneLayout(state.split, state.loaded, viewModel::onIntent)
}

/**
 * The day panel beside the list: the expanded day sheet's content as it is, for [date], without
 * the sheet's drag handle. A newly shown day starts at the top; a fold or rotation keeps the
 * place, as the sheet does.
 */
@Composable
internal fun DayPanel(
    state: DayDetailState,
    date: LocalDate,
    onShowZmanim: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var shownDate by rememberSaveable { mutableStateOf(date) }
    LaunchedEffect(date) {
        if (date == shownDate) return@LaunchedEffect
        shownDate = date
        scrollState.scrollTo(0)
    }
    Box(
        modifier
            .fillMaxSize()
            .padding(end = 12.dp, bottom = 12.dp)
            .clip(PanelShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(top = 8.dp)
    ) {
        DayDetailContent(state = state, onShowAllZmanim = onShowZmanim, scrollState = scrollState)
    }
}

/** The sheet's corner radius, all round: the panel stands free of the screen's edges. */
private val PanelShape = RoundedCornerShape(28.dp)
