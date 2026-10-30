package ninja.doskey.app.levana.ui.agenda

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.AppDataStores
import ninja.doskey.app.levana.data.AgendaRepository
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventDataChanges
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.data.db.LevanaDatabase
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.device.fixedClock
import ninja.doskey.app.levana.domain.agenda.AgendaItem
import ninja.doskey.app.levana.domain.model.Location
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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
class AgendaViewModelTest {

    private object NoChanges : EventDataChanges {
        override val changes: Flow<Unit> = emptyFlow()
        override fun recheckPermissions() = Unit
    }

    private val zone = ZoneId.of("America/New_York")
    private val today = LocalDate.of(2026, 10, 30)
    private val chunk = AgendaViewModel.CHUNK_DAYS

    private lateinit var db: LevanaDatabase
    private lateinit var preferences: PreferencesRepository
    private val store = ViewModelStore()

    private lateinit var savedZone: TimeZone

    @Before
    fun setUp() {
        // A stored position is read in the device's zone
        savedZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
        val app = RuntimeEnvironment.getApplication()
        AppDataStores.clearPreferences(app)
        db = Room.inMemoryDatabaseBuilder(app, LevanaDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        preferences = PreferencesRepository(app, CityRepository(app))
        runBlocking {
            preferences.saveCalendarHebrewMode(true)
            // The device is in New York; Jerusalem becomes the selected city in one test
            preferences.updateCurrentLocation(Location(40.7128, -74.006, 10.0, "America/New_York", "New York", "United States"))
            db.personalEventDao().insert(PersonalEvent(title = "Moshe ben Avraham", hebrewDay = 23, hebrewMonth = 8, hebrewYear = 5770, useYahrzeitRules = true))
        }
    }

    @After
    fun tearDown() {
        store.clear()
        db.close()
        AppDataStores.clearPreferences(RuntimeEnvironment.getApplication())
        TimeZone.setDefault(savedZone)
    }

    private fun viewModel(): AgendaViewModel {
        val app = RuntimeEnvironment.getApplication()
        val device = DeviceContext(preferences.preferences, emptyFlow()) { fixedClock(today, LocalTime.NOON, zone) }
        val repository = AgendaRepository(
            CalendarRepository(), ZmanimRepository(), PersonalEventRepository(db.personalEventDao()),
            ContactBirthdayRepository(app), SystemCalendarRepository(app)
        )
        val vm = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V =
                AgendaViewModel(repository, preferences, device, NoChanges) as V
        })[AgendaViewModel::class.java]
        awaitUntil { !vm.state.value.isLoading }
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
    fun `opens on the device's today with a window around it`() {
        val state = viewModel().state.value
        assertEquals(today, state.today)
        assertEquals(today.minusDays(chunk), state.start)
        assertEquals(today.plusDays(2 * chunk - 1), state.end)
        assertEquals(today, state.scrollRequest?.date)
        assertTrue(state.days.all { it.date in state.start!!..state.end!! })
        assertTrue(state.days.zipWithNext().all { (a, b) -> a.date < b.date })
        assertTrue(state.hebrewLeading)
    }

    @Test
    fun `the window grows a chunk at a time at either end`() {
        val vm = viewModel()
        val first = vm.state.value
        vm.onIntent(AgendaIntent.LoadLater)
        awaitUntil { vm.state.value.end == first.end!!.plusDays(chunk) && !vm.state.value.loadingLater }
        vm.onIntent(AgendaIntent.LoadEarlier)
        awaitUntil { vm.state.value.start == first.start!!.minusDays(chunk) && !vm.state.value.loadingEarlier }
        val days = vm.state.value.days
        assertTrue(days.zipWithNext().all { (a, b) -> a.date < b.date })
        assertTrue(days.first().date < first.days.first().date)
        assertTrue(days.last().date > first.days.last().date)
    }

    @Test
    fun `a date inside the window only scrolls, one outside reloads around it`() {
        val vm = viewModel()
        vm.onIntent(AgendaIntent.ScrollHandled(vm.state.value.scrollRequest!!.id))
        assertNull(vm.state.value.scrollRequest)

        val inside = today.plusDays(10)
        vm.onIntent(AgendaIntent.GoToDate(inside))
        assertEquals(inside, vm.state.value.scrollRequest?.date)
        assertEquals(today.minusDays(chunk), vm.state.value.start)

        val far = LocalDate.of(2028, 3, 1)
        vm.onIntent(AgendaIntent.GoToDate(far))
        awaitUntil { vm.state.value.start == far.minusDays(chunk) && !vm.state.value.isLoading }
        assertEquals(far, vm.state.value.scrollRequest?.date)

        vm.onIntent(AgendaIntent.GoToToday)
        awaitUntil { vm.state.value.start == today.minusDays(chunk) && !vm.state.value.isLoading }
        assertEquals(today, vm.state.value.scrollRequest?.date)
    }

    @Test
    fun `search finds entries in the year around today and jumps to them`() {
        val vm = viewModel()
        vm.onIntent(AgendaIntent.OpenSearch)
        vm.onIntent(AgendaIntent.SetSearchQuery("moshe"))
        awaitUntil { vm.state.value.search?.isLoading == false }
        // 23 Cheshvan: in 5786 (2025) and 5787 (2026); 5788's falls after today + 12 months
        val results = vm.state.value.search!!.results
        assertEquals(listOf(LocalDate.of(2025, 11, 14), LocalDate.of(2026, 11, 3)), results.map { it.date })

        vm.onIntent(AgendaIntent.SetSearchQuery("nothing like this"))
        assertTrue(vm.state.value.search!!.results.isEmpty())

        vm.onIntent(AgendaIntent.SelectSearchResult(LocalDate.of(2025, 11, 14)))
        assertNull(vm.state.value.search)
        awaitUntil { vm.state.value.scrollRequest?.date == LocalDate.of(2025, 11, 14) && !vm.state.value.isLoading }
    }

    @Test
    fun `the leading calendar is the Month screen's setting`() {
        val vm = viewModel()
        vm.onIntent(AgendaIntent.SetHebrewLeading(false))
        awaitUntil { !vm.state.value.hebrewLeading }
        assertEquals(false, runBlocking { preferences.preferences.first().calendarHebrewMode })
    }

    @Test
    fun `selecting another city reloads its times and labels its clock`() {
        val vm = viewModel()
        fun candles() = vm.state.value.days.first { it.date == today }.items.filterIsInstance<AgendaItem.CandleLighting>().single().time
        val before = candles()
        assertNull(vm.state.value.otherClockCityName)
        runBlocking {
            preferences.addSavedLocation(Location.JERUSALEM)
            preferences.setActiveLocation(preferences.preferences.first().savedLocations.single().id)
        }
        awaitUntil { vm.state.value.otherClockCityName == Location.JERUSALEM.name && candles() != before }
        assertNotEquals(before, candles())
    }
}
