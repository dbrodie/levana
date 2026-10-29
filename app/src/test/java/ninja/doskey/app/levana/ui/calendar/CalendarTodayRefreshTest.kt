package ninja.doskey.app.levana.ui.calendar

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventDataChanges
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.data.db.LevanaDatabase
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.device.fixedClock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** TASK-5 case 13: the calendar's today highlight follows the device date while alive. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CalendarTodayRefreshTest {

    private object NoChanges : EventDataChanges {
        override val changes: Flow<Unit> = emptyFlow()
        override fun recheckPermissions() = Unit
    }

    private val nyZone = ZoneId.of("America/New_York")
    private val day = LocalDate.of(2026, 10, 29)
    private var clock: Clock = fixedClock(day, LocalTime.of(23, 59), nyZone)
    private val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    private lateinit var db: LevanaDatabase
    private lateinit var preferences: PreferencesRepository
    private val store = ViewModelStore()

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, LevanaDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        preferences = PreferencesRepository(app, CityRepository(app))
        runBlocking {
            preferences.saveCalendarHebrewMode(false)
            preferences.saveDevDayShift(0)
        }
    }

    @After
    fun tearDown() {
        store.clear()
        db.close()
        runBlocking { preferences.saveDevDayShift(0) }
    }

    private fun calendarViewModel(): CalendarViewModel {
        val app = RuntimeEnvironment.getApplication()
        val device = DeviceContext(preferences.preferences, signals) { clock }
        val vm = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = CalendarViewModel(
                CalendarRepository(), preferences, PersonalEventRepository(db.personalEventDao()),
                ContactBirthdayRepository(app), SystemCalendarRepository(app), device, NoChanges
            ) as V
        })[CalendarViewModel::class.java]
        awaitUntil { vm.state.value.today != null && !vm.state.value.isLoading }
        return vm
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

    @Test
    fun `opens on the device date with it selected`() {
        val vm = calendarViewModel()
        assertEquals(day, vm.state.value.today)
        assertEquals(day, vm.state.value.selectedDate)
        assertEquals(java.time.YearMonth.from(day), vm.state.value.currentMonth)
    }

    @Test
    fun `the highlight moves at midnight without a restart`() {
        val vm = calendarViewModel()
        clock = fixedClock(day.plusDays(1), LocalTime.of(0, 0, 5), nyZone)
        signals.tryEmit(Unit)
        awaitUntil { vm.state.value.today == day.plusDays(1) }
        // Only the highlight moves; the selection stays where the user left it
        assertEquals(day, vm.state.value.selectedDate)
    }

    @Test
    fun `resume re-reads the device date after a frozen midnight`() {
        val vm = calendarViewModel()
        clock = fixedClock(day.plusDays(1), LocalTime.of(8, 0), nyZone)
        vm.onIntent(CalendarIntent.Pause)
        vm.onIntent(CalendarIntent.Resume)
        assertEquals(day.plusDays(1), vm.state.value.today)
    }

    @Test
    fun `a time-zone change moves the highlight`() {
        val vm = calendarViewModel()
        clock = clock.withZone(ZoneId.of("Asia/Jerusalem"))
        signals.tryEmit(Unit)
        awaitUntil { vm.state.value.today == day.plusDays(1) }
    }

    @Test
    fun `the developer override moves the highlight and turning it off restores it`() {
        val vm = calendarViewModel()
        runBlocking { preferences.saveDevDayShift(3) }
        awaitUntil { vm.state.value.today == day.plusDays(3) }
        runBlocking { preferences.saveDevDayShift(0) }
        awaitUntil { vm.state.value.today == day }
    }
}
