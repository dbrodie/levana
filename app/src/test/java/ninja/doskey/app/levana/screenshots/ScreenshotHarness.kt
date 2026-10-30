package ninja.doskey.app.levana.screenshots

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import android.os.Looper
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.AppDataStores
import ninja.doskey.app.levana.MainActivity
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.EventDataChanges
import ninja.doskey.app.levana.data.LocationService
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.db.LevanaDatabase
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.di.allModules
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.UserPreferences
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Screenshot baselines for the app shell and Settings (TASK-12.1). Each test launches the
 * real [MainActivity] (theme resolution, drawer, top bar, NavHost) over the production Koin
 * graph, with only the clock, database and change signals replaced, and navigates by UI.
 *
 * Record: `./gradlew :app:recordRoborazziDebug --tests '*.screenshots.*'`;
 * verify: `./gradlew :app:verifyRoborazziDebug`. See specs/ARCHITECTURE_SPEC.md.
 */
abstract class ScreenshotTest(
    private val variant: Variant,
    private val device: String? = null,
    private val fontScale: Float? = null,
) {

    /**
     * Light/dark × English/Hebrew, applied on top of the device before the activity starts:
     * the class's `@Config` qualifiers ([DEVICE]), or [device] when given (see [FoldableSize]).
     * [fontScale], when given, is the system font size (2 = 200%).
     */
    enum class Variant(val qualifiers: String) {
        EN_LIGHT("+en-notnight"),
        EN_DARK("+en-night"),
        HE_LIGHT("+iw-notnight"),
        HE_DARK("+iw-night");

        val tag = name.lowercase()
    }

    @get:Rule
    val compose: ComposeTestRule = createEmptyComposeRule()

    protected lateinit var app: Application
    protected lateinit var koin: Koin
    protected lateinit var db: LevanaDatabase
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var savedZone: TimeZone
    private lateinit var devicePreferences: StateFlow<UserPreferences>

    protected val preferences get() = koin.get<PreferencesRepository>()

    /**
     * Koin bindings for one test class, over the production graph and the harness's own. The
     * walkthrough stays off unless a test binds `WalkthroughGate(showOnLaunch = true)` here.
     */
    protected open fun testModules(): List<Module> = emptyList()

    /** Whether the device has granted location; set before [launch]. */
    protected open val locationPermission = true

    @Before
    fun setUpScreenshot() {
        savedZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE))
        (Audit.size?.qualifiers ?: device)?.let { RuntimeEnvironment.setQualifiers(it) }
        RuntimeEnvironment.setQualifiers(variant.qualifiers)
        (Audit.fontScale ?: fontScale)?.let { RuntimeEnvironment.setFontScale(it) }
        app = RuntimeEnvironment.getApplication()

        // About shows the version and installer; pin them so a git tag does not change it
        shadowOf(app.packageManager).getInternalMutablePackageInfo(app.packageName).versionName = VERSION_NAME
        shadowOf(app.packageManager).setInstallSourceInfo(app.packageName, null, null)
        if (locationPermission) {
            shadowOf(app).grantPermissions(*LocationService.PERMISSIONS)
        } else {
            shadowOf(app).denyPermissions(*LocationService.PERMISSIONS)
        }

        db = Room.inMemoryDatabaseBuilder(app, LevanaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val overrides = module {
            single { db }
            single<Flow<Unit>>(named("systemTimeChanges")) { emptyFlow() }
            // The device reads the seeded preferences without a DataStore round trip
            single { DeviceContext(devicePreferences, emptyFlow()) { FIXED_CLOCK } }
            single<EventDataChanges> { NoEventChanges }
        }
        koin = startKoin {
            allowOverride(true)
            androidContext(app)
            modules(allModules + overrides + testModules())
        }.koin

        AppDataStores.clearPreferences(app)
        devicePreferences = runBlocking {
            seedPreferences(preferences)
            MutableStateFlow(preferences.preferences.first())
        }
    }

    @After
    fun tearDownScreenshot() {
        scenario?.close()
        stopKoin()
        db.close()
        AppDataStores.clearPreferences(app)
        TimeZone.setDefault(savedZone)
    }

    /** Extra preference state for one test, applied after the shared seed. */
    protected open suspend fun seedPreferences(preferences: PreferencesRepository) {
        preferences.updateCurrentLocation(NEW_YORK)
        preferences.addSavedLocation(Location.JERUSALEM)
        preferences.addSavedLocation(LONDON)
        val jerusalem = preferences.preferences.first().savedLocations.first { it.location.name == "Jerusalem" }
        preferences.setActiveLocation(jerusalem.id)
    }

    /**
     * Starts [MainActivity] and waits for [ready]. Under Robolectric with the Compose test
     * dispatcher, a launch occasionally never receives its first preferences value: LevanaApp
     * stays empty (0×0) and later emissions do not revive it. That launch is closed and
     * retried; it shows nothing at all, so it cannot pass for a different image.
     */
    protected fun launch(ready: SemanticsMatcher, intent: Intent = Intent(app, MainActivity::class.java)) {
        repeat(LAUNCH_ATTEMPTS) { attempt ->
            scenario = ActivityScenario.launch(intent)
            advanceFrame()
            if (awaitOrFalse(ready, LAUNCH_TIMEOUT_MILLIS)) return
            println("ScreenshotTest: launch ${attempt + 1} of ${variant.tag} never showed its content; relaunching")
            scenario?.close()
            scenario = null
        }
        error("MainActivity did not show ${ready.description} in $LAUNCH_ATTEMPTS launches")
    }

    /** Starts the app on its start route, the calendar, once the day panel has loaded. */
    protected fun launchCalendar() {
        launch(hasContentDescription(string(R.string.design_open_menu)))
        // The month grid shows no clock times; the day panel's zmanim do
        await(
            SemanticsMatcher("shows a clock time") { node ->
                node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { CLOCK_TIME.containsMatchIn(it.text) }
            }
        )
    }

    protected fun openDrawer() {
        compose.onAllNodes(hasContentDescription(string(R.string.design_open_menu)))[0].performClick()
        awaitText(string(R.string.design_manage_cities))
    }

    protected fun openSettings() {
        launchCalendar()
        openDrawer()
        clickable(string(R.string.nav_settings)).performClick()
        // The view model starts from SettingsState(), which hides the developer group; the
        // stored preferences show it, so its row (below the fold) marks the loaded state
        await(hasText(string(R.string.settings_developer_settings)))
    }

    /** Runs [block] on the launched activity, e.g. to deliver an activity result. */
    protected fun onActivity(block: (MainActivity) -> Unit) {
        checkNotNull(scenario) { "launch first" }.onActivity(block)
        advanceFrame()
    }

    protected fun string(@StringRes id: Int, vararg args: Any): String = app.getString(id, *args)

    /** Waits until a node matching [matcher] is shown (async loads finish), then until idle. */
    protected fun await(matcher: SemanticsMatcher) {
        check(awaitOrFalse(matcher, AWAIT_TIMEOUT_MILLIS)) { "Timed out waiting for ${matcher.description}" }
    }

    /** Waits until no node matches [matcher] (an animation or navigation has finished), then until idle. */
    protected fun awaitGone(matcher: SemanticsMatcher) {
        val start = System.currentTimeMillis()
        while (compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()) {
            check(System.currentTimeMillis() - start <= AWAIT_TIMEOUT_MILLIS) { "Timed out waiting for no ${matcher.description}" }
            Thread.sleep(10)
            advanceFrame()
        }
        compose.waitForIdle()
    }

    private fun awaitOrFalse(matcher: SemanticsMatcher, timeoutMillis: Long): Boolean {
        val start = System.currentTimeMillis()
        while (compose.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()) {
            if (System.currentTimeMillis() - start > timeoutMillis) return false
            Thread.sleep(10)
            advanceFrame()
        }
        compose.waitForIdle()
        return true
    }

    /**
     * Runs the main looper a frame ahead. Under the paused looper, window layout and other
     * Choreographer work wait for its fake clock, which Compose idling does not advance.
     */
    private fun advanceFrame() {
        shadowOf(Looper.getMainLooper()).idleFor(FRAME)
        compose.waitForIdle()
    }

    /**
     * Waits for [text] within the screen's width; the closed drawer keeps its own text
     * composed beside it.
     */
    protected fun awaitText(text: String) = await(hasText(text) and onScreen())

    protected fun onScreen() = SemanticsMatcher("is within the screen's width") { node ->
        val bounds = node.boundsInRoot
        val root = node.layoutInfo.coordinates.findRootCoordinates().size
        bounds.width > 0 && bounds.left >= 0 && bounds.right <= root.width
    }

    protected fun node(text: String): SemanticsNodeInteraction =
        compose.onAllNodes(hasText(text))[0]

    /** The clickable element showing [text]; a group header may carry the same words. */
    protected fun clickable(text: String): SemanticsNodeInteraction {
        val matcher = hasText(text) and hasClickAction()
        await(matcher)
        return compose.onAllNodes(matcher)[0]
    }

    /**
     * Lets values still in flight from DataStore's IO thread reach the UI. Compose idling
     * does not wait for other threads, and MainActivity reads the preferences itself.
     */
    private fun settle() {
        repeat(SETTLE_ROUNDS) {
            Thread.sleep(SETTLE_MILLIS)
            advanceFrame()
        }
    }

    @OptIn(ExperimentalRoborazziApi::class)
    protected fun capture(name: String) {
        settle()
        captureScreenRoboImage("${Audit.dir ?: OUTPUT_DIR}/${name}_${variant.tag}.png")
    }

    /**
     * Opt-in layout audit (TASK-52.2): with `-Plevana.audit.size=<FoldableSize>` and
     * `-Plevana.audit.dir=<absolute path outside the worktree>` (optionally
     * `-Plevana.audit.fontScale=2`), `recordRoborazziDebug` renders every screenshot test at that
     * window size into that directory instead of the baselines. Not for verify runs.
     */
    private object Audit {
        val size: FoldableSize? = System.getProperty("levana.audit.size")?.let { FoldableSize.valueOf(it) }
        val fontScale: Float? = System.getProperty("levana.audit.fontScale")?.toFloat()
        val dir: String? = System.getProperty("levana.audit.dir")
    }

    private object NoEventChanges : EventDataChanges {
        override val changes: Flow<Unit> = emptyFlow()
        override fun recheckPermissions() = Unit
    }

    companion object {
        /** A fixed medium phone at mdpi, which keeps the committed images small. */
        const val DEVICE = "w393dp-h851dp-normal-long-notround-port-mdpi-keyshidden-nonav"
        const val OUTPUT_DIR = "src/test/screenshots"
        const val VERSION_NAME = "1.2.3-test"
        private const val SETTLE_ROUNDS = 3
        private const val SETTLE_MILLIS = 50L
        private val CLOCK_TIME = Regex("""\d{1,2}:\d{2}""")
        private val FRAME: Duration = Duration.ofMillis(16)
        private const val LAUNCH_ATTEMPTS = 5
        private const val LAUNCH_TIMEOUT_MILLIS = 3_000L
        private const val AWAIT_TIMEOUT_MILLIS = 10_000L

        val ZONE: ZoneId = ZoneId.of("America/New_York")

        /**
         * Noon on 7 Kislev 5787, outside every holiday theme. MainActivity applies a theme on a
         * later composition, after its own preferences and today arrive; under Robolectric a
         * capture occasionally preceded it, so the images avoid holiday colors altogether.
         */
        val TODAY: LocalDate = LocalDate.of(2026, 11, 17)
        val FIXED_CLOCK: Clock = Clock.fixed(ZonedDateTime.of(TODAY, LocalTime.NOON, ZONE).toInstant(), ZONE)

        val NEW_YORK = Location(
            latitude = 40.7128, longitude = -74.0060, elevation = 10.0,
            timezoneId = "America/New_York", name = "New York", country = "United States"
        )
        val LONDON = Location(
            latitude = 51.5074, longitude = -0.1278, elevation = 11.0,
            timezoneId = "Europe/London", name = "London", country = "United Kingdom"
        )

        @JvmStatic
        fun variants(): List<Array<Any>> = Variant.entries.map { arrayOf(it) }
    }
}
