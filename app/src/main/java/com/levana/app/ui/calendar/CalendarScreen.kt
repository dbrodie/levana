package com.levana.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levana.app.domain.model.HebrewDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

private const val PAGER_PAGE_COUNT = 1200
private const val PAGER_INITIAL_PAGE = 600

@Composable
fun CalendarScreen(modifier: Modifier = Modifier, viewModel: CalendarViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CalendarContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
fun CalendarContent(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = PAGER_INITIAL_PAGE,
        pageCount = { PAGER_PAGE_COUNT }
    )

    val baseMonth = YearMonth.now()

    // Load month when pager settles on a new page
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val offset = page - PAGER_INITIAL_PAGE
            val targetMonth = baseMonth.plusMonths(offset.toLong())
            if (targetMonth != state.currentMonth) {
                onIntent(CalendarIntent.LoadMonth(targetMonth))
            }
        }
    }

    // Sync pager when navigating via arrows
    val expectedPage = PAGER_INITIAL_PAGE +
        ((state.currentMonth.year - baseMonth.year) * 12) +
        (state.currentMonth.monthValue - baseMonth.monthValue)

    LaunchedEffect(expectedPage) {
        if (pagerState.settledPage != expectedPage) {
            pagerState.animateScrollToPage(expectedPage)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        MonthHeader(
            state = state,
            onPrevious = { onIntent(CalendarIntent.PreviousMonth) },
            onNext = { onIntent(CalendarIntent.NextMonth) }
        )

        DayOfWeekHeader()

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
                    MonthGrid(
                        monthDays = state.monthDays,
                        currentMonth = state.currentMonth,
                        today = state.today
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(state: CalendarState, onPrevious: () -> Unit, onNext: () -> Unit) {
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
private fun DayOfWeekHeader() {
    val daysOfWeek = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )

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
private fun MonthGrid(monthDays: List<HebrewDay>, currentMonth: YearMonth, today: LocalDate) {
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek
    // Sunday = 0 offset for our grid (DayOfWeek.SUNDAY.value is 7)
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
                isToday = day.gregorianDate == today
            )
        }
    }
}

@Composable
private fun DayCell(day: HebrewDay, isToday: Boolean) {
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
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            Text(
                text = day.gregorianDate.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isToday) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
