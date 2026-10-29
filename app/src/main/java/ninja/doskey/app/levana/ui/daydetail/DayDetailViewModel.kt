package ninja.doskey.app.levana.ui.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventDataChanges
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.CalendarEvent
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.model.displayCity
import ninja.doskey.app.levana.ui.otherClockCityName
import ninja.doskey.app.levana.ui.ChangeRefresher
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class DayDetailViewModel(
    private val calendarRepository: CalendarRepository,
    private val zmanimRepository: ZmanimRepository,
    private val preferencesRepository: PreferencesRepository,
    private val personalEventRepository: PersonalEventRepository,
    private val contactBirthdayRepository: ContactBirthdayRepository,
    private val systemCalendarRepository: SystemCalendarRepository,
    private val deviceContext: DeviceContext,
    eventDataChanges: EventDataChanges
) : ViewModel() {

    private val _state = MutableStateFlow(DayDetailState())
    val state: StateFlow<DayDetailState> = _state.asStateFlow()

    private var lastLoadedDate: LocalDate? = null
    private var loadJob: Job? = null

    // Reloads the open day in place when its events change.
    private val changeRefresher = ChangeRefresher(viewModelScope, eventDataChanges) {
        lastLoadedDate?.let { date -> loadDay(date, keepContent = true) }
    }

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
            is DayDetailIntent.Resume -> changeRefresher.onResume()
            is DayDetailIntent.Pause -> changeRefresher.onPause()
        }
    }

    private fun loadDay(date: LocalDate, keepContent: Boolean = false) {
        lastLoadedDate = date
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!keepContent) _state.value = _state.value.copy(isLoading = true)
            val prefs = preferencesRepository.preferences.first()
            // The day panel's times are the selected city's; everything else follows the device
            val location = prefs.displayCity
            val dayInfo = calendarRepository.getDayInfo(
                date,
                prefs.isInIsrael,
                prefs.showModernIsraeliHolidays
            )
            val shabbatInfo = zmanimRepository.getShabbatInfo(
                date,
                location,
                prefs.candleLightingOffset,
                prefs.isInIsrael
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
                calendarEvents.add(CalendarEvent.Birthday(it.birthday, it.label))
            }
            personalEvents.forEach {
                calendarEvents.add(CalendarEvent.CustomEvent(it.event, it.label))
            }

            val systemEvents = try {
                systemCalendarRepository.getEventsForDate(
                    date,
                    prefs.selectedCalendarIds
                )
            } catch (_: SecurityException) {
                emptyList()
            }

            val allZmanim = zmanimRepository.getZmanim(
                date,
                location,
                prefs.candleLightingOffset,
                prefs.isInIsrael
            )
            val halachicTimes = allZmanim.filter { it.name in prefs.selectedZmanim }

            _state.value = DayDetailState(
                dayInfo = dayInfo.copy(shabbatInfo = shabbatInfo),
                calendarEvents = calendarEvents,
                systemEvents = systemEvents,
                allZmanim = allZmanim,
                halachicTimes = halachicTimes,
                otherClockCityName = otherClockCityName(location, deviceContext.now(prefs).zone),
                isLoading = false
            )
        }
    }
}
