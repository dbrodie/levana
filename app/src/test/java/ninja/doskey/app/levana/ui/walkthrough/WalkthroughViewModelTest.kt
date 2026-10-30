package ninja.doskey.app.levana.ui.walkthrough

import android.app.Application
import android.os.Looper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.AppDataStores
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.LocationService
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.domain.model.Location
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
import java.time.Duration

/** The walkthrough's launch decision, seen state, Skip, replay and permissions (TASK-43). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WalkthroughViewModelTest {

    private lateinit var app: Application
    private lateinit var preferences: PreferencesRepository
    private val granted = mutableSetOf<WalkthroughPermission>()
    private val allIds = WalkthroughRegistry.screens.map { it.id }

    private val device = object : WalkthroughDevice {
        override fun isGranted(permission: WalkthroughPermission) = permission in granted
        override fun isAvailable(screen: FeatureScreen) = true
    }

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        AppDataStores.clearPreferences(app)
        preferences = PreferencesRepository(app, CityRepository(app))
    }

    @After
    fun tearDown() = AppDataStores.clearPreferences(app)

    private fun viewModel(showOnLaunch: Boolean = true, registry: List<FeatureScreen> = WalkthroughRegistry.screens) =
        WalkthroughViewModel(preferences, LocationService(app), WalkthroughGate(showOnLaunch), device, registry)

    private fun WalkthroughViewModel.started(deepLink: Boolean = false): WalkthroughState {
        onIntent(WalkthroughIntent.Start(deepLink))
        return awaitState { it.decided }
    }

    private fun WalkthroughViewModel.awaitState(condition: (WalkthroughState) -> Boolean): WalkthroughState {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition(state.value)) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))
            assertTrue("timed out", System.currentTimeMillis() < deadline)
            Thread.sleep(5)
        }
        return state.value
    }

    /** The stored seen IDs once [expected] has been written (writes run on DataStore's thread). */
    private fun awaitSeen(expected: Set<String>): Set<String> {
        val deadline = System.currentTimeMillis() + 10_000
        var seen = runBlocking { preferences.walkthroughSeenIds.first() }
        while (seen != expected && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))
            Thread.sleep(5)
            seen = runBlocking { preferences.walkthroughSeenIds.first() }
        }
        return seen
    }

    private fun seen() = runBlocking { preferences.walkthroughSeenIds.first() }

    @Test
    fun aFreshInstallShowsEveryScreenAndRecordsTheFirstAsSeen() {
        val state = viewModel().started()
        assertEquals(allIds, state.screens.map { it.id })
        assertEquals(0, state.page)
        assertTrue(state.shownOnLaunch)
        assertEquals(PageIndicator.DOTS, state.indicator)
        assertEquals(setOf(allIds[0]), awaitSeen(setOf(allIds[0])))
    }

    /** Owner decision 1: settings and cities from before the update don't pre-mark anything. */
    @Test
    fun anExistingUserSeesEveryScreenTheFirstTime() {
        runBlocking {
            preferences.addSavedLocation(Location.JERUSALEM)
            preferences.saveCandleLightingOffset(40.0)
        }
        assertEquals(allIds, viewModel().started().screens.map { it.id })
    }

    @Test
    fun anUpgradeWithOneNewScreenShowsOnlyItAsASingleGotItScreen() {
        runBlocking { preferences.markWalkthroughSeen(allIds) }
        val agenda = FeatureScreen("agenda", 0, 0) { }
        val state = viewModel(registry = WalkthroughRegistry.screens + agenda).started()
        assertEquals(listOf("agenda"), state.screens.map { it.id })
        assertEquals(PageIndicator.NONE, state.indicator)
        assertTrue(state.isLastPage)
    }

    @Test
    fun nothingLeftToSeeShowsNothing() {
        runBlocking { preferences.markWalkthroughSeen(allIds) }
        val state = viewModel().started()
        assertFalse(state.showing)
        assertFalse(state.shownOnLaunch)
    }

    @Test
    fun eachScreenIsSeenOnceDisplayed() {
        val vm = viewModel()
        vm.started()
        vm.onIntent(WalkthroughIntent.Next)
        vm.onIntent(WalkthroughIntent.Next)
        assertEquals(2, vm.state.value.page)
        assertEquals(allIds.take(3).toSet(), awaitSeen(allIds.take(3).toSet()))
        vm.onIntent(WalkthroughIntent.Back)
        assertEquals(1, vm.state.value.page)
        // A swipe settles on a page the same way
        vm.onIntent(WalkthroughIntent.PageShown(5))
        assertEquals(5, vm.state.value.page)
        assertEquals(allIds.take(3).toSet() + allIds[5], awaitSeen(allIds.take(3).toSet() + allIds[5]))
    }

    @Test
    fun skipMarksTheWholeRunSeenAndClosesIt() {
        val vm = viewModel()
        vm.started()
        vm.onIntent(WalkthroughIntent.Skip)
        assertFalse(vm.state.value.showing)
        assertEquals(allIds.toSet(), awaitSeen(allIds.toSet()))
        // The next launch has nothing to show
        assertFalse(viewModel().started().showing)
    }

    @Test
    fun anInterruptedRunShowsTheRestNextTime() {
        val vm = viewModel()
        vm.started()
        vm.onIntent(WalkthroughIntent.Next)
        awaitSeen(allIds.take(2).toSet())
        assertEquals(allIds.drop(2), viewModel().started().screens.map { it.id })
    }

    @Test
    fun nextOnTheLastPageFinishes() {
        val vm = viewModel()
        vm.started()
        vm.onIntent(WalkthroughIntent.PageShown(allIds.lastIndex))
        assertTrue(vm.state.value.isLastPage)
        vm.onIntent(WalkthroughIntent.Next)
        assertFalse(vm.state.value.showing)
        assertEquals(allIds.toSet(), awaitSeen(allIds.toSet()))
    }

    @Test
    fun aDeepLinkDefersTheWalkthroughToTheNextNormalLaunch() {
        val state = viewModel().started(deepLink = true)
        assertTrue(state.decided)
        assertFalse(state.showing)
        assertTrue(seen().isEmpty())
        assertEquals(allIds, viewModel().started().screens.map { it.id })
    }

    @Test
    fun theLaunchIsDecidedOncePerActivity() {
        val vm = viewModel()
        vm.started()
        vm.onIntent(WalkthroughIntent.Next)
        vm.onIntent(WalkthroughIntent.Start(deepLink = false))
        assertEquals(1, vm.state.value.page)
    }

    @Test
    fun withoutTheGateNothingOpensButReplayStillWorks() {
        val vm = viewModel(showOnLaunch = false)
        assertTrue(vm.state.value.decided)
        vm.onIntent(WalkthroughIntent.Start(deepLink = false))
        assertFalse(vm.state.value.showing)
        vm.onIntent(WalkthroughIntent.Replay)
        assertEquals(allIds, vm.state.value.screens.map { it.id })
    }

    @Test
    fun replayShowsEverythingAndLeavesTheSeenStateAlone() {
        runBlocking { preferences.markWalkthroughSeen(allIds.take(2)) }
        val vm = viewModel(showOnLaunch = false)
        vm.onIntent(WalkthroughIntent.Replay)
        val state = vm.state.value
        assertTrue(state.replay)
        assertEquals(allIds, state.screens.map { it.id })
        vm.onIntent(WalkthroughIntent.Next)
        vm.onIntent(WalkthroughIntent.Next)
        vm.onIntent(WalkthroughIntent.Skip)
        assertFalse(vm.state.value.showing)
        assertFalse(vm.state.value.replay)
        // Give any (wrong) write time to land
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        Thread.sleep(200)
        assertEquals(allIds.take(2).toSet(), seen())
    }

    @Test
    fun theDeveloperResetShowsEverythingAgain() {
        runBlocking { preferences.markWalkthroughSeen(allIds) }
        runBlocking { preferences.resetWalkthroughSeen() }
        assertEquals(allIds, viewModel().started().screens.map { it.id })
    }

    @Test
    fun permissionsShowGrantedStateAndARefusalNeverBlocks() {
        granted += WalkthroughPermission.CALENDAR
        val vm = viewModel()
        val state = vm.started()
        assertEquals(PermissionStatus.GRANTED, state.status(WalkthroughPermission.CALENDAR))
        assertEquals(PermissionStatus.NOT_GRANTED, state.status(WalkthroughPermission.NOTIFICATIONS))

        vm.onIntent(WalkthroughIntent.PermissionResult(WalkthroughPermission.NOTIFICATIONS, granted = false))
        assertEquals(PermissionStatus.DENIED, vm.state.value.status(WalkthroughPermission.NOTIFICATIONS))
        // A recheck keeps the refusal shown until the permission is granted, e.g. in system settings
        vm.onIntent(WalkthroughIntent.RefreshPermissions)
        assertEquals(PermissionStatus.DENIED, vm.state.value.status(WalkthroughPermission.NOTIFICATIONS))
        granted += WalkthroughPermission.NOTIFICATIONS
        vm.onIntent(WalkthroughIntent.RefreshPermissions)
        assertEquals(PermissionStatus.GRANTED, vm.state.value.status(WalkthroughPermission.NOTIFICATIONS))

        vm.onIntent(WalkthroughIntent.PermissionResult(WalkthroughPermission.CONTACTS, granted = true))
        assertEquals(PermissionStatus.GRANTED, vm.state.value.status(WalkthroughPermission.CONTACTS))
    }

    @Test
    fun notNowMovesOnAndFinishesOnTheLastPage() {
        val vm = viewModel()
        vm.started()
        vm.onIntent(WalkthroughIntent.PageShown(1))
        vm.onIntent(WalkthroughIntent.NotNow)
        assertEquals(2, vm.state.value.page)
        vm.onIntent(WalkthroughIntent.PageShown(allIds.lastIndex))
        vm.onIntent(WalkthroughIntent.NotNow)
        assertFalse(vm.state.value.showing)
    }

    @Test
    fun addCityOpensAndClosesThePicker() {
        val vm = viewModel()
        vm.started()
        vm.onIntent(WalkthroughIntent.AddCity)
        assertTrue(vm.state.value.addingCity)
        vm.onIntent(WalkthroughIntent.CloseCityPicker)
        assertFalse(vm.state.value.addingCity)
    }
}
