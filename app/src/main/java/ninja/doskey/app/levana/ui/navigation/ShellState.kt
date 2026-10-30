package ninja.doskey.app.levana.ui.navigation

import android.content.Context
import android.os.Bundle
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator

/*
 * The app shell's saved state, each under its own key (TASK-50).
 *
 * rememberSaveable keys a value by its position, and value-returning composables such as
 * rememberNavController and rememberDrawerState add no group of their own, so two of them in
 * one function share a key and are told apart only by the order they register. The shell
 * returns early while the preferences load, so after a recreation the nav controller
 * registered alone; a second recreation in that window saved the drawer's value as the nav
 * controller's, and restoring it crashed (DrawerValue cannot be cast to Bundle).
 */

private const val NAV_CONTROLLER_KEY = "levana.shell.navController"
private const val DRAWER_KEY = "levana.shell.drawer"

/**
 * [androidx.navigation.compose.rememberNavController] under its own key. Until the NavHost sets
 * the graph, the controller has nothing to save; it then saves the state it was restored with,
 * so a recreation in that window keeps the back stack.
 */
@Composable
internal fun rememberLevanaNavController(): NavHostController {
    val context = LocalContext.current
    return rememberSaveable(key = NAV_CONTROLLER_KEY, saver = restoredNavSaver(context)) {
        RestoredNav(createNavController(context), restored = null)
    }.controller
}

/** The drawer's open state under its own key. */
@Composable
internal fun rememberLevanaDrawerState(): DrawerState =
    rememberSaveable(key = DRAWER_KEY, saver = DrawerState.Saver(confirmStateChange = { true })) {
        DrawerState(DrawerValue.Closed)
    }

private class RestoredNav(val controller: NavHostController, val restored: Bundle?)

private fun restoredNavSaver(context: Context) = Saver<RestoredNav, Bundle>(
    save = { it.controller.saveState() ?: it.restored },
    restore = { RestoredNav(createNavController(context).apply { restoreState(it) }, restored = it) },
)

// The navigators rememberNavController adds
private fun createNavController(context: Context) = NavHostController(context).apply {
    navigatorProvider.addNavigator(ComposeNavigator())
    navigatorProvider.addNavigator(DialogNavigator())
}
