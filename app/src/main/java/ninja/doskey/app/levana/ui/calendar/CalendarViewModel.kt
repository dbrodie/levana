package ninja.doskey.app.levana.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventDataChanges
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.domain.model.HebrewDay
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.activeLocation
import ninja.doskey.app.levana.ui.ChangeRefresher
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val preferencesRepository: PreferencesRepository,
    private val personalEventRepository: PersonalEventRepository,
    private val contactBirthdayRepository: ContactBirthdayRepository,
    private val systemCalendarRepository: SystemCalendarRepository,
    eventDataChanges: EventDataChanges
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private val _hebrewScrollTarget = Channel<HebrewYearMonth>(Channel.CONFLATED)
    val hebrewScrollTarget = _hebrewScrollTarget.receiveAsFlow()

    private val _gregorianScrollTarget = Channel<YearMonth>(Channel.CONFLATED)
    val gregorianScrollTarget = _gregorianScrollTarget.receiveAsFlow()

    private var currentPrefs = UserPreferences()
    private var preferencesLoaded = false
    private var loadJob: Job? = null
    private var adjacentJob: Job? = null

    private val changeRefresher = ChangeRefresher(viewModelScope, eventDataChanges, refresh = ::refresh)

    init {
        observePreferences()
    }

    private fun today(): LocalDate = currentPrefs.devDateOverride ?: LocalDate.now()

    fun onIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.LoadToday -> {
                if (currentPrefs.calendarHebrewMode) {
                    val hym = HebrewYearMonth.from(today())
                    _hebrewScrollTarget.trySend(hym)
                    loadHebrewMonth(hym)
                } else {
                    val todayMonth = YearMonth.from(today())
                    _gregorianScrollTarget.trySend(todayMonth)
                    loadMonth(todayMonth)
                }
            }
            is CalendarIntent.LoadMonth -> loadMonth(intent.yearMonth)
            is CalendarIntent.NextMonth ->
                loadMonth(_state.value.currentMonth.plusMonths(1))
            is CalendarIntent.PreviousMonth ->
                loadMonth(_state.value.currentMonth.minusMonths(1))
            is CalendarIntent.LoadHebrewMonth ->
                loadHebrewMonth(intent.hebrewYearMonth)
            is CalendarIntent.NextHebrewMonth -> {
                val current = _state.value.hebrewYearMonth
                    ?: HebrewYearMonth.from(today())
                loadHebrewMonth(current.next())
            }
            is CalendarIntent.PreviousHebrewMonth -> {
                val current = _state.value.hebrewYearMonth
                    ?: HebrewYearMonth.from(today())
                loadHebrewMonth(current.previous())
            }
            is CalendarIntent.GoToToday -> {
                val today = today()
                _state.value = _state.value.copy(selectedDate = today)
                if (currentPrefs.calendarHebrewMode) {
                    val hebrewToday = HebrewYearMonth.from(today)
                    _hebrewScrollTarget.trySend(hebrewToday)
                    loadHebrewMonth(hebrewToday)
                } else {
                    val gregorianToday = YearMonth.from(today)
                    _gregorianScrollTarget.trySend(gregorianToday)
                    loadMonth(gregorianToday)
                }
            }
            is CalendarIntent.SelectDay -> {
                _state.value = _state.value.copy(selectedDate = intent.date)
            }
            is CalendarIntent.SetCalendarHebrewMode -> {
                if (intent.hebrew != currentPrefs.calendarHebrewMode) {
                    viewModelScope.launch { preferencesRepository.saveCalendarHebrewMode(intent.hebrew) }
                }
            }
            is CalendarIntent.OpenGoToDateDialog -> {
                _state.value = _state.value.copy(showGoToDateDialog = true)
            }
            is CalendarIntent.CloseGoToDateDialog -> {
                _state.value = _state.value.copy(showGoToDateDialog = false)
            }
            is CalendarIntent.GoToDate -> {
                val date = intent.date
                _state.value = _state.value.copy(
                    selectedDate = date,
                    showGoToDateDialog = false
                )
                if (currentPrefs.calendarHebrewMode) {
                    val target = HebrewYearMonth.from(date)
                    _hebrewScrollTarget.trySend(target)
                    loadHebrewMonth(target)
                } else {
                    val target = YearMonth.from(date)
                    _gregorianScrollTarget.trySend(target)
                    loadMonth(target)
                }
            }
            is CalendarIntent.Resume -> changeRefresher.onResume()
            is CalendarIntent.Pause -> changeRefresher.onPause()
        }
    }

    /** Reloads the browsed month and its neighbours in place, keeping the selection. */
    private fun refresh() {
        // Before the first preferences the initial load has not started; it will read fresh data.
        if (!preferencesLoaded) return
        val current = _state.value
        if (currentPrefs.calendarHebrewMode) {
            current.hebrewYearMonth?.let { loadHebrewMonth(it, keepContent = true) }
        } else {
            loadMonth(current.currentMonth, keepContent = true)
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                val modeChanged =
                    currentPrefs.calendarHebrewMode != prefs.calendarHebrewMode
                currentPrefs = prefs
                preferencesLoaded = true
                _state.value = _state.value.copy(
                    locationName = prefs.activeLocation.name,
                    calendarHebrewMode = prefs.calendarHebrewMode
                )
                if (_state.value.monthDays.isEmpty()) {
                    onIntent(CalendarIntent.LoadToday)
                } else if (modeChanged) {
                    // Switching calendar systems follows the currently selected day,
                    // rather than unexpectedly returning a browsed calendar to today.
                    val selectedDate = _state.value.selectedDate
                    if (prefs.calendarHebrewMode) {
                        val target = HebrewYearMonth.from(selectedDate)
                        _hebrewScrollTarget.trySend(target)
                        loadHebrewMonth(target)
                    } else {
                        val target = YearMonth.from(selectedDate)
                        _gregorianScrollTarget.trySend(target)
                        loadMonth(target)
                    }
                } else if (prefs.calendarHebrewMode) {
                    _state.value.hebrewYearMonth?.let {
                        loadHebrewMonth(it)
                    }
                } else {
                    loadMonth(_state.value.currentMonth)
                }
            }
        }
    }

    private suspend fun loadMonthDaysOnly(yearMonth: YearMonth): List<HebrewDay> {
        val monthDays = calendarRepository.getMonthDays(
            yearMonth,
            currentPrefs.isInIsrael,
            currentPrefs.showModernIsraeliHolidays
        )

        val eventDates = personalEventRepository
            .getEventDatesForGregorianMonth(
                yearMonth.year,
                yearMonth.monthValue
            )

        val birthdayDates = try {
            contactBirthdayRepository
                .getBirthdayDatesForGregorianMonth(
                    yearMonth.year,
                    yearMonth.monthValue
                )
        } catch (_: SecurityException) {
            emptySet()
        }

        val allEventDates = eventDates + birthdayDates

        val systemEventColors = try {
            val start = yearMonth.atDay(1)
            val end = yearMonth.atEndOfMonth()
            systemCalendarRepository.getEventColorsForDateRange(
                start,
                end,
                currentPrefs.selectedCalendarIds
            )
        } catch (_: SecurityException) {
            emptyMap()
        }

        return monthDays.map { day ->
            day.copy(
                hasPersonalEvent = allEventDates.contains(day.gregorianDate),
                systemEventColors = systemEventColors[day.gregorianDate] ?: emptyList()
            )
        }
    }

    private fun loadMonth(yearMonth: YearMonth, keepContent: Boolean = false) {
        loadJob?.cancel()
        adjacentJob?.cancel()

        if (!keepContent) {
            _state.value = _state.value.copy(
                isLoading = true,
                currentMonth = yearMonth,
                monthDays = emptyList()
            )
        }

        loadJob = viewModelScope.launch {
            val markedDays = loadMonthDaysOnly(yearMonth)
            val current = _state.value
            _state.value = current.copy(
                monthDays = markedDays,
                today = currentPrefs.devDateOverride ?: LocalDate.now(),
                isLoading = false,
                gregorianMonthCache = if (keepContent) {
                    current.gregorianMonthCache + (yearMonth to markedDays)
                } else {
                    mapOf(yearMonth to markedDays)
                }
            )
        }

        adjacentJob = viewModelScope.launch {
            val prev = yearMonth.minusMonths(1)
            val next = yearMonth.plusMonths(1)
            val prevDays = loadMonthDaysOnly(prev)
            val nextDays = loadMonthDaysOnly(next)
            val current = _state.value
            if (current.currentMonth == yearMonth) {
                _state.value = current.copy(
                    gregorianMonthCache = mapOf(
                        prev to prevDays,
                        yearMonth to (current.gregorianMonthCache[yearMonth] ?: current.monthDays),
                        next to nextDays
                    )
                )
            }
        }
    }

    private suspend fun loadHebrewMonthDaysOnly(hym: HebrewYearMonth): List<HebrewDay> {
        val monthDays = calendarRepository.getHebrewMonthDays(
            hym,
            currentPrefs.isInIsrael,
            currentPrefs.showModernIsraeliHolidays
        )

        val eventDays = personalEventRepository
            .getEventDaysForHebrewMonth(hym.year, hym.jewishDateMonth)

        val birthdayDays = try {
            contactBirthdayRepository
                .getBirthdayDaysForHebrewMonth(
                    hym.year,
                    hym.jewishDateMonth
                )
        } catch (_: SecurityException) {
            emptySet()
        }

        val allEventDays = eventDays + birthdayDays

        val systemEventColors = try {
            if (monthDays.isNotEmpty()) {
                val start = monthDays.first().gregorianDate
                val end = monthDays.last().gregorianDate
                systemCalendarRepository.getEventColorsForDateRange(
                    start,
                    end,
                    currentPrefs.selectedCalendarIds
                )
            } else {
                emptyMap()
            }
        } catch (_: SecurityException) {
            emptyMap()
        }

        return monthDays.map { day ->
            day.copy(
                hasPersonalEvent = allEventDays.contains(day.day),
                systemEventColors = systemEventColors[day.gregorianDate] ?: emptyList()
            )
        }
    }

    private fun loadHebrewMonth(hym: HebrewYearMonth, keepContent: Boolean = false) {
        loadJob?.cancel()
        adjacentJob?.cancel()

        if (!keepContent) {
            _state.value = _state.value.copy(
                isLoading = true,
                hebrewYearMonth = hym,
                monthDays = emptyList()
            )
        }

        loadJob = viewModelScope.launch {
            val markedDays = loadHebrewMonthDaysOnly(hym)
            val current = _state.value
            _state.value = current.copy(
                monthDays = markedDays,
                today = currentPrefs.devDateOverride ?: LocalDate.now(),
                isLoading = false,
                hebrewMonthCache = if (keepContent) {
                    current.hebrewMonthCache + (hym to markedDays)
                } else {
                    mapOf(hym to markedDays)
                }
            )
        }

        adjacentJob = viewModelScope.launch {
            val prev = hym.previous()
            val next = hym.next()
            val prevDays = loadHebrewMonthDaysOnly(prev)
            val nextDays = loadHebrewMonthDaysOnly(next)
            val current = _state.value
            if (current.hebrewYearMonth == hym) {
                _state.value = current.copy(
                    hebrewMonthCache = mapOf(
                        prev to prevDays,
                        hym to (current.hebrewMonthCache[hym] ?: current.monthDays),
                        next to nextDays
                    )
                )
            }
        }
    }
}
