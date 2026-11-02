package ninja.doskey.app.levana.ui.panes

import android.app.Application
import android.os.Looper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import java.time.Duration
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ninja.doskey.app.levana.AppDataStores
import ninja.doskey.app.levana.data.PaneLayoutRepository
import ninja.doskey.app.levana.domain.model.PaneSplit
import ninja.doskey.app.levana.domain.model.PaneStop
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

/** The two-pane divider's stops, hiding and storage (TASK-52.6). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PaneSplitViewModelTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private lateinit var repository: PaneLayoutRepository
    private val store = ViewModelStore()
    private var count = 0

    @Before
    fun setUp() {
        clearStore()
        repository = PaneLayoutRepository(app)
    }

    @After
    fun tearDown() {
        store.clear()
        clearStore()
    }

    /**
     * DataStore runs a write's transform in the writer's context, here the main looper, so a
     * view model's write still queued would block a clear that holds the main thread.
     */
    private fun clearStore() {
        val clearing = thread { AppDataStores.clearPaneLayout(app) }
        while (clearing.isAlive) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))
            clearing.join(10)
        }
    }

    /** A new view model each call, as Month and Agenda each have their own. */
    private fun viewModel(): PaneSplitViewModel {
        val vm = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = PaneSplitViewModel(repository) as V
        })["screen${count++}", PaneSplitViewModel::class.java]
        awaitUntil { vm.state.value.loaded }
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

    private fun stored() = runBlocking { withTimeout(5_000) { repository.split.first() } }

    @Test
    fun `starts at half and a double tap cycles the stops`() {
        val vm = viewModel()
        assertEquals(PaneSplit(PaneStop.LIST_50), vm.state.value.split)
        val seen = (1..3).map { vm.onIntent(PaneSplitIntent.Cycle); vm.state.value.split.stop }
        assertEquals(listOf(PaneStop.LIST_65, PaneStop.LIST_40, PaneStop.LIST_50), seen)
    }

    @Test
    fun `hiding keeps the stop, and showing or a double tap restores it`() {
        val vm = viewModel()
        vm.onIntent(PaneSplitIntent.SetStop(PaneStop.LIST_40))
        vm.onIntent(PaneSplitIntent.Collapse)
        assertEquals(PaneSplit(PaneStop.LIST_40, collapsed = true), vm.state.value.split)
        vm.onIntent(PaneSplitIntent.Cycle)
        assertEquals(PaneSplit(PaneStop.LIST_40), vm.state.value.split)
        vm.onIntent(PaneSplitIntent.Collapse)
        vm.onIntent(PaneSplitIntent.Expand)
        assertEquals(PaneSplit(PaneStop.LIST_40), vm.state.value.split)
        // A drag released at a stop shows a hidden panel there
        vm.onIntent(PaneSplitIntent.Collapse)
        vm.onIntent(PaneSplitIntent.SetStop(PaneStop.LIST_65))
        assertEquals(PaneSplit(PaneStop.LIST_65), vm.state.value.split)
    }

    @Test
    fun `the position is stored, so Month and Agenda share it and it survives the process`() {
        val month = viewModel()
        month.onIntent(PaneSplitIntent.SetStop(PaneStop.LIST_65))
        month.onIntent(PaneSplitIntent.Collapse)
        awaitUntil { stored() == PaneSplit(PaneStop.LIST_65, collapsed = true) }
        val agenda = viewModel()
        assertEquals(PaneSplit(PaneStop.LIST_65, collapsed = true), agenda.state.value.split)
    }

    @Test
    fun `stops keep the list at phone width and the panel at its minimum`() {
        // Ultra portrait, 859dp: 40% would be 344dp and 65% would leave the panel 285dp
        assertEquals(MinListWidth, listWidth(PaneStop.LIST_40, 859.dp))
        assertEquals(429.5.dp, listWidth(PaneStop.LIST_50, 859.dp))
        assertEquals(859.dp - PaneGutter - MinPanelWidth, listWidth(PaneStop.LIST_65, 859.dp))
        // Fold8 landscape, 933dp: 40% fits
        assertEquals(933.dp * 0.4f, listWidth(PaneStop.LIST_40, 933.dp))
    }
}
