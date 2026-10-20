package ninja.doskey.app.levana.verifyio

import android.Manifest
import android.provider.CalendarContract
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.ui.calendarselection.CalendarSelectionIntent
import ninja.doskey.app.levana.ui.calendarselection.CalendarSelectionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Seeds persisted selection explicitly so other tests cannot change the toggle's starting state. */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class VerifyCalendarSelectionToggleIsolatedTest {

    private val world = CalendarWorld()
    private lateinit var provider: ScriptedProvider
    private val queryNo = AtomicInteger(0)
    private val holds = ConcurrentHashMap<Int, CountDownLatch>()
    private val failures = ConcurrentHashMap<Int, () -> RuntimeException>()

    @Before
    fun setUp() {
        grantAll()
        provider = installCalendar(world)
        provider.responder = { uri, proj, sel, args ->
            val n = queryNo.incrementAndGet()
            val snapshot = world.respond(uri, proj, sel, args)
            holds[n]?.await(5, TimeUnit.SECONDS)
            failures[n]?.let { throw it() }
            snapshot
        }
    }

    private fun prefs() = PreferencesRepository(app(), CityRepository(app()))
    private fun vm(prefs: PreferencesRepository = prefs()) =
        CalendarSelectionViewModel(SystemCalendarRepository(app()), prefs)

    private fun names(vm: CalendarSelectionViewModel) = vm.state.value.calendars.map { it.name }.toSet()

    private fun settle() = repeat(3) { Thread.sleep(40); idleUntil { true } }

    private fun awaitQueries(n: Int) = idleUntil(what = "query $n") { queryNo.get() >= n }

    @Test
    fun toggleDuringAReloadIsNotLostWhenTheReloadPublishes() {
        world.calendars += CalendarWorld.Cal(1, "Home", "a", 1)
        world.calendars += CalendarWorld.Cal(2, "Work", "a", 2)
        val prefs = prefs()
        runBlocking { prefs.saveSelectedCalendarIds(emptySet()) }
        val vm = vm(prefs)
        idleUntil(what = "first load") { !vm.state.value.isLoading }
        holds[2] = CountDownLatch(1)
        vm.onIntent(CalendarSelectionIntent.PermissionGranted)
        awaitQueries(2)
        vm.onIntent(CalendarSelectionIntent.ToggleCalendar(2))
        idleUntil(what = "saved") { runBlocking { prefs.preferences.first().selectedCalendarIds } == setOf(2L) }
        holds[2]!!.countDown()
        settle()
        assertEquals(setOf(2L), vm.state.value.selectedIds)
    }
}
