package ninja.doskey.app.levana.ui.datepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.NoFood
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.Layout
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.ui.calendar.CalendarModeSelector
import ninja.doskey.app.levana.ui.calendar.holidayCategoryColor
import ninja.doskey.app.levana.ui.calendar.monthGridWeeks
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.random.Random
import ninja.doskey.app.levana.ui.CappedFontScale

/** An extra footer button, such as the developer override's "Clear". */
data class DatePickerAction(val label: String, val onClick: () -> Unit)

/**
 * The app's one date picker (TASK-45, design 1b): a dual Hebrew/Gregorian calendar in a
 * dialog. [leading] is the calendar the grid starts in. [onConfirm] receives the selected
 * day on OK; Cancel and dismissing call [onDismiss] and commit nothing.
 */
@Composable
fun HebrewDatePickerDialog(
    initialDate: LocalDate,
    leading: LeadingCalendar,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    extraAction: DatePickerAction? = null,
    viewModel: DatePickerViewModel = koinViewModel()
) {
    // One session per showing: rotation keeps it, closing and reopening starts afresh
    val session = rememberSaveable { Random.nextLong() }
    LaunchedEffect(session) {
        if (viewModel.state.value?.session != session) {
            viewModel.onIntent(DatePickerIntent.Open(initialDate, leading, session))
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = stringResource(R.string.date_picker_title)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .semantics { paneTitle = title }
        ) {
            val shown = state?.takeIf { it.session == session } ?: return@Surface
            DatePickerContent(
                state = shown,
                onIntent = viewModel::onIntent,
                onConfirm = { onConfirm(shown.selection) },
                onDismiss = onDismiss,
                extraAction = extraAction
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DatePickerContent(
    state: DatePickerState,
    onIntent: (DatePickerIntent) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    extraAction: DatePickerAction?
) {
    val locale = LocalConfiguration.current.locales[0]
    val hebrewLed = state.leading == LeadingCalendar.HEBREW
    val scheme = MaterialTheme.colorScheme

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TitleBesideOrAbove(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 20.dp),
            title = {
                Text(
                    stringResource(R.string.date_picker_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant
                )
            },
            trailing = {
                CalendarModeSelector(
                    hebrewSelected = hebrewLed,
                    onSelectMode = { hebrew ->
                        onIntent(DatePickerIntent.SetLeading(if (hebrew) LeadingCalendar.HEBREW else LeadingCalendar.GREGORIAN))
                    }
                )
            }
        )

        MonthHeader(state, locale, onIntent)
        HorizontalDivider(color = scheme.outlineVariant)
        Spacer(Modifier.height(8.dp))

        Box(Modifier.fillMaxWidth().height(BODY_HEIGHT)) {
            // Fixed-size cells, whose numbers overlap beyond a modest font scale
            CappedFontScale {
                if (state.yearListOpen) {
                    YearGrid(state) { onIntent(DatePickerIntent.PickYear(it)) }
                } else {
                    DayGrid(state, locale) { onIntent(DatePickerIntent.SelectDay(it)) }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val hebrewDate = DatePickerFormatter.hebrewDate(state.selection)
                val gregorianDate = DatePickerFormatter.gregorianDate(state.selection, locale)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (hebrewLed) hebrewDate else gregorianDate,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Text(
                        if (hebrewLed) gregorianDate else hebrewDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                state.selectedDay?.let { day -> HolidayChip(day, locale) }
            }
            extraAction?.let { action -> TextButton(onClick = action.onClick) { Text(action.label) } }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_ok)) }
        }
    }
}

/**
 * [title] filling the row beside [trailing], both centred vertically, as a Row with a weighted
 * title. When the title does not fit there on one line (large text on a narrow window), it
 * goes above [trailing] instead of wrapping a letter or two per line.
 */
@Composable
private fun TitleBesideOrAbove(title: @Composable () -> Unit, trailing: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Layout(contents = listOf(title, trailing), modifier = modifier) { (titleMeasurables, trailingMeasurables), constraints ->
        val titleMeasurable = titleMeasurables.single()
        val width = constraints.maxWidth
        val end = trailingMeasurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        if (titleMeasurable.maxIntrinsicWidth(constraints.maxHeight) + end.width <= width) {
            val start = titleMeasurable.measure(constraints.copy(minWidth = width - end.width, maxWidth = width - end.width, minHeight = 0))
            val height = maxOf(start.height, end.height).coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                start.placeRelative(0, Alignment.CenterVertically.align(start.height, height))
                end.placeRelative(start.width, Alignment.CenterVertically.align(end.height, height))
            }
        } else {
            val start = titleMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val gap = STACK_GAP.roundToPx()
            val height = (start.height + gap + end.height).coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                start.placeRelative(0, 0)
                end.placeRelative(0, start.height + gap)
            }
        }
    }
}

@Composable
private fun MonthHeader(state: DatePickerState, locale: Locale, onIntent: (DatePickerIntent) -> Unit) {
    val hebrewLed = state.leading == LeadingCalendar.HEBREW
    val scheme = MaterialTheme.colorScheme
    val title = if (hebrewLed) {
        DatePickerFormatter.hebrewMonthTitle(state.hebrewMonth)
    } else {
        DatePickerFormatter.gregorianMonthTitle(state.gregorianMonth, locale)
    }
    val subtitle = if (hebrewLed) {
        DatePickerFormatter.gregorianSpan(state.days, locale)
    } else {
        DatePickerFormatter.hebrewSpan(state.days, locale)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        onClickLabel = stringResource(
                            if (state.yearListOpen) R.string.date_picker_choose_day else R.string.date_picker_choose_year
                        ),
                        role = Role.Button
                    ) { onIntent(DatePickerIntent.ToggleYearList) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        if (state.yearListOpen) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant
                    )
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!state.yearListOpen) {
            IconButton(
                onClick = { onIntent(DatePickerIntent.PreviousMonth) },
                enabled = DatePickerReducer.canGoPrevious(state)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.date_picker_previous_month))
            }
            IconButton(
                onClick = { onIntent(DatePickerIntent.NextMonth) },
                enabled = DatePickerReducer.canGoNext(state)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.date_picker_next_month))
            }
        }
    }
}

@Composable
private fun DayGrid(state: DatePickerState, locale: Locale, onSelect: (LocalDate) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // The cells announce their own full dates, so the weekday initials are not read
        Row(Modifier.fillMaxWidth().height(WEEKDAY_ROW_HEIGHT).clearAndSetSemantics {}) {
            WEEK.forEach { day ->
                Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        day.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        val firstDay = state.days.firstOrNull()
        monthGridWeeks(state.days).forEach { week ->
            Row(Modifier.fillMaxWidth().height(CELL_HEIGHT)) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (day != null) DayCell(day, state, firstCell = day == firstDay, locale, onSelect)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: HebrewDay,
    state: DatePickerState,
    firstCell: Boolean,
    locale: Locale,
    onSelect: (LocalDate) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val date = day.gregorianDate
    val selected = date == state.selection
    val isToday = date == state.today
    val enabled = DatePickerReducer.isSelectable(state, date)
    val hebrewLed = state.leading == LeadingCalendar.HEBREW
    val primaryColor = when {
        selected -> scheme.onPrimary
        isToday -> scheme.primary
        else -> scheme.onSurface
    }
    val secondaryColor = if (selected) scheme.onPrimary else scheme.onSurfaceVariant
    val description = DatePickerFormatter.dayDescription(
        day,
        locale,
        today = if (isToday) stringResource(R.string.calendar_today) else null
    )

    Box(
        modifier = Modifier
            .size(CELL_SIZE)
            .clip(CircleShape)
            .then(if (selected) Modifier.background(scheme.primary) else Modifier)
            .then(if (isToday && !selected) Modifier.border(1.dp, scheme.primary, CircleShape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button) { onSelect(date) }
            // On the clickable node itself, so a screen reader focuses one element per day
            .semantics {
                contentDescription = description
                this.selected = selected
            }
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        contentAlignment = Alignment.Center
    ) {
        // Lifted a little so the holiday dot below stays clear of the small line
        Column(Modifier.padding(bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (hebrewLed) {
                CellText(day.hebrewDayOfMonthFormatted, 16, FontWeight.Normal, primaryColor)
                CellText(DatePickerFormatter.gregorianCellLabel(day, firstCell, locale), 9, FontWeight.SemiBold, secondaryColor)
            } else {
                CellText(date.dayOfMonth.toString(), 15, FontWeight.Medium, primaryColor)
                CellText(DatePickerFormatter.hebrewCellLabel(day), 10, FontWeight.Normal, secondaryColor)
            }
        }
        day.holidayCategory?.let { category ->
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (selected) scheme.onPrimary else holidayCategoryColor(category, scheme))
            )
        }
    }
}

@Composable
private fun CellText(text: String, size: Int, weight: FontWeight, color: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        fontSize = size.sp,
        lineHeight = (size + 2).sp,
        fontWeight = weight,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.clearAndSetSemantics {}
    )
}

@Composable
private fun YearGrid(state: DatePickerState, onPick: (Int) -> Unit) {
    val hebrewLed = state.leading == LeadingCalendar.HEBREW
    val years = DatePickerReducer.yearOptions(state)
    val viewed = DatePickerReducer.viewedYear(state)
    val current = DatePickerReducer.currentYear(state)
    val scheme = MaterialTheme.colorScheme
    // Opens with the viewed year (or the nearest end of the list) on the third row
    val viewedIndex = viewed.coerceIn(years.first, years.last) - years.first
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = ((viewedIndex / 3 - 2) * 3).coerceAtLeast(0))

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(years.toList(), key = { it }) { year ->
            val selected = year == viewed
            val shape = RoundedCornerShape(50)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (hebrewLed) 52.dp else 40.dp)
                    .clip(shape)
                    .then(if (selected) Modifier.background(scheme.primary) else Modifier)
                    .then(if (year == current && !selected) Modifier.border(1.dp, scheme.primary, shape) else Modifier)
                    .clickable(role = Role.Button) { onPick(year) }
                    .semantics { this.selected = selected },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val color = if (selected) scheme.onPrimary else scheme.onSurface
                if (hebrewLed) {
                    Text(DatePickerFormatter.hebrewYear(year), fontSize = 16.sp, lineHeight = 18.sp, color = color)
                    Text(year.toString(), fontSize = 11.sp, lineHeight = 13.sp, color = if (selected) color else scheme.onSurfaceVariant)
                } else {
                    Text(year.toString(), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = color)
                }
            }
        }
    }
}

@Composable
private fun HolidayChip(day: HebrewDay, locale: Locale) {
    val category = day.holidayCategory ?: return
    val name = DatePickerFormatter.holidayName(day, locale) ?: return
    val color = holidayCategoryColor(category, MaterialTheme.colorScheme)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(categoryIcon(category), contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

private fun categoryIcon(category: HolidayCategory): ImageVector = when (category) {
    HolidayCategory.TORAH -> Icons.Outlined.AutoAwesome
    HolidayCategory.RABBINIC -> Icons.Outlined.Celebration
    HolidayCategory.FAST -> Icons.Outlined.NoFood
    HolidayCategory.MINOR -> Icons.Outlined.Event
    HolidayCategory.MODERN_ISRAELI -> Icons.Outlined.Flag
}

private val WEEK = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
)
private val WEEKDAY_ROW_HEIGHT = 32.dp
private val CELL_HEIGHT = 48.dp
private val CELL_SIZE = 44.dp
private val STACK_GAP = 8.dp

// Six weeks of cells under the weekday row, so the dialog keeps its size between months
private val BODY_HEIGHT = WEEKDAY_ROW_HEIGHT + CELL_HEIGHT * 6
private const val DISABLED_ALPHA = 0.38f
