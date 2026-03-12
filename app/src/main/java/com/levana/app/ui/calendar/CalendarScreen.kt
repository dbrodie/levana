package com.levana.app.ui.calendar

import androidx.annotation.VisibleForTesting
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levana.app.domain.model.HebrewDay
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

    if (state.hebrewPrimary) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            HebrewCalendarContent(
                state = state,
                onIntent = viewModel::onIntent,
                onOpenDrawer = onOpenDrawer,
                dayDetailState = dayDetailState,
                onShowZmanim = onShowZmanim,
                onAddEvent = onAddEvent,
                modifier = modifier
            )
        }
    } else {
        GregorianCalendarContent(
            state = state,
            onIntent = viewModel::onIntent,
            onOpenDrawer = onOpenDrawer,
            dayDetailState = dayDetailState,
            onShowZmanim = onShowZmanim,
            onAddEvent = onAddEvent,
            modifier = modifier
        )
    }
}

@Composable
private fun GregorianCalendarContent(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit,
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

    val baseMonth = YearMonth.now()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val offset = page - PAGER_INITIAL_PAGE
            val targetMonth = baseMonth.plusMonths(offset.toLong())
            if (targetMonth != state.currentMonth) {
                onIntent(CalendarIntent.LoadMonth(targetMonth))
            }
        }
    }

    val expectedPage = PAGER_INITIAL_PAGE +
        ((state.currentMonth.year - baseMonth.year) * 12) +
        (state.currentMonth.monthValue - baseMonth.monthValue)

    LaunchedEffect(expectedPage) {
        if (pagerState.settledPage != expectedPage) {
            pagerState.animateScrollToPage(expectedPage)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        GregorianMonthHeader(
            state = state,
            onOpenDrawer = onOpenDrawer,
            onNext = { onIntent(CalendarIntent.NextMonth) },
            onGoToToday = { onIntent(CalendarIntent.GoToToday) }
        )

        DayOfWeekHeader(hebrewPrimary = false)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gridHeight = maxWidth / 7 / 0.85f * 6 + 4.dp

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(gridHeight)
            ) { page ->
                val offset = page - PAGER_INITIAL_PAGE
                val pageMonth = baseMonth.plusMonths(offset.toLong())

                if (pageMonth == state.currentMonth) {
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        GregorianMonthGrid(
                            monthDays = state.monthDays,
                            currentMonth = state.currentMonth,
                            today = state.today,
                            selectedDate = state.selectedDate,
                            onDayClick = { date ->
                                onIntent(CalendarIntent.SelectDay(date))
                            }
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            DayDetailContent(
                state = dayDetailState,
                onShowZmanim = onShowZmanim,
                onAddEvent = onAddEvent,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun HebrewCalendarContent(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit,
    onOpenDrawer: () -> Unit,
    dayDetailState: com.levana.app.ui.daydetail.DayDetailState,
    onShowZmanim: (LocalDate) -> Unit,
    onAddEvent: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        HebrewMonthHeader(
            hebrewHeader = state.hebrewMonthHeader,
            gregorianHeader = state.gregorianHeader,
            onOpenDrawer = onOpenDrawer,
            onPrevious = { onIntent(CalendarIntent.PreviousHebrewMonth) },
            onNext = { onIntent(CalendarIntent.NextHebrewMonth) },
            onGoToToday = { onIntent(CalendarIntent.GoToToday) }
        )

        DayOfWeekHeader(hebrewPrimary = true)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gridHeight = maxWidth / 7 / 0.85f * 6 + 4.dp

            if (state.isLoading && state.monthDays.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(gridHeight),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(gridHeight)) {
                    HebrewMonthGrid(
                        monthDays = state.monthDays,
                        today = state.today,
                        selectedDate = state.selectedDate,
                        onDayClick = { date ->
                            onIntent(CalendarIntent.SelectDay(date))
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            DayDetailContent(
                state = dayDetailState,
                onShowZmanim = onShowZmanim,
                onAddEvent = onAddEvent,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun GregorianMonthHeader(
    state: CalendarState,
    onOpenDrawer: () -> Unit,
    onNext: () -> Unit,
    onGoToToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenDrawer) {
            Icon(Icons.Filled.Menu, contentDescription = "Open menu")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.hebrewMonthHeader,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = state.currentMonth.month.getDisplayName(
                    TextStyle.FULL,
                    Locale.getDefault()
                ) + " " + state.currentMonth.year,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onGoToToday) {
                Text("Today", style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month"
                )
            }
        }
    }
}

@Composable
private fun HebrewMonthHeader(
    hebrewHeader: String,
    gregorianHeader: String,
    onOpenDrawer: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoToToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // In RTL context the hamburger stays on the physical left via LTR override
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = "Open menu")
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = hebrewHeader,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                Text(
                    text = gregorianHeader,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onGoToToday) {
                    Text("Today", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month"
                    )
                }
            }
        }
    }
}

@VisibleForTesting
@Composable
internal fun DayOfWeekHeader(hebrewPrimary: Boolean) {
    val daysOfWeek = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )

    val locale = if (hebrewPrimary) Locale("he") else Locale.getDefault()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        daysOfWeek.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, locale),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
        items(leadingEmptyCells) {
            Box(modifier = Modifier.aspectRatio(0.85f))
        }

        items(monthDays, key = { it.gregorianDate.toEpochDay() }) { day ->
            DayCell(
                day = day,
                isToday = day.gregorianDate == today,
                isSelected = day.gregorianDate == selectedDate,
                hebrewPrimary = false,
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
    val dayVal = firstDate.dayOfWeek.value // Mon=1..Sun=7
    val leadingEmptyCells = (dayVal % 7 + 1) % 7

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        userScrollEnabled = false
    ) {
        items(leadingEmptyCells) {
            Box(modifier = Modifier.aspectRatio(0.85f))
        }

        items(monthDays, key = { it.gregorianDate.toEpochDay() }) { day ->
            DayCell(
                day = day,
                isToday = day.gregorianDate == today,
                isSelected = day.gregorianDate == selectedDate,
                hebrewPrimary = true,
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
    hebrewPrimary: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isToday -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
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
            .aspectRatio(0.85f)
            .padding(1.dp)
            .clip(RoundedCornerShape(8.dp))
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

            if (hebrewPrimary) {
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
