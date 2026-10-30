package ninja.doskey.app.levana.ui.datepicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.DeviceContext
import java.time.YearMonth

/**
 * Holds one picking session of the shared date picker. The rules live in
 * [DatePickerReducer]; this adds the device's today and Israel/Diaspora (TASK-5), read
 * once per session, and the shown month's holidays from the app's calendar repository.
 */
class DatePickerViewModel(
    private val calendarRepository: CalendarRepository,
    private val preferencesRepository: PreferencesRepository,
    private val deviceContext: DeviceContext
) : ViewModel() {

    // Null until a session's device settings are read
    private val _state = MutableStateFlow<DatePickerState?>(null)
    val state: StateFlow<DatePickerState?> = _state.asStateFlow()

    private var inIsrael = false
    private var showModernIsraeli = true
    private var openJob: Job? = null

    fun onIntent(intent: DatePickerIntent) {
        if (intent is DatePickerIntent.Open) {
            open(intent)
            return
        }
        val current = _state.value ?: return
        publish(DatePickerReducer.reduce(current, intent), previous = current)
    }

    private fun open(intent: DatePickerIntent.Open) {
        _state.value = null
        openJob?.cancel()
        openJob = viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            inIsrael = prefs.isInIsrael
            showModernIsraeli = prefs.showModernIsraeliHolidays
            val today = deviceContext.now(prefs).today
            publish(
                DatePickerReducer.initial(intent.initialDate, intent.leading, today, intent.session),
                previous = null
            )
        }
    }

    private fun publish(state: DatePickerState, previous: DatePickerState?) {
        val sameMonth = previous != null && previous.leading == state.leading && when (state.leading) {
            LeadingCalendar.HEBREW -> previous.hebrewMonth == state.hebrewMonth
            LeadingCalendar.GREGORIAN -> previous.gregorianMonth == state.gregorianMonth
        }
        val days = if (sameMonth) previous!!.days else when (state.leading) {
            LeadingCalendar.HEBREW ->
                calendarRepository.getHebrewMonthDays(state.hebrewMonth, inIsrael, showModernIsraeli)
            LeadingCalendar.GREGORIAN ->
                calendarRepository.getMonthDays(state.gregorianMonth, inIsrael, showModernIsraeli)
        }
        val selectedDay = days.firstOrNull { it.gregorianDate == state.selection }
            ?: previous?.selectedDay?.takeIf { it.gregorianDate == state.selection }
            ?: calendarRepository.getMonthDays(YearMonth.from(state.selection), inIsrael, showModernIsraeli)
                .first { it.gregorianDate == state.selection }
        _state.value = state.copy(days = days, selectedDay = selectedDay)
    }
}
