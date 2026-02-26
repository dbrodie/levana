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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private var currentPrefs = UserPreferences()

    init {
        observePreferences()
    }

    fun onIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.LoadToday -> {
                if (currentPrefs.hebrewPrimary) {
                    loadHebrewMonth(HebrewYearMonth.now())
                } else {
                    loadMonth(YearMonth.now())
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
                    ?: HebrewYearMonth.now()
                loadHebrewMonth(current.next())
            }
            is CalendarIntent.PreviousHebrewMonth -> {
                val current = _state.value.hebrewYearMonth
                    ?: HebrewYearMonth.now()
                loadHebrewMonth(current.previous())
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                val modeChanged =
                    currentPrefs.hebrewPrimary != prefs.hebrewPrimary
                currentPrefs = prefs
                _state.value = _state.value.copy(
                    locationName = prefs.location?.name ?: "",
                    hebrewPrimary = prefs.hebrewPrimary
                )
                if (modeChanged || _state.value.monthDays.isEmpty()) {
                    onIntent(CalendarIntent.LoadToday)
                } else if (prefs.hebrewPrimary) {
                    _state.value.hebrewYearMonth?.let {
                        loadHebrewMonth(it)
                    }
                } else {
                    loadMonth(_state.value.currentMonth)
                }
            }
        }
    }

    private fun loadMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val monthDays = calendarRepository.getMonthDays(
                yearMonth,
                currentPrefs.isInIsrael
            )
            val hebrewMonthHeader = buildHebrewMonthHeader(monthDays)

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

            val markedDays = monthDays.map { day ->
                day.copy(
                    hasPersonalEvent = allEventDates.contains(
                        day.gregorianDate
                    ),
                    systemEventColors = systemEventColors[
                        day.gregorianDate
                    ] ?: emptyList()
                )
            }

            _state.value = _state.value.copy(
                currentMonth = yearMonth,
                monthDays = markedDays,
                today = currentPrefs.devDateOverride ?: LocalDate.now(),
                hebrewMonthHeader = hebrewMonthHeader,
                isLoading = false
            )
        }
    }

    private fun loadHebrewMonth(hym: HebrewYearMonth) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val monthDays = calendarRepository.getHebrewMonthDays(
                hym,
                currentPrefs.isInIsrael
            )
            val hebrewMonthName =
                calendarRepository.getHebrewMonthName(hym)
            val hebrewHeader = "$hebrewMonthName ${hym.year}"
            val gregHeader = buildGregorianRange(monthDays)

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

            val markedDays = monthDays.map { day ->
                day.copy(
                    hasPersonalEvent = allEventDays.contains(day.day),
                    systemEventColors = systemEventColors[
                        day.gregorianDate
                    ] ?: emptyList()
                )
            }

            _state.value = _state.value.copy(
                hebrewYearMonth = hym,
                monthDays = markedDays,
                today = currentPrefs.devDateOverride ?: LocalDate.now(),
                hebrewMonthHeader = hebrewHeader,
                gregorianHeader = gregHeader,
                isLoading = false
            )
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
