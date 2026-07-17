package ninja.doskey.app.levana.ui.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.CalendarEvent
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.activeLocation
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DayDetailViewModel(
    private val calendarRepository: CalendarRepository,
    private val zmanimRepository: ZmanimRepository,
    private val preferencesRepository: PreferencesRepository,
    private val personalEventRepository: PersonalEventRepository,
    private val contactBirthdayRepository: ContactBirthdayRepository,
    private val systemCalendarRepository: SystemCalendarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DayDetailState())
    val state: StateFlow<DayDetailState> = _state.asStateFlow()

    private var lastLoadedDate: LocalDate? = null

    init {
        viewModelScope.launch {
            preferencesRepository.preferences
                .distinctUntilChanged()
                .collect { lastLoadedDate?.let { date -> loadDay(date) } }
        }
    }

    fun onIntent(intent: DayDetailIntent) {
        when (intent) {
            is DayDetailIntent.LoadDay -> loadDay(intent.date)
        }
    }

    private fun loadDay(date: LocalDate) {
        lastLoadedDate = date
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val prefs = preferencesRepository.preferences.first()
            val location = prefs.activeLocation ?: Location.JERUSALEM
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

            val personalEvents =
                personalEventRepository.getEventsForDate(date)
            val contactBirthdays = try {
                contactBirthdayRepository.getBirthdaysForDate(date)
            } catch (_: SecurityException) {
                emptyList()
            }

            val calendarEvents = mutableListOf<CalendarEvent>()
            contactBirthdays.forEach {
                calendarEvents.add(CalendarEvent.Birthday(it))
            }
            personalEvents.forEach {
                calendarEvents.add(CalendarEvent.CustomEvent(it))
            }

            val systemEvents = try {
                systemCalendarRepository.getEventsForDate(
                    date,
                    prefs.selectedCalendarIds
                )
            } catch (_: SecurityException) {
                emptyList()
            }

            val allZmanim = zmanimRepository.getZmanim(date, location, prefs.candleLightingOffset)
            val halachicTimes = allZmanim.filter { it.name in prefs.selectedZmanim }

            _state.value = DayDetailState(
                dayInfo = dayInfo.copy(shabbatInfo = shabbatInfo),
                calendarEvents = calendarEvents,
                systemEvents = systemEvents,
                halachicTimes = halachicTimes,
                isLoading = false
            )
        }
    }
}
