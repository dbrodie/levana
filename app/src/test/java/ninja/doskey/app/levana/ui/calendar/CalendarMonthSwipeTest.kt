package ninja.doskey.app.levana.ui.calendar

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventDataChanges
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.data.db.LevanaDatabase
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Swiping to a prefetched month must never blank the pager page or drop the neighbours (TASK-40). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CalendarMonthSwipeTest {

    private object NoChanges : EventDataChanges {
        override val changes: Flow<Unit> = emptyFlow()
        override fun recheckPermissions() = Unit
    }

    private lateinit var db: LevanaDatabase
    private lateinit var preferences: PreferencesRepository
    private val store = ViewModelStore()
    private var recorder: Job? = null

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, LevanaDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        preferences = PreferencesRepository(app, CityRepository(app))
    }

    @After
    fun tearDown() {
        recorder?.cancel()
        store.clear()
        db.close()
    }

    private fun calendarViewModel(hebrew: Boolean): CalendarViewModel {
        runBlocking { preferences.saveCalendarHebrewMode(hebrew) }
        val app = RuntimeEnvironment.getApplication()
        val vm = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = CalendarViewModel(
                CalendarRepository(), preferences, PersonalEventRepository(db.personalEventDao()),
                ContactBirthdayRepository(app), SystemCalendarRepository(app),
                DeviceContext(preferences.preferences), NoChanges
            ) as V
        })[CalendarViewModel::class.java]
        awaitUntil {
            val state = vm.state.value
            state.calendarHebrewMode == hebrew && !state.isLoading &&
                (if (hebrew) state.hebrewMonthCache else state.gregorianMonthCache).size == 3
        }
        return vm
    }

    /** Records every state the view model publishes, as the screen would observe it. */
    private fun CalendarViewModel.record(): List<CalendarState> {
        val states = mutableListOf<CalendarState>()
        recorder = CoroutineScope(Dispatchers.Unconfined).launch { state.collect { states += it } }
        return states
    }

    @Test
    fun `swiping to a cached Gregorian month never shows an empty or loading page`() {
        val calendar = calendarViewModel(hebrew = false)
        val start = calendar.state.value.currentMonth
        val next = start.plusMonths(1)
        val cachedNext = calendar.state.value.gregorianMonthCache.getValue(next)
        val states = calendar.record()

        calendar.onIntent(CalendarIntent.LoadMonth(next))

        val switched = calendar.state.value
        assertEquals(next, switched.currentMonth)
        assertEquals("the cached month is shown at once", cachedNext, switched.monthDays)
        assertTrue(
            "neighbours are kept while loading",
            switched.gregorianMonthCache.keys.containsAll(listOf(start, next))
        )

        awaitUntil { calendar.state.value.gregorianMonthCache.keys == setOf(start, next, next.plusMonths(1)) }
        for (state in states) {
            assertFalse("never loading", state.isLoading)
            assertTrue("never empty", state.monthDays.isNotEmpty())
            if (state.currentMonth == next) {
                assertTrue(
                    "the month swiped away from stays cached",
                    state.gregorianMonthCache.keys.containsAll(listOf(start, next))
                )
            }
        }
    }

    @Test
    fun `swiping to a cached Hebrew month never shows an empty or loading page`() {
        val calendar = calendarViewModel(hebrew = true)
        val start = calendar.state.value.hebrewYearMonth!!
        val previous = start.previous()
        val cachedPrevious = calendar.state.value.hebrewMonthCache.getValue(previous)
        val states = calendar.record()

        calendar.onIntent(CalendarIntent.LoadHebrewMonth(previous))

        val switched = calendar.state.value
        assertEquals(previous, switched.hebrewYearMonth)
        assertEquals("the cached month is shown at once", cachedPrevious, switched.monthDays)
        assertTrue(
            "neighbours are kept while loading",
            switched.hebrewMonthCache.keys.containsAll(listOf(start, previous))
        )

        awaitUntil {
            calendar.state.value.hebrewMonthCache.keys == setOf(previous.previous(), previous, start)
        }
        for (state in states) {
            assertFalse("never loading", state.isLoading)
            assertTrue("never empty", state.monthDays.isNotEmpty())
            if (state.hebrewYearMonth == previous) {
                assertTrue(
                    "the month swiped away from stays cached",
                    state.hebrewMonthCache.keys.containsAll(listOf(start, previous))
                )
            }
        }
    }

    @Test
    fun `jumping to an uncached month still shows the loading state`() {
        val calendar = calendarViewModel(hebrew = false)
        val target = LocalDate.now().plusYears(3)
        val states = calendar.record()

        calendar.onIntent(CalendarIntent.GoToDate(target))

        awaitUntil { !calendar.state.value.isLoading }
        assertEquals(YearMonth.from(target), calendar.state.value.currentMonth)
        assertEquals(YearMonth.from(target), YearMonth.from(calendar.state.value.monthDays.first().gregorianDate))
        // In tests the load may finish inside onIntent, so look for the loading state among the emissions.
        assertTrue(states.any { it.isLoading && it.monthDays.isEmpty() })
    }

    @Test
    fun `jumping to an uncached Hebrew month still shows the loading state`() {
        val calendar = calendarViewModel(hebrew = true)
        val target = LocalDate.now().plusYears(3)
        val states = calendar.record()

        calendar.onIntent(CalendarIntent.GoToDate(target))

        awaitUntil { !calendar.state.value.isLoading }
        assertEquals(HebrewYearMonth.from(target), calendar.state.value.hebrewYearMonth)
        assertTrue(calendar.state.value.monthDays.any { it.gregorianDate == target })
        assertTrue(states.any { it.isLoading && it.monthDays.isEmpty() })
    }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (true) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))
            if (condition()) return
            assertTrue("timed out", System.currentTimeMillis() < deadline)
            Thread.sleep(10)
        }
    }
}
