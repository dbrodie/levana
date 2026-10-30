package ninja.doskey.app.levana.screenshots

import android.Manifest
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.LocationService
import ninja.doskey.app.levana.ui.walkthrough.FeatureScreen
import ninja.doskey.app.levana.ui.walkthrough.WalkthroughGate
import ninja.doskey.app.levana.ui.walkthrough.WalkthroughRegistry
import ninja.doskey.app.levana.ui.walkthrough.WalkthroughViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The feature walkthrough (TASK-43) as it opens in [ninja.doskey.app.levana.MainActivity]:
 * each screen with its permission asks, the granted states, and runs of 1, 3 and 12 screens
 * for the page indicator.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = ScreenshotTest.DEVICE)
class WalkthroughScreenshotTest(variant: Variant) : ScreenshotTest(variant) {

    private val screens = WalkthroughRegistry.screens
    private val ids = screens.map { it.id }

    // The asks show until a test grants them
    override val locationPermission = false

    override fun testModules(): List<Module> = listOf(module { single { WalkthroughGate(showOnLaunch = true) } })

    @Test
    fun everyScreen() {
        walkThrough(screens) { screen -> capture("walkthrough_${screen.id.replace('-', '_')}") }
    }

    @Test
    fun permissionsGranted() {
        shadowOf(app).grantPermissions(
            *LocationService.PERMISSIONS,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALENDAR,
        )
        walkThrough(screens) { screen ->
            if (screen.permission != null) capture("walkthrough_${screen.id.replace('-', '_')}_granted")
        }
    }

    /** After an update with one new screen: no dots, no Skip, Back or Next, just Got it. */
    @Test
    fun runOfOne() {
        runBlocking { preferences.markWalkthroughSeen(ids - "appearance") }
        launch(hasText(string(R.string.walkthrough_appearance_title)))
        capture("walkthrough_run_1")
    }

    @Test
    fun runOfThree() {
        runBlocking { preferences.markWalkthroughSeen(ids.take(5)) }
        walkThrough(screens.drop(5)) { screen ->
            when (screen.id) {
                "device-calendars" -> capture("walkthrough_run_3_first")
                "appearance" -> capture("walkthrough_run_3_last")
            }
        }
    }

    /** Ten or more screens count "n of m" instead of dots. */
    @Test
    fun runOfTwelve() {
        val twelve = screens + screens.take(4).mapIndexed { i, screen ->
            FeatureScreen("extra-$i", screen.title, screen.body, screen.permission, illustration = screen.illustration)
        }
        koin.loadModules(
            listOf(module { viewModel { WalkthroughViewModel(get(), get(), get(), get(), twelve) } }),
            allowOverride = true,
        )
        walkThrough(twelve.take(3)) { screen ->
            if (screen.id == "days-shabbat-holidays") capture("walkthrough_run_12")
        }
    }

    /** Opens the app on [run]'s first screen and pages through it with Next. */
    private fun walkThrough(run: List<FeatureScreen>, atEach: (FeatureScreen) -> Unit) {
        launch(hasText(string(run.first().title)))
        run.forEachIndexed { index, screen ->
            if (index > 0) {
                clickable(string(R.string.walkthrough_next)).performClick()
                awaitText(string(screen.title))
                awaitGone(hasText(string(run[index - 1].title)))
            }
            atEach(screen)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params() = variants()
    }
}
