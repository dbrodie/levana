package ninja.doskey.app.levana.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ninja.doskey.app.levana.data.AgendaRepository
import ninja.doskey.app.levana.data.EventDataChanges
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.domain.agenda.AgendaDay
import ninja.doskey.app.levana.domain.agenda.AgendaSearch
import ninja.doskey.app.levana.domain.model.UserPreferences
import ninja.doskey.app.levana.domain.model.displayCity
import ninja.doskey.app.levana.ui.ChangeRefresher
import ninja.doskey.app.levana.ui.otherClockCityName
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The agenda: a window of days around the device's today, extended a chunk at a time as the
 * list nears either end, so it has no fixed end in either direction. A jump outside the
 * window (date picker, search) loads a new window around that day.
 */
class AgendaViewModel(
    private val agendaRepository: AgendaRepository,
    private val preferencesRepository: PreferencesRepository,
    private val deviceContext: DeviceContext,
    eventDataChanges: EventDataChanges
) : ViewModel() {

    private val _state = MutableStateFlow(AgendaState())
    val state: StateFlow<AgendaState> = _state.asStateFlow()

    private var prefs: UserPreferences? = null
    private var windowJob: Job? = null
    private var earlierJob: Job? = null
    private var laterJob: Job? = null
    private var searchJob: Job? = null
    /** Bumped whenever the window is replaced, so an edge load of the old one is dropped. */
    private var windowVersion = 0
    private var scrollIds = 0L
    /** The days of the search range, loaded once per search and dropped when data changes. */
    private var searchDays: List<AgendaDay>? = null

    private val changeRefresher = ChangeRefresher(viewModelScope, eventDataChanges, refresh = ::refresh)

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect(::onPreferences)
        }
        viewModelScope.launch {
            deviceContext.todayUpdates.collect { today ->
                if (prefs != null) _state.value = _state.value.copy(today = today)
            }
        }
    }

    fun onIntent(intent: AgendaIntent) {
        when (intent) {
            is AgendaIntent.LoadEarlier -> loadEarlier()
            is AgendaIntent.LoadLater -> loadLater()
            is AgendaIntent.SetHebrewLeading -> {
                // Shared with the Month screen's toggle
                if (intent.hebrew != prefs?.calendarHebrewMode) {
                    viewModelScope.launch { preferencesRepository.saveCalendarHebrewMode(intent.hebrew) }
                }
            }
            is AgendaIntent.GoToToday -> today()?.let(::jumpTo)
            is AgendaIntent.OpenDatePicker -> _state.value = _state.value.copy(datePickerDate = intent.date)
            is AgendaIntent.CloseDatePicker -> _state.value = _state.value.copy(datePickerDate = null)
            is AgendaIntent.GoToDate -> {
                _state.value = _state.value.copy(datePickerDate = null)
                jumpTo(intent.date)
            }
            is AgendaIntent.ScrollHandled -> {
                if (_state.value.scrollRequest?.id == intent.id) {
                    _state.value = _state.value.copy(scrollRequest = null)
                }
            }
            is AgendaIntent.OpenSearch -> openSearch()
            is AgendaIntent.CloseSearch -> closeSearch()
            is AgendaIntent.SetSearchQuery -> setQuery(intent.query)
            is AgendaIntent.SelectSearchResult -> {
                closeSearch()
                jumpTo(intent.date)
            }
            is AgendaIntent.Resume -> {
                // The process may have been frozen across midnight or a time-zone change
                today()?.let { _state.value = _state.value.copy(today = it) }
                changeRefresher.onResume()
            }
            is AgendaIntent.Pause -> changeRefresher.onPause()
        }
    }

    private fun today(): LocalDate? = prefs?.let { deviceContext.now(it).today }

    private fun onPreferences(new: UserPreferences) {
        val old = prefs
        prefs = new
        val device = deviceContext.now(new)
        _state.value = _state.value.copy(
            today = device.today,
            hebrewLeading = new.calendarHebrewMode,
            otherClockCityName = otherClockCityName(new.displayCity, device.zone)
        )
        when {
            // The agenda opens on today
            old == null -> loadWindow(device.today)
            // The leading calendar only regroups the same days
            old.copy(calendarHebrewMode = new.calendarHebrewMode) != new -> refresh()
        }
    }

    /** Reloads the shown window and any search in place, keeping the scroll position. */
    private fun refresh() {
        val current = _state.value
        val start = current.start
        val end = current.end
        if (prefs == null || start == null || end == null) return
        searchDays = null
        if (current.search != null) loadSearch()
        loadWindow(start, end, scrollTo = null, keepContent = true)
    }

    private fun jumpTo(date: LocalDate) {
        val current = _state.value
        val start = current.start
        val end = current.end
        if (start != null && end != null && !date.isBefore(start) && !date.isAfter(end) && !current.isLoading) {
            _state.value = current.copy(scrollRequest = ScrollRequest(date, ++scrollIds))
        } else {
            loadWindow(date)
        }
    }

    private fun loadWindow(anchor: LocalDate) =
        loadWindow(anchor.minusDays(CHUNK_DAYS), anchor.plusDays(2 * CHUNK_DAYS - 1), scrollTo = anchor, keepContent = false)

    private fun loadWindow(start: LocalDate, end: LocalDate, scrollTo: LocalDate?, keepContent: Boolean) {
        val prefs = prefs ?: return
        windowJob?.cancel()
        earlierJob?.cancel()
        laterJob?.cancel()
        val version = ++windowVersion
        if (!keepContent) {
            _state.value = _state.value.copy(isLoading = true, loadingEarlier = false, loadingLater = false)
        }
        windowJob = viewModelScope.launch {
            val range = agendaRepository.load(start, end, prefs)
            if (version != windowVersion) return@launch
            _state.value = _state.value.copy(
                days = range.days,
                start = start,
                end = end,
                isLoading = false,
                loadingEarlier = false,
                loadingLater = false,
                calendarPermission = range.calendarPermission,
                scrollRequest = scrollTo?.let { ScrollRequest(it, ++scrollIds) } ?: _state.value.scrollRequest
            )
        }
    }

    private fun loadEarlier() {
        val current = _state.value
        val start = current.start ?: return
        val prefs = prefs ?: return
        if (current.isLoading || earlierJob?.isActive == true) return
        val version = windowVersion
        val from = start.minusDays(CHUNK_DAYS)
        _state.value = current.copy(loadingEarlier = true)
        earlierJob = viewModelScope.launch {
            val range = agendaRepository.load(from, start.minusDays(1), prefs)
            if (version != windowVersion) return@launch
            val latest = _state.value
            _state.value = latest.copy(
                days = range.days + latest.days,
                start = from,
                loadingEarlier = false,
                calendarPermission = range.calendarPermission
            )
        }
    }

    private fun loadLater() {
        val current = _state.value
        val end = current.end ?: return
        val prefs = prefs ?: return
        if (current.isLoading || laterJob?.isActive == true) return
        val version = windowVersion
        val to = end.plusDays(CHUNK_DAYS)
        _state.value = current.copy(loadingLater = true)
        laterJob = viewModelScope.launch {
            val range = agendaRepository.load(end.plusDays(1), to, prefs)
            if (version != windowVersion) return@launch
            val latest = _state.value
            _state.value = latest.copy(
                days = latest.days + range.days,
                end = to,
                loadingLater = false,
                calendarPermission = range.calendarPermission
            )
        }
    }

    private fun openSearch() {
        if (_state.value.search != null) return
        _state.value = _state.value.copy(search = AgendaSearchState())
        loadSearch()
    }

    private fun closeSearch() {
        searchJob?.cancel()
        _state.value = _state.value.copy(search = null)
    }

    private fun loadSearch() {
        val prefs = prefs ?: return
        val today = today() ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val cached = searchDays
            val days = cached ?: AgendaSearch.range(today).let { range ->
                agendaRepository.load(range.start, range.endInclusive, prefs).days
            }
            searchDays = days
            val search = _state.value.search ?: return@launch
            _state.value = _state.value.copy(
                search = search.copy(results = AgendaSearch.search(days, search.query), isLoading = false)
            )
        }
    }

    private fun setQuery(query: String) {
        val search = _state.value.search ?: return
        val days = searchDays
        _state.value = _state.value.copy(
            search = search.copy(
                query = query,
                results = if (days != null) AgendaSearch.search(days, query) else search.results
            )
        )
    }

    companion object {
        /** Days loaded at a time; the first window spans one chunk back and two ahead. */
        const val CHUNK_DAYS = 42L
    }
}
