package com.levana.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.CalendarRepository
import java.time.LocalDate
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
            is CalendarIntent.LoadToday -> loadToday()
        }
    }

    private fun loadToday() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val hebrewDay = calendarRepository.getHebrewDay(LocalDate.now())
            _state.value = CalendarState(
                hebrewDay = hebrewDay,
                isLoading = false
            )
        }
    }
}
