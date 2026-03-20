package com.levana.app.ui.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levana.app.domain.model.HebrewDay
import com.levana.app.domain.model.HebrewYearMonth
import com.levana.app.domain.model.HolidayCategory
import com.levana.app.ui.daydetail.DayDetailContent
import com.levana.app.ui.daydetail.DayDetailIntent
import com.levana.app.ui.daydetail.DayDetailViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

private const val PAGER_PAGE_COUNT = 1200
private const val PAGER_INITIAL_PAGE = 600
private val HEBREW_LOCALE = Locale.forLanguageTag("he")

private val DAYS_OF_WEEK = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
)

private fun LazyGridScope.dayHeaderRow(locale: Locale) {
    items(7) { index ->
        Box(
            modifier = Modifier.fillMaxWidth().height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = DAYS_OF_WEEK[index].getDisplayName(TextStyle.SHORT, locale),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun HebrewYearMonth.plusMonths(n: Int): HebrewYearMonth {
    var result = this
    if (n > 0) repeat(n) { result = result.next() }
    else if (n < 0) repeat(-n) { result = result.previous() }
    return result
}

private fun HebrewYearMonth.stepsTo(other: HebrewYearMonth): Int {
    if (this == other) return 0
    var current = this
    for (i in 1..PAGER_INITIAL_PAGE) {
        current = current.next()
        if (current == other) return i
    }
    current = this
    for (i in 1..PAGER_INITIAL_PAGE) {
        current = current.previous()
        if (current == other) return -i
    }
    return 0
}

@Composable
fun CalendarScreen(
    onOpenDrawer: () -> Unit = {},
    onShowZmanim: (LocalDate) -> Unit = {},
    onAddEvent: (Int, Int, Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = koinViewModel(),
    dayDetailViewModel: DayDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dayDetailState by dayDetailViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.selectedDate) {
        dayDetailViewModel.onIntent(DayDetailIntent.LoadDay(state.selectedDate))
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (state.calendarHebrewMode) {
            HebrewCalendarContent(
                state = state,
                onIntent = viewModel::onIntent,
                scrollTarget = viewModel.hebrewScrollTarget,
                onOpenDrawer = onOpenDrawer,
                dayDetailState = dayDetailState,
                onShowZmanim = onShowZmanim,
                onAddEvent = onAddEvent,
                modifier = modifier
            )
        } else {
            GregorianCalendarContent(
                state = state,
                onIntent = viewModel::onIntent,
                scrollTarget = viewModel.gregorianScrollTarget,
                onOpenDrawer = onOpenDrawer,
                dayDetailState = dayDetailState,
                onShowZmanim = onShowZmanim,
                onAddEvent = onAddEvent,
                modifier = modifier
            )
        }
    }

    if (state.showGoToDateDialog) {
        GoToDateDialog(
            initialDate = state.selectedDate,
            calendarHebrewMode = state.calendarHebrewMode,
            onConfirm = { date -> viewModel.onIntent(CalendarIntent.GoToDate(date)) },
            onDismiss = { viewModel.onIntent(CalendarIntent.CloseGoToDateDialog) }
        )
    }
}

@Composable
private fun GregorianCalendarContent(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit,
    scrollTarget: Flow<YearMonth>,
    onOpenDrawer: () -> Unit,
    dayDetailState: com.levana.app.ui.daydetail.DayDetailState,
    onShowZmanim: (LocalDate) -> Unit,
    onAddEvent: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = PAGER_INITIAL_PAGE,
        pageCount = { PAGER_PAGE_COUNT }
    )

    val baseMonth = remember { YearMonth.now() }
    val currentState by rememberUpdatedState(state)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val offset = page - PAGER_INITIAL_PAGE
            val targetMonth = baseMonth.plusMonths(offset.toLong())
            if (targetMonth != currentState.currentMonth) {
                onIntent(CalendarIntent.LoadMonth(targetMonth))
            }
        }
    }

    LaunchedEffect(Unit) {
        scrollTarget.collect { targetMonth ->
            val offset = ((targetMonth.year - baseMonth.year) * 12) +
                (targetMonth.monthValue - baseMonth.monthValue)
            pagerState.animateScrollToPage(PAGER_INITIAL_PAGE + offset)
        }
    }

    val eventsScrollState = rememberScrollState()
    val calendarHeightPx = remember { mutableIntStateOf(0) }
    val calendarHiddenPx = remember { mutableFloatStateOf(0f) }
    val calendarFraction by remember {
        derivedStateOf {
            val h = calendarHeightPx.intValue.toFloat().coerceAtLeast(1f)
            1f - (calendarHiddenPx.floatValue / h).coerceIn(0f, 1f)
        }
    }

    val isResetting = remember { booleanArrayOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) {
                    val h = calendarHeightPx.intValue.toFloat()
                    if (h > 0f) {
                        val canConsume = h - calendarHiddenPx.floatValue
                        if (canConsume > 0f) {
                            val consume = minOf(-available.y, canConsume)
                            calendarHiddenPx.floatValue += consume
                            return Offset(0f, -consume)
                        }
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!isResetting[0] && available.y > 0f && calendarHiddenPx.floatValue > 0f) {
                    val consume = minOf(available.y, calendarHiddenPx.floatValue)
                    calendarHiddenPx.floatValue -= consume
                    return Offset(0f, consume)
                }
                return Offset.Zero
            }
        }
    }

    suspend fun animateExpand() = coroutineScope {
        launch {
            Animatable(calendarHiddenPx.floatValue).animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { calendarHiddenPx.floatValue = value }
        }
        launch {
            eventsScrollState.animateScrollTo(0)
        }
    }

    LaunchedEffect(state.selectedDate) {
        isResetting[0] = true
        try { animateExpand() } finally { isResetting[0] = false }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    isResetting[0] = true
                    try { animateExpand() } finally { isResetting[0] = false }
                }
            }
    }

    val onAddEventClick: () -> Unit = {
        dayDetailState.dayInfo?.hebrewDay?.let { h ->
            onAddEvent(h.day, h.month.jewishDateValue, h.year)
        }
    }

    Column(modifier = modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        GregorianMonthHeader(
            state = state,
            onOpenDrawer = onOpenDrawer,
            onGoToToday = { onIntent(CalendarIntent.GoToToday) },
            onGoToDate = { onIntent(CalendarIntent.OpenGoToDateDialog) },
            onToggleMode = { onIntent(CalendarIntent.ToggleCalendarHebrewMode) },
            onAddEvent = onAddEventClick
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    if (placeable.height > 0 && calendarHeightPx.intValue == 0) {
                        calendarHeightPx.intValue = placeable.height
                    }
                    val hidden = calendarHiddenPx.floatValue.coerceIn(0f, placeable.height.toFloat())
                    val reportedHeight = (placeable.height - hidden).toInt().coerceAtLeast(0)
                    layout(placeable.width, reportedHeight) {
                        placeable.placeRelative(0, 0)
                    }
                }
                .alpha(calendarFraction)
                .clipToBounds()
        ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gridHeight = 24.dp + maxWidth / 7 * 6 + 4.dp

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(gridHeight)
            ) { page ->
                val offset = page - PAGER_INITIAL_PAGE
                val pageMonth = baseMonth.plusMonths(offset.toLong())

                val cachedDays = state.gregorianMonthCache[pageMonth]
                when {
                    pageMonth == state.currentMonth && state.isLoading && state.monthDays.isEmpty() ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    pageMonth == state.currentMonth ->
                        GregorianMonthGrid(
                            monthDays = state.monthDays,
                            currentMonth = state.currentMonth,
                            today = state.today,
                            selectedDate = state.selectedDate,
                            onDayClick = { date ->
                                onIntent(CalendarIntent.SelectDay(date))
                            }
                        )
                    cachedDays != null ->
                        GregorianMonthGrid(
                            monthDays = cachedDays,
                            currentMonth = pageMonth,
                            today = state.today,
                            selectedDate = state.selectedDate,
                            onDayClick = { date ->
                                onIntent(CalendarIntent.SelectDay(date))
                            }
                        )
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        } // ElevatedCard
        } // Box (animated wrapper)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            DayDetailContent(
                state = dayDetailState,
                onShowAllZmanim = onShowZmanim,
                scrollState = eventsScrollState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun HebrewCalendarContent(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit,
    scrollTarget: Flow<HebrewYearMonth>,
    onOpenDrawer: () -> Unit,
    dayDetailState: com.levana.app.ui.daydetail.DayDetailState,
    onShowZmanim: (LocalDate) -> Unit,
    onAddEvent: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = PAGER_INITIAL_PAGE,
        pageCount = { PAGER_PAGE_COUNT }
    )

    val baseHebrewMonth = remember { HebrewYearMonth.now() }
    val currentState by rememberUpdatedState(state)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val offset = page - PAGER_INITIAL_PAGE
            val targetMonth = baseHebrewMonth.plusMonths(offset)
            if (targetMonth != currentState.hebrewYearMonth) {
                onIntent(CalendarIntent.LoadHebrewMonth(targetMonth))
            }
        }
    }

    LaunchedEffect(Unit) {
        scrollTarget.collect { targetMonth ->
            pagerState.animateScrollToPage(PAGER_INITIAL_PAGE + baseHebrewMonth.stepsTo(targetMonth))
        }
    }

    val eventsScrollState = rememberScrollState()
    val calendarHeightPx = remember { mutableIntStateOf(0) }
    val calendarHiddenPx = remember { mutableFloatStateOf(0f) }
    val calendarFraction by remember {
        derivedStateOf {
            val h = calendarHeightPx.intValue.toFloat().coerceAtLeast(1f)
            1f - (calendarHiddenPx.floatValue / h).coerceIn(0f, 1f)
        }
    }

    val isResetting = remember { booleanArrayOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) {
                    val h = calendarHeightPx.intValue.toFloat()
                    if (h > 0f) {
                        val canConsume = h - calendarHiddenPx.floatValue
                        if (canConsume > 0f) {
                            val consume = minOf(-available.y, canConsume)
                            calendarHiddenPx.floatValue += consume
                            return Offset(0f, -consume)
                        }
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!isResetting[0] && available.y > 0f && calendarHiddenPx.floatValue > 0f) {
                    val consume = minOf(available.y, calendarHiddenPx.floatValue)
                    calendarHiddenPx.floatValue -= consume
                    return Offset(0f, consume)
                }
                return Offset.Zero
            }
        }
    }

    suspend fun animateExpand() = coroutineScope {
        launch {
            Animatable(calendarHiddenPx.floatValue).animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { calendarHiddenPx.floatValue = value }
        }
        launch {
            eventsScrollState.animateScrollTo(0)
        }
    }

    LaunchedEffect(state.selectedDate) {
        isResetting[0] = true
        try { animateExpand() } finally { isResetting[0] = false }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    isResetting[0] = true
                    try { animateExpand() } finally { isResetting[0] = false }
                }
            }
    }

    val onAddEventClick: () -> Unit = {
        dayDetailState.dayInfo?.hebrewDay?.let { h ->
            onAddEvent(h.day, h.month.jewishDateValue, h.year)
        }
    }

    Column(modifier = modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        HebrewMonthHeader(
            hebrewHeader = state.hebrewMonthHeader,
            gregorianHeader = state.gregorianHeader,
            onOpenDrawer = onOpenDrawer,
            onGoToToday = { onIntent(CalendarIntent.GoToToday) },
            onGoToDate = { onIntent(CalendarIntent.OpenGoToDateDialog) },
            onToggleMode = { onIntent(CalendarIntent.ToggleCalendarHebrewMode) },
            onAddEvent = onAddEventClick
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    if (placeable.height > 0 && calendarHeightPx.intValue == 0) {
                        calendarHeightPx.intValue = placeable.height
                    }
                    val hidden = calendarHiddenPx.floatValue.coerceIn(0f, placeable.height.toFloat())
                    val reportedHeight = (placeable.height - hidden).toInt().coerceAtLeast(0)
                    layout(placeable.width, reportedHeight) {
                        placeable.placeRelative(0, 0)
                    }
                }
                .alpha(calendarFraction)
                .clipToBounds()
        ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gridHeight = 24.dp + maxWidth / 7 * 6 + 4.dp

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(gridHeight)
            ) { page ->
                val offset = page - PAGER_INITIAL_PAGE
                val pageMonth = baseHebrewMonth.plusMonths(offset)

                val cachedDays = state.hebrewMonthCache[pageMonth]
                when {
                    pageMonth == state.hebrewYearMonth && state.isLoading && state.monthDays.isEmpty() ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    pageMonth == state.hebrewYearMonth ->
                        HebrewMonthGrid(
                            monthDays = state.monthDays,
                            today = state.today,
                            selectedDate = state.selectedDate,
                            onDayClick = { date ->
                                onIntent(CalendarIntent.SelectDay(date))
                            }
                        )
                    cachedDays != null ->
                        HebrewMonthGrid(
                            monthDays = cachedDays,
                            today = state.today,
                            selectedDate = state.selectedDate,
                            onDayClick = { date ->
                                onIntent(CalendarIntent.SelectDay(date))
                            }
                        )
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        } // ElevatedCard
        } // Box (animated wrapper)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            DayDetailContent(
                state = dayDetailState,
                onShowAllZmanim = onShowZmanim,
                scrollState = eventsScrollState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GregorianMonthHeader(
    state: CalendarState,
    onOpenDrawer: () -> Unit,
    onGoToToday: () -> Unit,
    onGoToDate: () -> Unit,
    onToggleMode: () -> Unit,
    onAddEvent: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = "Open menu")
            }
        },
        title = {
            Row(
                modifier = Modifier.clickable(onClick = onToggleMode),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.SwapVert,
                    contentDescription = "Switch to Hebrew calendar",
                    modifier = Modifier.padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Text(
                        text = state.currentMonth.month.getDisplayName(
                            TextStyle.FULL,
                            Locale.getDefault()
                        ) + " " + state.currentMonth.year,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = state.hebrewMonthHeader,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onGoToToday) {
                Icon(Icons.Filled.CalendarToday, contentDescription = "Go to today")
            }
            IconButton(onClick = onGoToDate) {
                Icon(Icons.Filled.EditCalendar, contentDescription = "Go to date")
            }
            IconButton(onClick = onAddEvent) {
                Icon(Icons.Filled.Add, contentDescription = "Add event")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(),
        windowInsets = WindowInsets(0)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HebrewMonthHeader(
    hebrewHeader: String,
    gregorianHeader: String,
    onOpenDrawer: () -> Unit,
    onGoToToday: () -> Unit,
    onGoToDate: () -> Unit,
    onToggleMode: () -> Unit,
    onAddEvent: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = "Open menu")
            }
        },
        title = {
            Row(
                modifier = Modifier.clickable(onClick = onToggleMode),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.SwapVert,
                    contentDescription = "Switch to Gregorian calendar",
                    modifier = Modifier.padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Text(
                        text = hebrewHeader,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = gregorianHeader,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onGoToToday) {
                Icon(Icons.Filled.CalendarToday, contentDescription = "Go to today")
            }
            IconButton(onClick = onGoToDate) {
                Icon(Icons.Filled.EditCalendar, contentDescription = "Go to date")
            }
            IconButton(onClick = onAddEvent) {
                Icon(Icons.Filled.Add, contentDescription = "Add event")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(),
        windowInsets = WindowInsets(0)
    )
}

@Composable
private fun GregorianMonthGrid(
    monthDays: List<HebrewDay>,
    currentMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek
    val leadingEmptyCells = firstDayOfWeek.value % 7

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        userScrollEnabled = false
    ) {
        dayHeaderRow(Locale.getDefault())
        items(leadingEmptyCells) {
            Box(modifier = Modifier.aspectRatio(0.85f))
        }

        items(monthDays, key = { it.gregorianDate.toEpochDay() }) { day ->
            DayCell(
                day = day,
                isToday = day.gregorianDate == today,
                isSelected = day.gregorianDate == selectedDate,
                calendarHebrewMode = false,
                onClick = { onDayClick(day.gregorianDate) }
            )
        }
    }
}

@Composable
private fun HebrewMonthGrid(
    monthDays: List<HebrewDay>,
    today: LocalDate,
    selectedDate: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    if (monthDays.isEmpty()) return
    val firstDate = monthDays.first().gregorianDate
    val leadingEmptyCells = firstDate.dayOfWeek.value % 7

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        userScrollEnabled = false
    ) {
        dayHeaderRow(HEBREW_LOCALE)
        items(leadingEmptyCells) {
            Box(modifier = Modifier.aspectRatio(0.85f))
        }

        items(monthDays, key = { it.gregorianDate.toEpochDay() }) { day ->
            DayCell(
                day = day,
                isToday = day.gregorianDate == today,
                isSelected = day.gregorianDate == selectedDate,
                calendarHebrewMode = true,
                onClick = { onDayClick(day.gregorianDate) }
            )
        }
    }
}

@Composable
private fun DayCell(
    day: HebrewDay,
    isToday: Boolean,
    isSelected: Boolean,
    calendarHebrewMode: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isToday -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }

    val primaryTextColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val secondaryTextColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (day.hasCandles) {
                    Text(
                        text = "\uD83D\uDD6F",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                day.holidayCategory?.let { category ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(holidayCategoryColor(category))
                    )
                }
                if (day.hasPersonalEvent) {
                    Text(
                        text = "\u2605",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                day.systemEventColors.forEach { colorInt ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                    )
                }
            }

            if (calendarHebrewMode) {
                // Hebrew letter is primary
                Text(
                    text = day.hebrewDayOfMonthFormatted,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = primaryTextColor
                )
                Text(
                    text = "${day.gregorianDate.dayOfMonth}/${day.gregorianDate.monthValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryTextColor
                )
            } else {
                // Gregorian day is primary
                Text(
                    text = day.gregorianDate.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = primaryTextColor
                )
                Text(
                    text = day.hebrewDayOfMonthFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryTextColor
                )
            }
        }
    }
}

private fun holidayCategoryColor(category: HolidayCategory): Color = when (category) {
    HolidayCategory.TORAH -> Color(0xFFD32F2F)
    HolidayCategory.RABBINIC -> Color(0xFF1976D2)
    HolidayCategory.FAST -> Color(0xFF757575)
    HolidayCategory.MINOR -> Color(0xFF388E3C)
    HolidayCategory.MODERN_ISRAELI -> Color(0xFFE65100)
}
