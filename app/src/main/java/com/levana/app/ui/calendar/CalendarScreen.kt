package com.levana.app.ui.calendar

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.hebrewPrimary) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            HebrewCalendarContent(
                state = state,
                onIntent = viewModel::onIntent,
                onDayClick = onDayClick,
                modifier = modifier
            )
        }
    } else {
        GregorianCalendarContent(
            state = state,
            onIntent = viewModel::onIntent,
            onDayClick = onDayClick,
            modifier = modifier
        )
    }
}

@Composable
private fun GregorianCalendarContent(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit,
    onDayClick: (LocalDate) -> Unit,
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
            onPrevious = { onIntent(CalendarIntent.PreviousMonth) },
            onNext = { onIntent(CalendarIntent.NextMonth) }
        )

        DayOfWeekHeader(rtl = false)

        if (state.isLoading && state.monthDays.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val offset = page - PAGER_INITIAL_PAGE
                val pageMonth = baseMonth.plusMonths(offset.toLong())

                if (pageMonth == state.currentMonth) {
                    GregorianMonthGrid(
                        monthDays = state.monthDays,
                        currentMonth = state.currentMonth,
                        today = state.today,
                        onDayClick = onDayClick
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun HebrewCalendarContent(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        HebrewMonthHeader(
            hebrewHeader = state.hebrewMonthHeader,
            gregorianHeader = state.gregorianHeader,
            onPrevious = { onIntent(CalendarIntent.PreviousHebrewMonth) },
            onNext = { onIntent(CalendarIntent.NextHebrewMonth) }
        )

        DayOfWeekHeader(rtl = true)

        if (state.isLoading && state.monthDays.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            HebrewMonthGrid(
                monthDays = state.monthDays,
                today = state.today,
                onDayClick = onDayClick
            )
        }
    }
}

@Composable
private fun GregorianMonthHeader(state: CalendarState, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month"
            )
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

        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month"
            )
        }
    }
}

@Composable
private fun HebrewMonthHeader(
    hebrewHeader: String,
    gregorianHeader: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Next month"
            )
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

        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Previous month"
            )
        }
    }
}

@VisibleForTesting
@Composable
internal fun DayOfWeekHeader(rtl: Boolean) {
    val daysOfWeek = if (rtl) {
        listOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.SUNDAY
        )
    } else {
        listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        daysOfWeek.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
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
            Box(modifier = Modifier.aspectRatio(1f))
        }

        items(monthDays, key = { it.gregorianDate.toEpochDay() }) { day ->
            DayCell(
                day = day,
                isToday = day.gregorianDate == today,
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
    onDayClick: (LocalDate) -> Unit
) {
    if (monthDays.isEmpty()) return
    val firstDate = monthDays.first().gregorianDate
    // For RTL grid: Shabbat (Saturday) is column 0
    // DayOfWeek.SATURDAY.value = 6, we want offset from Saturday
    val dayVal = firstDate.dayOfWeek.value // Mon=1..Sun=7
    // Map to Sat=0, Sun=1, Mon=2, ..., Fri=6
    val leadingEmptyCells = (dayVal % 7 + 1) % 7

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        userScrollEnabled = false
    ) {
        items(leadingEmptyCells) {
            Box(modifier = Modifier.aspectRatio(1f))
        }

        items(monthDays, key = { it.gregorianDate.toEpochDay() }) { day ->
            DayCell(
                day = day,
                isToday = day.gregorianDate == today,
                hebrewPrimary = true,
                onClick = { onDayClick(day.gregorianDate) }
            )
        }
    }
}

@Composable
private fun DayCell(day: HebrewDay, isToday: Boolean, hebrewPrimary: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isToday) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
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
            Text(
                text = day.hebrewDayOfMonthFormatted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (hebrewPrimary) {
                Text(
                    text = "${day.gregorianDate.dayOfMonth}/${day.gregorianDate.monthValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                            alpha = 0.7f
                        )
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            } else {
                Text(
                    text = day.gregorianDate.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                            alpha = 0.7f
                        )
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
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
