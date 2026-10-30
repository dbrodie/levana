package ninja.doskey.app.levana.ui.calendar

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.Flow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ninja.doskey.app.levana.ui.datepicker.HebrewDatePickerDialog
import ninja.doskey.app.levana.ui.datepicker.LeadingCalendar
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.ui.daydetail.DayDetailIntent
import ninja.doskey.app.levana.ui.daydetail.DayDetailViewModel
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

@Composable
private fun DayHeaderRow(locale: Locale) {
    Row(modifier = Modifier.fillMaxWidth().height(28.dp)) {
        DAYS_OF_WEEK.forEach { dow ->
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dow.getDisplayName(TextStyle.SHORT, locale),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    /** A day to select on opening (a widget or notification deep link), or null for today. */
    initialDate: LocalDate? = null,
    viewModel: CalendarViewModel = koinViewModel(),
    dayDetailViewModel: DayDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dayDetailState by dayDetailViewModel.state.collectAsStateWithLifecycle()

    // Nothing is shown until the view model has the device date
    val today = state.today
    LaunchedEffect(state.selectedDate, today != null) {
        if (today != null) dayDetailViewModel.onIntent(DayDetailIntent.LoadDay(state.selectedDate))
    }

    // Selected once the device date is known, as the first load selects today; only once,
    // so returning to the screen or rotating keeps the user's own selection
    var initialDateShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialDate, today != null) {
        if (today != null && initialDate != null && !initialDateShown) {
            initialDateShown = true
            if (initialDate != today) viewModel.onIntent(CalendarIntent.GoToDate(initialDate))
        }
    }

    // Changes to events made elsewhere (or a newly granted permission) refresh on return.
    LifecycleResumeEffect(viewModel, dayDetailViewModel) {
        viewModel.onIntent(CalendarIntent.Resume)
        dayDetailViewModel.onIntent(DayDetailIntent.Resume)
        onPauseOrDispose {
            viewModel.onIntent(CalendarIntent.Pause)
            dayDetailViewModel.onIntent(DayDetailIntent.Pause)
        }
    }

    if (today == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = state.calendarHebrewMode,
            label = "calendar_mode"
        ) { hebrewMode ->
            if (hebrewMode) {
                HebrewCalendarContent(
                    state = state,
                    today = today,
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
                    today = today,
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
    }

    if (state.showGoToDateDialog) {
        HebrewDatePickerDialog(
            initialDate = state.selectedDate,
            leading = if (state.calendarHebrewMode) LeadingCalendar.HEBREW else LeadingCalendar.GREGORIAN,
            onConfirm = { date -> viewModel.onIntent(CalendarIntent.GoToDate(date)) },
            onDismiss = { viewModel.onIntent(CalendarIntent.CloseGoToDateDialog) }
        )
    }
}

@Composable
private fun GregorianCalendarContent(
    state: CalendarState,
    today: LocalDate,
    onIntent: (CalendarIntent) -> Unit,
    scrollTarget: Flow<YearMonth>,
    onOpenDrawer: () -> Unit,
    dayDetailState: ninja.doskey.app.levana.ui.daydetail.DayDetailState,
    onShowZmanim: (LocalDate) -> Unit,
    onAddEvent: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = PAGER_INITIAL_PAGE, pageCount = { PAGER_PAGE_COUNT })
    // The pager is anchored on the device's month when it is first shown
    val baseMonth = remember { YearMonth.from(today) }
    val currentState by rememberUpdatedState(state)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val offset = page - PAGER_INITIAL_PAGE
            val target = baseMonth.plusMonths(offset.toLong())
            if (target != currentState.currentMonth) onIntent(CalendarIntent.LoadMonth(target))
        }
    }
    LaunchedEffect(Unit) {
        scrollTarget.collect { target ->
            pagerState.animateScrollToPage(PAGER_INITIAL_PAGE + ((target.year - baseMonth.year) * 12 + target.monthValue - baseMonth.monthValue))
        }
    }
    CalendarDaySheet(
        state = dayDetailState,
        selectedDate = state.selectedDate,
        onShowZmanim = onShowZmanim,
        modifier = modifier,
        header = {
            GregorianMonthHeader(
                state = state,
                onOpenDrawer = onOpenDrawer,
                onGoToToday = { onIntent(CalendarIntent.GoToToday) },
                onGoToDate = { onIntent(CalendarIntent.OpenGoToDateDialog) },
                onSelectMode = { onIntent(CalendarIntent.SetCalendarHebrewMode(it)) },
                onAddEvent = { dayDetailState.dayInfo?.hebrewDay?.let { onAddEvent(it.day, it.month.jewishDateValue, it.year) } },
            )
        },
    ) { gridModifier ->
        HorizontalPager(state = pagerState, modifier = gridModifier) { page ->
            val offset = page - PAGER_INITIAL_PAGE
            val pageMonth = baseMonth.plusMonths(offset.toLong())
            val days = if (pageMonth == state.currentMonth) state.monthDays else state.gregorianMonthCache[pageMonth].orEmpty()
            if (days.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (state.isLoading) CircularProgressIndicator()
                }
            } else {
                GregorianMonthGrid(
                    monthDays = days,
                    today = today, selectedDate = state.selectedDate,
                    onDayClick = { onIntent(CalendarIntent.SelectDay(it)) },
                )
            }
        }
    }
}

@Composable
private fun HebrewCalendarContent(
    state: CalendarState,
    today: LocalDate,
    onIntent: (CalendarIntent) -> Unit,
    scrollTarget: Flow<HebrewYearMonth>,
    onOpenDrawer: () -> Unit,
    dayDetailState: ninja.doskey.app.levana.ui.daydetail.DayDetailState,
    onShowZmanim: (LocalDate) -> Unit,
    onAddEvent: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = PAGER_INITIAL_PAGE, pageCount = { PAGER_PAGE_COUNT })
    val baseMonth = remember { HebrewYearMonth.from(today) }
    val currentState by rememberUpdatedState(state)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val offset = page - PAGER_INITIAL_PAGE
            val target = baseMonth.plusMonths(offset)
            if (target != currentState.hebrewYearMonth) onIntent(CalendarIntent.LoadHebrewMonth(target))
        }
    }
    LaunchedEffect(Unit) {
        scrollTarget.collect { target ->
            pagerState.animateScrollToPage(PAGER_INITIAL_PAGE + baseMonth.stepsTo(target))
        }
    }
    CalendarDaySheet(
        state = dayDetailState,
        selectedDate = state.selectedDate,
        onShowZmanim = onShowZmanim,
        modifier = modifier,
        header = {
            val locale = LocalConfiguration.current.locales[0]
            HebrewMonthHeader(
                hebrewHeader = remember(state.hebrewYearMonth, locale) {
                    CalendarHeaderFormatter.hebrewMonthTitle(state.hebrewYearMonth, locale)
                },
                gregorianHeader = remember(state.monthDays, locale) {
                    CalendarHeaderFormatter.gregorianRange(state.monthDays, locale)
                },
                onOpenDrawer = onOpenDrawer,
                onGoToToday = { onIntent(CalendarIntent.GoToToday) },
                onGoToDate = { onIntent(CalendarIntent.OpenGoToDateDialog) },
                onSelectMode = { onIntent(CalendarIntent.SetCalendarHebrewMode(it)) },
                onAddEvent = { dayDetailState.dayInfo?.hebrewDay?.let { onAddEvent(it.day, it.month.jewishDateValue, it.year) } },
            )
        },
    ) { gridModifier ->
        HorizontalPager(state = pagerState, modifier = gridModifier) { page ->
            val offset = page - PAGER_INITIAL_PAGE
            val pageMonth = baseMonth.plusMonths(offset)
            val days = if (pageMonth == state.hebrewYearMonth) state.monthDays else state.hebrewMonthCache[pageMonth].orEmpty()
            if (days.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (state.isLoading) CircularProgressIndicator()
                }
            } else {
                HebrewMonthGrid(
                    monthDays = days,
                    today = today, selectedDate = state.selectedDate,
                    onDayClick = { onIntent(CalendarIntent.SelectDay(it)) },
                )
            }
        }
    }
}

@Composable
private fun GregorianMonthHeader(
    state: CalendarState,
    onOpenDrawer: () -> Unit,
    onGoToToday: () -> Unit,
    onGoToDate: () -> Unit,
    onSelectMode: (Boolean) -> Unit,
    onAddEvent: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    CalendarMonthHeader(
        title = state.currentMonth.month.getDisplayName(TextStyle.FULL, locale) + " " + state.currentMonth.year,
        subtitle = remember(state.monthDays, locale) { CalendarHeaderFormatter.hebrewMonthRange(state.monthDays, locale) },
        selectedHebrew = false,
        onOpenDrawer = onOpenDrawer, onGoToToday = onGoToToday, onGoToDate = onGoToDate,
        onSelectMode = onSelectMode, onAddEvent = onAddEvent
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
    onSelectMode: (Boolean) -> Unit,
    onAddEvent: () -> Unit
) {
    CalendarMonthHeader(
        title = hebrewHeader, subtitle = gregorianHeader, selectedHebrew = true,
        onOpenDrawer = onOpenDrawer, onGoToToday = onGoToToday, onGoToDate = onGoToDate,
        onSelectMode = onSelectMode, onAddEvent = onAddEvent
    )
}

@Composable
private fun CalendarMonthHeader(
    title: String, subtitle: String, selectedHebrew: Boolean,
    onOpenDrawer: () -> Unit, onGoToToday: () -> Unit, onGoToDate: () -> Unit,
    onSelectMode: (Boolean) -> Unit, onAddEvent: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, stringResource(R.string.design_open_menu), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(modifier = Modifier.weight(1f).clickable(onClick = onGoToDate).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.calendar_go_to_date), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onGoToToday) { Icon(Icons.Filled.CalendarToday, stringResource(R.string.calendar_today), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onAddEvent) { Icon(Icons.Filled.Add, stringResource(R.string.design_add_event), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
            CalendarModeSelector(hebrewSelected = selectedHebrew, onSelectMode = onSelectMode)
        }
    }
}

@Composable
private fun GregorianMonthGrid(
    monthDays: List<HebrewDay>,
    today: LocalDate,
    selectedDate: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        DayHeaderRow(LocalConfiguration.current.locales[0])
        monthGridWeeks(monthDays).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                row.forEach { day ->
                    if (day != null) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            DayCell(
                                day = day,
                                isToday = day.gregorianDate == today,
                                isSelected = day.gregorianDate == selectedDate,
                                calendarHebrewMode = false,
                                onClick = { onDayClick(day.gregorianDate) }
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
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

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        DayHeaderRow(HEBREW_LOCALE)
        monthGridWeeks(monthDays).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                row.forEach { day ->
                    if (day != null) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            DayCell(
                                day = day,
                                isToday = day.gregorianDate == today,
                                isSelected = day.gregorianDate == selectedDate,
                                calendarHebrewMode = true,
                                onClick = { onDayClick(day.gregorianDate) }
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
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
    val locale = LocalConfiguration.current.locales[0]
    val isHebrew = locale.language in setOf("he", "iw")
    val dateDescription = day.gregorianDate.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL).withLocale(locale))
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        day.gregorianDate.dayOfWeek == DayOfWeek.SATURDAY -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> Color.Transparent
    }

    val primaryTextColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val secondaryTextColor = when {
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (isToday) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = isSelected; contentDescription = "${day.hebrewFormatted}, $dateDescription" },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 5.dp, start = 3.dp, end = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (calendarHebrewMode) {
                // Hebrew letter is primary
                Text(
                    text = day.hebrewDayOfMonthFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = primaryTextColor
                )
                Text(
                    // The Hebrew-primary reference uses the compact Gregorian day;
                    // including month/day here wraps in narrow RTL cells.
                    text = day.gregorianDate.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                } else {
                // Gregorian day is primary
                Text(
                    text = day.gregorianDate.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = primaryTextColor
                )
                Text(
                    text = day.hebrewDayOfMonthFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    color = secondaryTextColor
                )
                }
            }
            val bars = buildList {
                day.holidayCategory?.let { add(((if (isHebrew) day.holidayLabelHebrew else day.holidayLabel) ?: stringResource(R.string.holidays)) to holidayCategoryColor(it)) }
                (if (isHebrew) day.parshaLabelHebrew else day.parshaLabel)?.let { label -> if (size < 2) add(label to MaterialTheme.colorScheme.primary) }
                if (day.hasPersonalEvent) add(stringResource(R.string.calendar_event_personal) to MaterialTheme.colorScheme.tertiary)
                if (day.systemEventColors.isNotEmpty()) add(stringResource(R.string.calendar_event_system) to MaterialTheme.colorScheme.onSurfaceVariant)
                if (day.hasCandles && size < 2) add(stringResource(R.string.calendar_event_candles) to MaterialTheme.colorScheme.primary)
            }.take(2)
            bars.forEach { (label, color) ->
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = .18f)).padding(horizontal = 3.dp, vertical = 1.dp).semantics { contentDescription = label },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun holidayCategoryColor(category: HolidayCategory): Color =
    holidayCategoryColor(category, MaterialTheme.colorScheme)
