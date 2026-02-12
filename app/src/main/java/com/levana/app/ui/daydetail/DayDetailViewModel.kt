package com.levana.app.ui.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.CalendarRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DayDetailViewModel(
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DayDetailState())
    val state: StateFlow<DayDetailState> = _state.asStateFlow()

    fun onIntent(intent: DayDetailIntent) {
        when (intent) {
            is DayDetailIntent.LoadDay -> loadDay(intent.date)
        }
    }

    private fun loadDay(date: LocalDate) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val dayInfo = calendarRepository.getDayInfo(date)
            _state.value = DayDetailState(
                dayInfo = dayInfo,
                isLoading = false
            )
        }
    }
}
