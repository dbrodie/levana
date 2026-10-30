package ninja.doskey.app.levana.ui.navigation

import android.app.Application
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The shell's nav controller and drawer state across recreations (TASK-50). The content has
 * LevanaApp's shape: the nav controller first, then an early return while the shell waits for
 * the preferences, then the drawer and the NavHost. With rememberNavController() and
 * rememberDrawerState() called directly in [Shell], as LevanaApp did, the first test fails with
 * "DrawerValue cannot be cast to Bundle".
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ShellStateRestorationTest {

    @get:Rule
    val compose = createComposeRule()

    private val restoration = StateRestorationTester(compose)

    /** Read by the composition but not observed, like a value that has not loaded yet. */
    private var ready = true
    private var recompose by mutableIntStateOf(0)

    private lateinit var navController: NavHostController
    private var drawerState: DrawerState? = null
    private lateinit var scope: CoroutineScope

    private fun setShell() = restoration.setContent { Shell() }

    @Composable
    private fun Shell() {
        @Suppress("UNUSED_EXPRESSION") recompose
        navController = rememberLevanaNavController()
        scope = rememberCoroutineScope()
        if (!ready) return
        val drawer = rememberLevanaDrawerState()
        drawerState = drawer
        ModalNavigationDrawer(drawerState = drawer, drawerContent = { ModalDrawerSheet { Text("Drawer") } }) {
            NavHost(navController, startDestination = "month") {
                composable("month") { Text("Month") }
                composable("settings") { Text("Settings") }
            }
        }
    }

    private fun openSettingsAndDrawer() {
        compose.runOnIdle { navController.navigate("settings") }
        compose.runOnIdle { scope.launch { checkNotNull(drawerState).open() } }
        compose.waitForIdle()
        assertShell()
    }

    private fun assertShell() = compose.runOnIdle {
        assertEquals("settings", navController.currentBackStackEntry?.destination?.route)
        assertEquals(listOf("month", "settings"), navController.currentBackStack.value.mapNotNull { it.destination.route })
        assertEquals(DrawerValue.Open, checkNotNull(drawerState).currentValue)
    }

    @Test
    fun `two recreations before the shell shows keep the back stack and the drawer`() {
        setShell()
        openSettingsAndDrawer()

        // Recreated while the preferences load, then recreated again before they arrive
        ready = false
        restoration.emulateSavedInstanceStateRestore()
        restoration.emulateSavedInstanceStateRestore()

        ready = true
        recompose++
        assertShell()
    }

    @Test
    fun `a recreation after the shell shows keeps the back stack and the drawer`() {
        setShell()
        openSettingsAndDrawer()

        restoration.emulateSavedInstanceStateRestore()
        restoration.emulateSavedInstanceStateRestore()

        assertShell()
    }
}
