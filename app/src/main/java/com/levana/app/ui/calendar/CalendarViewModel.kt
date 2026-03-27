package com.levana.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levana.app.data.CalendarRepository
import com.levana.app.data.ContactBirthdayRepository
import com.levana.app.data.PersonalEventRepository
import com.levana.app.data.PreferencesRepository
import com.levana.app.data.SystemCalendarRepository
import com.levana.app.domain.model.HebrewDay
import com.levana.app.domain.model.HebrewYearMonth
import com.levana.app.domain.model.UserPreferences
import com.levana.app.domain.model.activeLocation
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    private val systemCalendarRepository: SystemCalendarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private val _hebrewScrollTarget = Channel<HebrewYearMonth>(Channel.CONFLATED)
    val hebrewScrollTarget = _hebrewScrollTarget.receiveAsFlow()

    private val _gregorianScrollTarget = Channel<YearMonth>(Channel.CONFLATED)
    val gregorianScrollTarget = _gregorianScrollTarget.receiveAsFlow()

    private var currentPrefs = UserPreferences()
    private var loadJob: Job? = null
    private var adjacentJob: Job? = null

    init {
        observePreferences()
    }

    private fun today(): LocalDate = currentPrefs.devDateOverride ?: LocalDate.now()

    fun onIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.LoadToday -> {
                if (currentPrefs.calendarHebrewMode) {
                    loadHebrewMonth(HebrewYearMonth.from(today()))
                } else {
                    loadMonth(YearMonth.from(today()))
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
            is CalendarIntent.ToggleCalendarHebrewMode -> {
                viewModelScope.launch {
                    preferencesRepository.saveCalendarHebrewMode(!currentPrefs.calendarHebrewMode)
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
                    loadHebrewMonth(HebrewYearMonth.from(date))
                } else {
                    loadMonth(YearMonth.from(date))
                }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                val modeChanged =
                    currentPrefs.calendarHebrewMode != prefs.calendarHebrewMode
                currentPrefs = prefs
                _state.value = _state.value.copy(
                    locationName = prefs.activeLocation?.name ?: "",
                    calendarHebrewMode = prefs.calendarHebrewMode
                )
                if (modeChanged || _state.value.monthDays.isEmpty()) {
                    onIntent(CalendarIntent.LoadToday)
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
            currentPrefs.isInIsrael
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

    private fun loadMonth(yearMonth: YearMonth) {
        loadJob?.cancel()
        adjacentJob?.cancel()

        _state.value = _state.value.copy(
            isLoading = true,
            currentMonth = yearMonth,
            monthDays = emptyList()
        )

        loadJob = viewModelScope.launch {
            val markedDays = loadMonthDaysOnly(yearMonth)
            val hebrewMonthHeader = buildHebrewMonthHeader(markedDays)
            _state.value = _state.value.copy(
                monthDays = markedDays,
                today = currentPrefs.devDateOverride ?: LocalDate.now(),
                hebrewMonthHeader = hebrewMonthHeader,
                isLoading = false,
                gregorianMonthCache = mapOf(yearMonth to markedDays)
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
            currentPrefs.isInIsrael
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

    private fun loadHebrewMonth(hym: HebrewYearMonth) {
        loadJob?.cancel()
        adjacentJob?.cancel()

        _state.value = _state.value.copy(
            isLoading = true,
            hebrewYearMonth = hym,
            monthDays = emptyList()
        )

        loadJob = viewModelScope.launch {
            val markedDays = loadHebrewMonthDaysOnly(hym)
            val hebrewMonthName = calendarRepository.getHebrewMonthName(hym)
            val hebrewHeader = "$hebrewMonthName ${hym.year}"
            val gregHeader = buildGregorianRange(markedDays)
            _state.value = _state.value.copy(
                monthDays = markedDays,
                today = currentPrefs.devDateOverride ?: LocalDate.now(),
                hebrewMonthHeader = hebrewHeader,
                gregorianHeader = gregHeader,
                isLoading = false,
                hebrewMonthCache = mapOf(hym to markedDays)
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

    private fun buildGregorianRange(days: List<HebrewDay>): String {
        if (days.isEmpty()) return ""
        val first = days.first().gregorianDate
        val last = days.last().gregorianDate
        val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        return if (first.month == last.month &&
            first.year == last.year
        ) {
            first.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        } else {
            "${first.format(fmt)} \u2013 ${last.format(fmt)}"
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
                "$firstMonthName\u2013$lastMonthName $firstYear"
            } else {
                "$firstMonthName $firstYear \u2013 " +
                    "$lastMonthName $lastYear"
            }
        }
    }
}
