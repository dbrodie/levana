package com.levana.app.ui.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.CalendarRepository
import com.levana.app.data.PersonalEventRepository
import com.levana.app.data.PreferencesRepository
import com.levana.app.data.ZmanimRepository
import com.levana.app.domain.model.Location
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DayDetailViewModel(
    private val calendarRepository: CalendarRepository,
    private val zmanimRepository: ZmanimRepository,
    private val preferencesRepository: PreferencesRepository,
    private val personalEventRepository: PersonalEventRepository
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
            val prefs = preferencesRepository.preferences.first()
            val location = prefs.location ?: Location.JERUSALEM
            val dayInfo = calendarRepository.getDayInfo(
                date,
                prefs.isInIsrael,
                prefs.showModernIsraeliHolidays
            )
            val shabbatInfo = zmanimRepository.getShabbatInfo(
                date,
                location,
                prefs.candleLightingOffset
            )
            val personalEvents = personalEventRepository.getEventsForDate(date)
            _state.value = DayDetailState(
                dayInfo = dayInfo.copy(shabbatInfo = shabbatInfo),
                personalEvents = personalEvents,
                isLoading = false
            )
        }
    }
}
