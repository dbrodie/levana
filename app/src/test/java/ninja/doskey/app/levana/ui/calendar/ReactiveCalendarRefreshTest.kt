package ninja.doskey.app.levana.ui.calendar

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.util.GregorianCalendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventDataChanges
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.data.db.LevanaDatabase
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
import ninja.doskey.app.levana.domain.model.CalendarEvent
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import ninja.doskey.app.levana.ui.ChangeRefresher
import ninja.doskey.app.levana.ui.daydetail.DayDetailIntent
import ninja.doskey.app.levana.ui.daydetail.DayDetailViewModel
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

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ReactiveCalendarRefreshTest {

    /** Stands in for the Room/provider observers: a test emits where a real change would. */
    private class FakeEventDataChanges : EventDataChanges {
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
        var permissionRechecks = 0
        override val changes: Flow<Unit> = signals
        override fun recheckPermissions() {
            permissionRechecks++
        }
        fun signal() = check(signals.tryEmit(Unit))
    }

    /** Counts full reads of the personal event table, i.e. loads that consult personal events. */
    private class CountingDao(private val dao: PersonalEventDao) : PersonalEventDao by dao {
        @Volatile var reads = 0
        override suspend fun getAllOnce(): List<PersonalEvent> {
            reads++
            return dao.getAllOnce()
        }
    }

    private lateinit var db: LevanaDatabase
    private lateinit var dao: CountingDao
    private lateinit var events: PersonalEventRepository
    private lateinit var preferences: PreferencesRepository
    private val changes = FakeEventDataChanges()
    private val store = ViewModelStore()

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, LevanaDatabase::class.java)
            .allowMainThreadQueries()
            // Run reads inline so a refresh completes within the looper idling in settle().
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        dao = CountingDao(db.personalEventDao())
        events = PersonalEventRepository(dao)
        preferences = PreferencesRepository(app, CityRepository(app))
        runBlocking { preferences.saveCalendarHebrewMode(false) }
    }

    @After
    fun tearDown() {
        store.clear()
        db.close()
    }

    private inline fun <reified T : ViewModel> create(crossinline make: () -> T): T =
        ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = make() as V
        })[T::class.java]

    private fun calendarViewModel(): CalendarViewModel {
        val app = RuntimeEnvironment.getApplication()
        val vm = create {
            CalendarViewModel(
                CalendarRepository(), preferences, events,
                ContactBirthdayRepository(app), SystemCalendarRepository(app),
                DeviceContext(preferences.preferences), changes
            )
        }
        awaitUntil { !vm.state.value.isLoading && vm.state.value.monthDays.isNotEmpty() }
        return vm
    }

    private fun dayDetailViewModel(date: LocalDate): DayDetailViewModel {
        val app = RuntimeEnvironment.getApplication()
        val vm = create {
            DayDetailViewModel(
                CalendarRepository(), ZmanimRepository(), preferences, events,
                ContactBirthdayRepository(app), SystemCalendarRepository(app), changes
            )
        }
        vm.onIntent(DayDetailIntent.LoadDay(date))
        awaitUntil { !vm.state.value.isLoading && vm.state.value.dayInfo?.gregorianDate == date }
        return vm
    }

    /** Browses away from today to [month] and selects [date], as a user would. */
    private fun CalendarViewModel.browseTo(month: YearMonth, date: LocalDate) {
        onIntent(CalendarIntent.LoadMonth(month))
        onIntent(CalendarIntent.SelectDay(date))
        awaitUntil {
            state.value.currentMonth == month && !state.value.isLoading &&
                state.value.gregorianMonthCache.size == 3
        }
    }

    private fun CalendarViewModel.marked(date: LocalDate) =
        state.value.monthDays.single { it.gregorianDate == date }.hasPersonalEvent

    private fun DayDetailViewModel.titles() =
        state.value.calendarEvents.filterIsInstance<CalendarEvent.CustomEvent>().map { it.title }

    private val browsedMonth = YearMonth.now().plusMonths(4)
    private val selected = browsedMonth.atDay(12)

    @Test
    fun `adding editing and deleting an event refreshes the browsed month and open day in place`() {
        val calendar = calendarViewModel()
        calendar.browseTo(browsedMonth, selected)
        val day = dayDetailViewModel(selected)
        assertFalse(calendar.marked(selected))
        assertEquals(emptyList<String>(), day.titles())

        val id = insert("Siyum", selected)
        changes.signal()
        settle()

        assertTrue(calendar.marked(selected))
        assertEquals(listOf("Siyum"), day.titles())
        assertEquals("browsed month is kept", browsedMonth, calendar.state.value.currentMonth)
        assertEquals("selection is kept", selected, calendar.state.value.selectedDate)
        assertFalse(calendar.state.value.isLoading)

        runBlocking { events.update(eventOn("Siyum HaShas", selected).copy(id = id)) }
        changes.signal()
        settle()
        assertEquals(listOf("Siyum HaShas"), day.titles())

        runBlocking { events.delete(eventOn("Siyum HaShas", selected).copy(id = id)) }
        changes.signal()
        settle()

        assertFalse(calendar.marked(selected))
        assertEquals(emptyList<String>(), day.titles())
        assertEquals(browsedMonth, calendar.state.value.currentMonth)
        assertEquals(selected, calendar.state.value.selectedDate)
    }

    @Test
    fun `neighbouring months in the cache are refreshed too`() {
        val calendar = calendarViewModel()
        calendar.browseTo(browsedMonth, selected)
        val nextMonthDate = browsedMonth.plusMonths(1).atDay(20)

        insert("Bar mitzvah", nextMonthDate)
        changes.signal()
        settle()

        awaitUntil {
            calendar.state.value.gregorianMonthCache[browsedMonth.plusMonths(1)]
                ?.any { it.gregorianDate == nextMonthDate && it.hasPersonalEvent } == true
        }
        val next = calendar.state.value.gregorianMonthCache.getValue(browsedMonth.plusMonths(1))
        assertTrue(next.single { it.gregorianDate == nextMonthDate }.hasPersonalEvent)
    }

    @Test
    fun `changes while paused are applied once on resume`() {
        val calendar = calendarViewModel()
        calendar.browseTo(browsedMonth, selected)
        val day = dayDetailViewModel(selected)
        calendar.onIntent(CalendarIntent.Pause)
        day.onIntent(DayDetailIntent.Pause)

        insert("Siyum", selected)
        val readsBefore = dao.reads
        repeat(3) { changes.signal(); settle() }

        assertEquals("no reload in the background", readsBefore, dao.reads)
        assertFalse(calendar.marked(selected))
        assertEquals(emptyList<String>(), day.titles())

        val rechecksBefore = changes.permissionRechecks
        calendar.onIntent(CalendarIntent.Resume)
        day.onIntent(DayDetailIntent.Resume)
        settle()

        assertTrue(calendar.marked(selected))
        assertEquals(listOf("Siyum"), day.titles())
        assertEquals("permissions are rechecked on resume", rechecksBefore + 2, changes.permissionRechecks)
        assertEquals(browsedMonth, calendar.state.value.currentMonth)
        assertEquals(selected, calendar.state.value.selectedDate)
    }

    @Test
    fun `resume without changes does not reload`() {
        val calendar = calendarViewModel()
        val day = dayDetailViewModel(selected)
        settle()
        val readsBefore = dao.reads

        calendar.onIntent(CalendarIntent.Pause)
        day.onIntent(DayDetailIntent.Pause)
        calendar.onIntent(CalendarIntent.Resume)
        day.onIntent(DayDetailIntent.Resume)
        settle()

        assertEquals(readsBefore, dao.reads)
    }

    @Test
    fun `a burst of changes reloads once and a reload triggers no further reloads`() {
        val calendar = calendarViewModel()
        calendar.browseTo(browsedMonth, selected)
        val day = dayDetailViewModel(selected)
        settle()

        var readsBefore = dao.reads
        changes.signal()
        settle()
        val perRefresh = dao.reads - readsBefore
        // Current month + two neighbours for the calendar, one read for the open day.
        assertEquals(4, perRefresh)

        readsBefore = dao.reads
        repeat(10) { changes.signal() }
        settle()
        assertEquals(perRefresh, dao.reads - readsBefore)

        readsBefore = dao.reads
        settle()
        settle()
        assertEquals("no reload loop", readsBefore, dao.reads)
        assertTrue(day.state.value.dayInfo != null)
    }

    @Test
    fun `hebrew mode refreshes the browsed hebrew month in place`() {
        runBlocking { preferences.saveCalendarHebrewMode(true) }
        try {
            val calendar = calendarViewModel()
            awaitUntil { calendar.state.value.calendarHebrewMode && !calendar.state.value.isLoading }
            val browsed = HebrewYearMonth.from(LocalDate.now()).next().next()
            calendar.onIntent(CalendarIntent.LoadHebrewMonth(browsed))
            awaitUntil {
                calendar.state.value.hebrewYearMonth == browsed && !calendar.state.value.isLoading &&
                    calendar.state.value.hebrewMonthCache.size == 3
            }
            val date = calendar.state.value.monthDays[9].gregorianDate
            calendar.onIntent(CalendarIntent.SelectDay(date))

            insert("Yahrzeit", date)
            changes.signal()
            settle()

            assertTrue(calendar.marked(date))
            assertEquals(browsed, calendar.state.value.hebrewYearMonth)
            assertEquals(date, calendar.state.value.selectedDate)
        } finally {
            runBlocking { preferences.saveCalendarHebrewMode(false) }
        }
    }

    @Test
    fun `selecting a calendar mode is explicit and idempotent`() {
        try {
            val calendar = calendarViewModel()
            val date = calendar.state.value.monthDays[9].gregorianDate
            calendar.onIntent(CalendarIntent.SelectDay(date))

            // Re-selecting the current mode neither switches nor reloads.
            val readsBefore = dao.reads
            calendar.onIntent(CalendarIntent.SetCalendarHebrewMode(false))
            settle()
            assertFalse(calendar.state.value.calendarHebrewMode)
            assertFalse(runBlocking { preferences.preferences.first() }.calendarHebrewMode)
            assertEquals(readsBefore, dao.reads)

            // Choosing the other mode switches once; choosing it again keeps it.
            calendar.onIntent(CalendarIntent.SetCalendarHebrewMode(true))
            awaitUntil { calendar.state.value.calendarHebrewMode && !calendar.state.value.isLoading }
            calendar.onIntent(CalendarIntent.SetCalendarHebrewMode(true))
            settle()
            assertTrue(calendar.state.value.calendarHebrewMode)
            assertTrue(runBlocking { preferences.preferences.first() }.calendarHebrewMode)
            assertEquals(date, calendar.state.value.selectedDate)
            assertEquals(HebrewYearMonth.from(date), calendar.state.value.hebrewYearMonth)

            calendar.onIntent(CalendarIntent.SetCalendarHebrewMode(false))
            awaitUntil { !calendar.state.value.calendarHebrewMode && !calendar.state.value.isLoading }
            assertFalse(runBlocking { preferences.preferences.first() }.calendarHebrewMode)
            assertEquals(date, calendar.state.value.selectedDate)
            assertEquals(YearMonth.from(date), calendar.state.value.currentMonth)
        } finally {
            runBlocking { preferences.saveCalendarHebrewMode(false) }
        }
    }

    @Test
    fun `clearing the view models stops observing changes`() {
        calendarViewModel()
        dayDetailViewModel(selected)
        assertEquals(2, changes.signals.subscriptionCount.value)

        store.clear()
        settle()

        assertEquals(0, changes.signals.subscriptionCount.value)
    }

    private fun insert(title: String, date: LocalDate): Long =
        runBlocking { events.insert(eventOn(title, date)) }

    private fun eventOn(title: String, date: LocalDate): PersonalEvent {
        val jd = JewishDate(GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth))
        return PersonalEvent(
            title = title,
            hebrewDay = jd.jewishDayOfMonth,
            hebrewMonth = jd.jewishMonth,
            hebrewYear = jd.jewishYear
        )
    }

    /** Lets the debounce elapse and every resulting load finish. */
    private fun settle() {
        repeat(5) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ChangeRefresher.DEBOUNCE_MILLIS))
            Thread.sleep(5)
        }
        shadowOf(Looper.getMainLooper()).idle()
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
