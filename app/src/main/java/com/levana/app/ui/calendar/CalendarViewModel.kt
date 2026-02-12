package com.levana.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.CalendarRepository
import com.levana.app.domain.model.HebrewDay
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    init {
        onIntent(CalendarIntent.LoadToday)
    }

    fun onIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.LoadToday -> loadMonth(YearMonth.now())
            is CalendarIntent.LoadMonth -> loadMonth(intent.yearMonth)
            is CalendarIntent.NextMonth -> loadMonth(_state.value.currentMonth.plusMonths(1))
            is CalendarIntent.PreviousMonth -> loadMonth(_state.value.currentMonth.minusMonths(1))
        }
    }

    private fun loadMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val monthDays = calendarRepository.getMonthDays(yearMonth)
            val hebrewMonthHeader = buildHebrewMonthHeader(monthDays)

            _state.value = _state.value.copy(
                currentMonth = yearMonth,
                monthDays = monthDays,
                today = LocalDate.now(),
                hebrewMonthHeader = hebrewMonthHeader,
                isLoading = false
            )
        }
    }

    private fun buildHebrewMonthHeader(days: List<HebrewDay>): String {
        if (days.isEmpty()) return ""
        val firstDay = days.first()
        val lastDay = days.last()

        val firstMonthName = firstDay.month.name.lowercase()
            .replaceFirstChar { it.uppercase() }
            .replace("_", " ")
        val firstYear = firstDay.year

        return if (firstDay.month == lastDay.month) {
            "$firstMonthName $firstYear"
        } else {
            val lastMonthName = lastDay.month.name.lowercase()
                .replaceFirstChar { it.uppercase() }
                .replace("_", " ")
            val lastYear = lastDay.year
            if (firstYear == lastYear) {
                "$firstMonthName–$lastMonthName $firstYear"
            } else {
                "$firstMonthName $firstYear – $lastMonthName $lastYear"
            }
        }
    }
}
