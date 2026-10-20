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

/**
 * Each calendar query snapshots the calendar list when it starts and can be
 * held individually, so an earlier (stale) answer can be released after a
 * newer request has published.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class VerifyCalendarSelectionLatestLoadTest {

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
    fun aSlowEarlierLoadCannotReplaceANewerCalendarList() {
        world.calendars += CalendarWorld.Cal(1, "Old", "a", 1)
        holds[1] = CountDownLatch(1)
        val vm = vm()
        awaitQueries(1)
        world.calendars.clear()
        world.calendars += CalendarWorld.Cal(2, "New", "a", 2)
        vm.onIntent(CalendarSelectionIntent.PermissionGranted)
        idleUntil(what = "newer list") { names(vm) == setOf("New") }
        holds[1]!!.countDown()
        settle()
        assertEquals(setOf("New"), names(vm))
        assertTrue(vm.state.value.hasPermission)
    }

    @Test
    fun repeatedLoadsPublishOnlyTheLastAnswerWhicheverOrderTheyFinish() {
        world.calendars += CalendarWorld.Cal(1, "A", "a", 1)
        holds[1] = CountDownLatch(1); holds[2] = CountDownLatch(1); holds[3] = CountDownLatch(1)
        val vm = vm()
        awaitQueries(1)
        world.calendars += CalendarWorld.Cal(2, "B", "a", 2)
        vm.onIntent(CalendarSelectionIntent.Load)
        awaitQueries(2)
        world.calendars += CalendarWorld.Cal(3, "C", "a", 3)
        vm.onIntent(CalendarSelectionIntent.PermissionGranted)
        awaitQueries(3)
        holds[3]!!.countDown()
        idleUntil(what = "latest") { names(vm) == setOf("A", "B", "C") }
        holds[2]!!.countDown(); settle()
        holds[1]!!.countDown(); settle()
        assertEquals(setOf("A", "B", "C"), names(vm))
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun anEarlierAnswerAlreadyBackFromTheProviderButNotYetAppliedIsDropped() {
        world.calendars += CalendarWorld.Cal(1, "Old", "a", 1)
        holds[1] = CountDownLatch(1)
        holds[2] = CountDownLatch(1)
        val vm = vm()
        awaitQueries(1)
        world.calendars.clear()
        world.calendars += CalendarWorld.Cal(2, "New", "a", 2)
        holds[1]!!.countDown()
        Thread.sleep(150) // provider returned; main has not run the resumption
        vm.onIntent(CalendarSelectionIntent.Load)
        awaitQueries(2)
        settle()
        assertTrue("stale list published: ${names(vm)}", vm.state.value.calendars.isEmpty())
        assertTrue(vm.state.value.isLoading)
        holds[2]!!.countDown()
        idleUntil(what = "new") { names(vm) == setOf("New") }
    }

    @Test
    fun revocationAfterAPendingLoadIsNotOverwrittenByThatLoad() {
        world.calendars += CalendarWorld.Cal(1, "Home", "a", 1)
        holds[1] = CountDownLatch(1)
        val vm = vm()
        awaitQueries(1)
        shadowOf(app()).denyPermissions(Manifest.permission.READ_CALENDAR)
        vm.onIntent(CalendarSelectionIntent.Load)
        idleUntil(what = "no permission") { !vm.state.value.isLoading }
        assertFalse(vm.state.value.hasPermission)
        holds[1]!!.countDown()
        settle()
        assertFalse("stale load restored permission", vm.state.value.hasPermission)
        assertTrue(vm.state.value.calendars.isEmpty())
    }

    @Test
    fun aStaleRevokedErrorCannotHideANewerGrantedList() {
        world.calendars += CalendarWorld.Cal(1, "Home", "a", 1)
        holds[1] = CountDownLatch(1)
        failures[1] = { SecurityException("revoked at the time") }
        val vm = vm()
        awaitQueries(1)
        vm.onIntent(CalendarSelectionIntent.PermissionGranted)
        idleUntil(what = "granted list") { names(vm) == setOf("Home") }
        holds[1]!!.countDown()
        settle()
        assertTrue("stale SecurityException published no-permission", vm.state.value.hasPermission)
        assertEquals(setOf("Home"), names(vm))
    }

    @Test
    fun toggleDuringAReloadIsNotLostWhenTheReloadPublishes() {
        world.calendars += CalendarWorld.Cal(1, "Home", "a", 1)
        world.calendars += CalendarWorld.Cal(2, "Work", "a", 2)
        // Seed an explicit starting selection: other tests in this JVM share the
        // persisted preferences (a full run leaves {7} selected here)
        val prefs = prefs()
        runBlocking { prefs.saveSelectedCalendarIds(setOf(1L)) }
        val vm = vm(prefs)
        idleUntil(what = "first load") { !vm.state.value.isLoading }
        assertEquals(setOf(1L), vm.state.value.selectedIds)
        holds[2] = CountDownLatch(1)
        vm.onIntent(CalendarSelectionIntent.PermissionGranted)
        awaitQueries(2)
        vm.onIntent(CalendarSelectionIntent.ToggleCalendar(2))
        idleUntil(what = "saved") { runBlocking { prefs.preferences.first().selectedCalendarIds } == setOf(1L, 2L) }
        holds[2]!!.countDown()
        settle()
        assertEquals(setOf(1L, 2L), vm.state.value.selectedIds)
    }
}
