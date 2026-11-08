package ninja.doskey.app.levana.ui.agenda

import ninja.doskey.app.levana.ui.placeName
import android.content.Context
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.NoFood
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.agenda.AgendaDay
import ninja.doskey.app.levana.domain.agenda.AgendaItem
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.ui.calendar.CalendarModeSelector
import ninja.doskey.app.levana.ui.calendar.CalendarHeaderFormatter
import ninja.doskey.app.levana.ui.agendaMevarchimText
import ninja.doskey.app.levana.ui.MISSING_TIME
import ninja.doskey.app.levana.ui.formatClockTime
import ninja.doskey.app.levana.ui.calendar.holidayCategoryColor
import ninja.doskey.app.levana.ui.datepicker.HebrewDatePickerDialog
import ninja.doskey.app.levana.ui.datepicker.LeadingCalendar
import ninja.doskey.app.levana.ui.events.occurrenceLabelText
import ninja.doskey.app.levana.ui.openDeviceCalendarEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.koin.androidx.compose.koinViewModel
import ninja.doskey.app.levana.ui.ContentWidthBox
import ninja.doskey.app.levana.ui.contentWidth
import ninja.doskey.app.levana.ui.daydetail.DayDetailIntent
import ninja.doskey.app.levana.ui.daydetail.DayDetailViewModel
import ninja.doskey.app.levana.ui.panes.DayPanel
import ninja.doskey.app.levana.ui.panes.ListDetailPanes
import ninja.doskey.app.levana.ui.panes.PaneLayout
import ninja.doskey.app.levana.ui.panes.rememberPaneLayout


/** Rows from the first loaded day within which the list asks for earlier days, and likewise at the end. */
private const val EDGE_ROWS = 4

/** Where a tapped agenda entry leads. */
class AgendaNavigation(
    val onOpenDrawer: () -> Unit = {},
    val onEditEvent: (eventId: Long) -> Unit = {},
    val onEditBirthday: (contactLookupKey: String) -> Unit = {},
    /** Candle lighting, Shabbat and havdalah: that day's Zmanim. */
    val onShowZmanim: (LocalDate) -> Unit = {},
    /** A holiday: that day in the Month calendar. */
    val onShowDay: (LocalDate) -> Unit = {},
    /** Device calendars: the System calendars screen, which asks for the permission. */
    val onChooseCalendars: () -> Unit = {},
)

@Composable
fun AgendaScreen(
    navigation: AgendaNavigation,
    modifier: Modifier = Modifier,
    viewModel: AgendaViewModel = koinViewModel(),
    dayDetailViewModel: DayDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dayDetailState by dayDetailViewModel.state.collectAsStateWithLifecycle()
    // Null below 840dp: the phone layout, without a day panel
    val panes = rememberPaneLayout()

    LifecycleResumeEffect(viewModel, dayDetailViewModel) {
        viewModel.onIntent(AgendaIntent.Resume)
        dayDetailViewModel.onIntent(DayDetailIntent.Resume)
        onPauseOrDispose {
            viewModel.onIntent(AgendaIntent.Pause)
            dayDetailViewModel.onIntent(DayDetailIntent.Pause)
        }
    }

    // The panel's day loads only while the panel shows; not again for the day it already shows
    val panelDate = state.selectedDate ?: state.today
    LaunchedEffect(panelDate, panes != null) {
        if (panes != null && panelDate != null && dayDetailViewModel.state.value.dayInfo?.gregorianDate != panelDate) {
            dayDetailViewModel.onIntent(DayDetailIntent.LoadDay(panelDate))
        }
    }

    AgendaContent(
        state = state,
        onIntent = viewModel::onIntent,
        navigation = navigation,
        modifier = modifier,
        panes = panes,
        dayPanel = { panelDate?.let { DayPanel(dayDetailState, it, navigation.onShowZmanim) } },
    )

    state.datePickerDate?.let { date ->
        HebrewDatePickerDialog(
            initialDate = date,
            leading = if (state.hebrewLeading) LeadingCalendar.HEBREW else LeadingCalendar.GREGORIAN,
            onConfirm = { viewModel.onIntent(AgendaIntent.GoToDate(it)) },
            onDismiss = { viewModel.onIntent(AgendaIntent.CloseDatePicker) },
        )
    }
}

@Composable
internal fun AgendaContent(
    state: AgendaState,
    onIntent: (AgendaIntent) -> Unit,
    navigation: AgendaNavigation,
    modifier: Modifier = Modifier,
    /** On wide windows, the list and [dayPanel] side by side under the header (TASK-52.6). */
    panes: PaneLayout? = null,
    dayPanel: @Composable () -> Unit = {},
) {
    val today = state.today
    if (today == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val listState = rememberLazyListState()
    val rows = remember(state.days, state.hebrewLeading) { agendaRows(state.days, state.hebrewLeading) }
    // The header follows the day at the top of the list (list indices count a leading loading row)
    val leadingRows = if (state.loadingEarlier) 1 else 0
    val topDate by remember(rows, leadingRows) {
        derivedStateOf {
            rows.getOrNull((listState.firstVisibleItemIndex - leadingRows).coerceAtLeast(0))?.day?.date ?: today
        }
    }
    LaunchedEffect(topDate) { onIntent(AgendaIntent.TopDayShown(topDate)) }
    var permissionDismissed by rememberSaveable { mutableStateOf(false) }

    val search = state.search
    BackHandler(search != null) { onIntent(AgendaIntent.CloseSearch) }

    // With a panel, the list highlights its day, and a tap on a day's date rail shows that day there
    val selection = panes?.let { AgendaSelection(state.selectedDate ?: today) { onIntent(AgendaIntent.SelectDay(it)) } }
    val list: @Composable (Modifier) -> Unit = { listModifier ->
        Column(listModifier) {
            if (search == null && !state.calendarPermission && !permissionDismissed) {
                CalendarPermissionBanner(
                    onChoose = navigation.onChooseCalendars,
                    onDismiss = { permissionDismissed = true },
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (search != null) {
                    SearchResults(search, today, state, onIntent)
                } else {
                    AgendaList(rows, listState, today, state, onIntent, navigation, selection)
                }
            }
        }
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (search != null) {
            SearchHeader(search.query, onIntent)
        } else {
            AgendaHeader(
                month = AgendaMonth.of(topDate, state.hebrewLeading),
                hebrewLeading = state.hebrewLeading,
                onOpenDrawer = navigation.onOpenDrawer,
                onGoToDate = { onIntent(AgendaIntent.OpenDatePicker(topDate)) },
                onSearch = { onIntent(AgendaIntent.OpenSearch) },
                onGoToToday = { onIntent(AgendaIntent.GoToToday) },
                onSelectMode = { onIntent(AgendaIntent.SetHebrewLeading(it)) },
            )
        }
        if (panes == null) {
            list(Modifier.weight(1f).fillMaxWidth())
        } else {
            ListDetailPanes(
                panes = panes,
                modifier = Modifier.weight(1f),
                list = { list(Modifier.fillMaxSize()) },
                detail = dayPanel,
            )
        }
    }
}

/** The day the panel beside the list shows, and how a tap on a date rail changes it. */
private class AgendaSelection(val date: LocalDate, val onSelect: (LocalDate) -> Unit)

@Composable
private fun AgendaList(
    rows: List<AgendaRow>,
    listState: LazyListState,
    today: LocalDate,
    state: AgendaState,
    onIntent: (AgendaIntent) -> Unit,
    navigation: AgendaNavigation,
    selection: AgendaSelection?,
) {
    if (state.isLoading && rows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (rows.isEmpty()) {
        EmptyMessage(stringResource(R.string.agenda_empty))
        return
    }

    // Bring a requested day (today, a picked date, a search result) to the top once listed
    val request = state.scrollRequest
    LaunchedEffect(request, rows) {
        if (request == null || state.isLoading) return@LaunchedEffect
        val index = rows.indexOfFirst { !it.day.date.isBefore(request.date) }.takeIf { it >= 0 } ?: rows.lastIndex
        listState.scrollToItem(index + if (state.loadingEarlier) 1 else 0)
        onIntent(AgendaIntent.ScrollHandled(request.id))
    }

    // Extend the window as either end comes near; not while a requested scroll is pending
    val currentState by rememberUpdatedState(state)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull()?.index ?: 0
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            Triple(first, last, info.totalItemsCount)
        }.distinctUntilChanged().collect { (first, last, total) ->
            if (total == 0 || currentState.scrollRequest != null) return@collect
            if (first < EDGE_ROWS) onIntent(AgendaIntent.LoadEarlier)
            if (last >= total - EDGE_ROWS) onIntent(AgendaIntent.LoadLater)
        }
    }

    val context = LocalContext.current
    ContentWidthBox(Modifier.fillMaxSize()) { gutter ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp + gutter, end = 16.dp + gutter, top = 4.dp, bottom = 24.dp),
        ) {
            if (state.loadingEarlier) item(key = "loading-earlier") { LoadingRow() }
            items(rows, key = { it.key }) { row ->
                AgendaRowItem(
                    row, today, state, clickLabel = null,
                    selected = selection?.date == row.day.date,
                    onSelectDay = selection?.let { { it.onSelect(row.day.date) } },
                ) { item -> openItem(item, row.day.date, navigation, context) }
            }
            if (state.loadingLater) item(key = "loading-later") { LoadingRow() }
        }
    }
}

/** Opens what a tapped entry refers to; the same beside the day panel, whose day it leaves alone. */
private fun openItem(item: AgendaItem, date: LocalDate, navigation: AgendaNavigation, context: Context) {
    when (item) {
        is AgendaItem.PersonalEventItem -> navigation.onEditEvent(item.event.id)
        is AgendaItem.BirthdayItem -> navigation.onEditBirthday(item.birthday.contactLookupKey)
        is AgendaItem.HolidayItem -> navigation.onShowDay(date)
        is AgendaItem.CandleLighting, is AgendaItem.Shabbat, is AgendaItem.Havdalah -> navigation.onShowZmanim(date)
        is AgendaItem.DeviceEventItem -> openDeviceCalendarEvent(context, item.event.eventId)
    }
}

@Composable
private fun SearchResults(
    search: AgendaSearchState,
    today: LocalDate,
    state: AgendaState,
    onIntent: (AgendaIntent) -> Unit,
) {
    when {
        search.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        search.query.isBlank() -> EmptyMessage(stringResource(R.string.agenda_search_prompt))
        search.results.isEmpty() -> EmptyMessage(stringResource(R.string.agenda_search_empty, search.query.trim()))
        else -> {
            val rows = remember(search.results, state.hebrewLeading) { agendaRows(search.results, state.hebrewLeading) }
            val showLabel = stringResource(R.string.agenda_show_in_agenda)
            ContentWidthBox(Modifier.fillMaxSize()) { gutter ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp + gutter, end = 16.dp + gutter, top = 4.dp, bottom = 24.dp),
                ) {
                    items(rows, key = { it.key }) { row ->
                        // A result shows its day in the agenda
                        AgendaRowItem(row, today, state, clickLabel = showLabel) { onIntent(AgendaIntent.SelectSearchResult(row.day.date)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaHeader(
    month: AgendaMonth,
    hebrewLeading: Boolean,
    onOpenDrawer: () -> Unit,
    onGoToDate: () -> Unit,
    onSearch: () -> Unit,
    onGoToToday: () -> Unit,
    onSelectMode: (Boolean) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val title = remember(month, locale) { AgendaFormatter.title(month, locale) }
    val span = remember(month, locale) { AgendaFormatter.otherCalendarSpan(month, locale) }
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, stringResource(R.string.design_open_menu), tint = iconTint) }
            Row(
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable(onClick = onGoToDate).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, modifier = Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.calendar_go_to_date), tint = iconTint)
            }
            IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, stringResource(R.string.agenda_search), tint = iconTint) }
            IconButton(onClick = onGoToToday) { Icon(Icons.Filled.CalendarToday, stringResource(R.string.calendar_today), tint = iconTint) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(span, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            CalendarModeSelector(hebrewSelected = hebrewLeading, onSelectMode = onSelectMode)
        }
    }
}

@Composable
private fun SearchHeader(query: String, onIntent: (AgendaIntent) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).background(MaterialTheme.colorScheme.surface).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onIntent(AgendaIntent.CloseSearch) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.agenda_search_close))
        }
        TextField(
            value = query,
            onValueChange = { onIntent(AgendaIntent.SetSearchQuery(it)) },
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.agenda_search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onIntent(AgendaIntent.SetSearchQuery("")) }) {
                Icon(Icons.Filled.Close, stringResource(R.string.agenda_search_clear))
            }
        }
    }
}

@Composable
private fun CalendarPermissionBanner(onChoose: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.contentWidth().fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(stringResource(R.string.agenda_calendar_permission), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.agenda_not_now)) }
                Spacer(Modifier.width(4.dp))
                Button(onClick = onChoose) { Text(stringResource(R.string.agenda_calendar_choose)) }
            }
        }
    }
}

/**
 * One list item: a day, under its month's header when it starts one. The header belongs to
 * the day's item, so regrouping by the other calendar changes items' content, never which
 * items the list holds.
 */
@Composable
private fun AgendaRowItem(
    row: AgendaRow,
    today: LocalDate,
    state: AgendaState,
    clickLabel: String?,
    selected: Boolean = false,
    onSelectDay: (() -> Unit)? = null,
    onClickItem: (AgendaItem) -> Unit,
) {
    Column {
        row.header?.let { MonthHeaderRow(it) }
        DayRow(
            day = row.day,
            isToday = row.day.date == today,
            hebrewLeading = state.hebrewLeading,
            otherClockCityName = state.otherClockCityName,
            onClickItem = onClickItem,
            clickLabel = clickLabel,
            selected = selected,
            onSelectDay = onSelectDay,
        )
    }
}

@Composable
private fun MonthHeaderRow(month: AgendaMonth) {
    val locale = LocalConfiguration.current.locales[0]
    val title = remember(month, locale) { AgendaFormatter.title(month, locale) }
    val span = remember(month, locale) { AgendaFormatter.otherCalendarSpan(month, locale, short = true) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 20.dp, bottom = 8.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(12.dp))
        Text(span, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp))
    }
}

/** A day with its cards; also the walkthrough's agenda illustration. */
@Composable
internal fun DayRow(
    day: AgendaDay,
    isToday: Boolean,
    hebrewLeading: Boolean,
    otherClockCityName: String?,
    onClickItem: (AgendaItem) -> Unit,
    clickLabel: String?,
    /** Times in 24-hour form; null follows the device setting. */
    use24Hour: Boolean? = null,
    /** The day the panel beside the list shows (TASK-52.6). */
    selected: Boolean = false,
    /** Shows this day in the panel beside the list; null without a panel. */
    onSelectDay: (() -> Unit)? = null,
) {
    val highlight = MaterialTheme.colorScheme.primaryContainer
    Row(
        Modifier.fillMaxWidth()
            // Drawn a little beyond the row, into the list's padding, so the cards keep their place
            .then(if (selected) Modifier.drawBehind {
                val outset = SelectionOutset.toPx()
                drawRoundRect(
                    color = highlight,
                    topLeft = Offset(-outset, 0f),
                    size = Size(size.width + 2 * outset, size.height),
                    cornerRadius = CornerRadius(SelectionRadius.toPx()),
                )
            } else Modifier)
            .padding(vertical = 6.dp)
    ) {
        DateRail(day, isToday, hebrewLeading, selected, onSelectDay)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            day.items.forEach { item ->
                AgendaCard(item, otherClockCityName, clickLabel, use24Hour) { onClickItem(item) }
            }
        }
    }
}

private val SelectionOutset = 6.dp
private val SelectionRadius = 16.dp

/**
 * The weekday, the leading calendar's day (in a filled circle today) and the other calendar's
 * date. With [onSelect], a tap shows the day in the panel beside the list.
 */
@Composable
private fun DateRail(day: AgendaDay, isToday: Boolean, hebrewLeading: Boolean, selected: Boolean = false, onSelect: (() -> Unit)? = null) {
    val locale = LocalConfiguration.current.locales[0]
    val hebrewUi = CalendarHeaderFormatter.isHebrew(locale)
    val gregorianDay = day.date.dayOfMonth.toString()
    // "ז׳ כסלו" in Hebrew, "7 Kislev" in English
    val hebrewSmall = if (hebrewUi) {
        "${day.hebrewDay.hebrewDayOfMonthFormatted} ${day.hebrewDay.month.hebrewName}"
    } else {
        "${day.hebrewDay.day} ${day.hebrewDay.month.englishName}"
    }
    val large = if (hebrewLeading) day.hebrewDay.hebrewDayOfMonthFormatted else gregorianDay
    val small = if (hebrewLeading) AgendaFormatter.shortGregorian(day.date, locale) else hebrewSmall
    val fullDate = day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    val dates = "${day.hebrewDay.hebrewFormatted}, $fullDate"
    val description = if (isToday) stringResource(R.string.agenda_day_today, dates) else dates
    Column(
        modifier = Modifier.width(64.dp)
            // Not clipped: a long other-calendar date overflows the rail, as without a panel
            .then(if (onSelect != null) Modifier.clickable(role = Role.Button, onClick = onSelect) else Modifier)
            .clearAndSetSemantics {
                contentDescription = description
                heading()
                if (onSelect != null) {
                    this.selected = selected
                    onClick { onSelect(); true }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(AgendaFormatter.weekday(day.date, locale), style = MaterialTheme.typography.labelMedium,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium, maxLines = 1)
        Box(
            modifier = Modifier.padding(vertical = 2.dp).sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                .then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(large, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, maxLines = 1,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp))
        }
        Text(small, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, maxLines = 2)
    }
}

private enum class CardStyle { OUTLINED, TONAL, TINTED }

/** What a card shows, resolved for the UI language. */
private data class CardContent(
    val icon: ImageVector,
    val title: String,
    val secondary: String?,
    val trailing: String?,
    val style: CardStyle,
    /** TalkBack: the entry's kind, then its title and time. */
    val description: String,
    val tint: Color? = null,
    /** A further line under [secondary], up to two lines: the Shabbat Mevarchim molad. */
    val detail: String? = null,
)

@Composable
private fun AgendaCard(
    item: AgendaItem,
    otherClockCityName: String?,
    clickLabel: String?,
    use24Hour: Boolean?,
    onClick: () -> Unit,
) {
    val content = cardContent(item, otherClockCityName, use24Hour)
    val scheme = MaterialTheme.colorScheme
    val container = when (content.style) {
        CardStyle.OUTLINED -> scheme.surface
        CardStyle.TONAL -> scheme.surfaceContainerHigh
        CardStyle.TINTED -> (content.tint ?: scheme.primary).copy(alpha = 0.16f).compositeOver(scheme.surface)
    }
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = container,
        contentColor = scheme.onSurface,
        border = if (content.style == CardStyle.OUTLINED) BorderStroke(1.dp, scheme.outlineVariant) else null,
        modifier = Modifier.fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = content.description
                role = Role.Button
                onClick(label = clickLabel) { onClick(); true }
            },
    ) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(content.icon, contentDescription = null, tint = content.tint ?: scheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(content.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                content.secondary?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                content.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            content.trailing?.let {
                Spacer(Modifier.width(8.dp))
                Text(it, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun cardContent(item: AgendaItem, otherClockCityName: String?, use24Hour: Boolean?): CardContent {
    val context = LocalContext.current
    val hebrewUi = CalendarHeaderFormatter.isHebrew(LocalConfiguration.current.locales[0])
    val clock24 = use24Hour ?: DateFormat.is24HourFormat(context)
    fun time(t: LocalTime) = formatClockTime(t, clock24, hebrewUi)
    fun timeOrMissing(t: LocalTime?) = t?.let(::time) ?: MISSING_TIME
    // Candle lighting and havdalah are the selected city's; label its clock when it differs
    val cityClock = otherClockCityName?.let { stringResource(R.string.zmanim_city_clock, placeName(it)) }
    fun describe(vararg parts: String?) = parts.filterNot { it.isNullOrBlank() }.joinToString(", ")
    return when (item) {
        is AgendaItem.HolidayItem -> {
            val month = item.roshChodeshOf?.let { if (hebrewUi) it.hebrewName else it.englishName }
            val name = when {
                month != null -> stringResource(R.string.agenda_rosh_chodesh, month)
                hebrewUi -> item.holiday.hebrewName
                else -> item.holiday.name
            }
            val fast = item.holiday.category == HolidayCategory.FAST
            val type = stringResource(if (fast) R.string.agenda_type_fast else R.string.agenda_type_holiday)
            CardContent(
                icon = if (fast) Icons.Outlined.NoFood else Icons.Outlined.Celebration,
                title = name, secondary = null, trailing = null, style = CardStyle.TINTED,
                description = describe(type, name),
                tint = holidayCategoryColor(item.holiday.category, MaterialTheme.colorScheme),
            )
        }
        is AgendaItem.CandleLighting -> {
            val title = stringResource(R.string.agenda_candle_lighting)
            CardContent(Icons.Outlined.WbTwilight, title, cityClock, timeOrMissing(item.time), CardStyle.OUTLINED,
                describe(title, timeOrMissing(item.time), cityClock))
        }
        is AgendaItem.Shabbat -> {
            val title = stringResource(R.string.agenda_parsha, if (hebrewUi) item.parshaHebrew ?: item.parsha else item.parsha)
            val havdalah = if (item.hasHavdalah) stringResource(R.string.agenda_havdalah_at, timeOrMissing(item.havdalah)) else null
            val secondary = listOfNotNull(havdalah, cityClock.takeIf { havdalah != null }).joinToString(" · ").ifEmpty { null }
            val mevarchim = item.molad?.let { agendaMevarchimText(context, it, clock24) }
            CardContent(Icons.AutoMirrored.Outlined.MenuBook, title, secondary, null, CardStyle.TONAL,
                describe(stringResource(R.string.agenda_type_shabbat), title, secondary, mevarchim), detail = mevarchim)
        }
        is AgendaItem.Havdalah -> {
            val title = stringResource(R.string.agenda_havdalah)
            CardContent(Icons.Outlined.NightsStay, title, cityClock, timeOrMissing(item.time), CardStyle.TONAL,
                describe(title, timeOrMissing(item.time), cityClock))
        }
        is AgendaItem.PersonalEventItem -> {
            val label = item.label?.let { occurrenceLabelText(context, it) }
            val secondary = label ?: item.event.notes.takeIf { it.isNotBlank() }
            if (item.isYahrzeit) {
                val title = stringResource(R.string.agenda_yahrzeit_title, item.event.title)
                CardContent(Icons.Outlined.LocalFireDepartment, title, secondary, null, CardStyle.OUTLINED,
                    describe(title, secondary))
            } else {
                CardContent(Icons.Outlined.Event, item.event.title, secondary, null, CardStyle.OUTLINED,
                    describe(stringResource(R.string.calendar_event_personal), item.event.title, secondary))
            }
        }
        is AgendaItem.BirthdayItem -> {
            val title = stringResource(R.string.agenda_birthday_title, item.birthday.contactName)
            val secondary = item.label?.let { occurrenceLabelText(context, it) }
            CardContent(Icons.Outlined.Cake, title, secondary, null, CardStyle.OUTLINED, describe(title, secondary))
        }
        is AgendaItem.DeviceEventItem -> {
            val trailing = when {
                item.event.allDay -> stringResource(R.string.sheet_all_day)
                else -> item.time?.let(::time)
            }
            val location = item.event.location.takeIf { it.isNotBlank() }
            CardContent(Icons.Outlined.Event, item.event.title, location, trailing, CardStyle.OUTLINED,
                describe(stringResource(R.string.calendar_event_system), item.event.title, trailing, location),
                tint = Color(item.event.calendarColor))
        }
    }
}

@Composable
private fun LoadingRow() {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
    }
}
