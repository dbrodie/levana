package ninja.doskey.app.levana.ui.calendar

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.os.Looper
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.room.Room
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.db.LevanaDatabase
import ninja.doskey.app.levana.data.db.PersonalEventDao
import ninja.doskey.app.levana.di.allModules
import ninja.doskey.app.levana.domain.model.CalendarEvent
import ninja.doskey.app.levana.ui.daydetail.DayDetailIntent
import ninja.doskey.app.levana.ui.daydetail.DayDetailViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Verifier tests for pause/resume, permission grant/revocation and observer sharing/cleanup. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class VerifyReactiveRefreshLifecycleTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var app: Application
    private lateinit var resolver: ContentResolver
    private lateinit var db: LevanaDatabase
    private lateinit var dao: ReadCountingDao
    private lateinit var koin: Koin
    private var store = ViewModelStore()

    private val events get() = koin.get<PersonalEventRepository>()
    private val preferences get() = koin.get<PreferencesRepository>()

    private val browsedMonth = YearMonth.now().plusMonths(5)
    private val selected = browsedMonth.atDay(14)

    private val contactPerms = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
    private val calendarPerm = Manifest.permission.READ_CALENDAR

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        resolver = app.contentResolver
        FakeContactsProvider.reset()
        FakeCalendarProvider.reset()
        Robolectric.buildContentProvider(FakeContactsProvider::class.java).create(ContactsContract.AUTHORITY)
        Robolectric.buildContentProvider(FakeCalendarProvider::class.java).create(CalendarContract.AUTHORITY)
        shadowOf(app).grantPermissions(*contactPerms, calendarPerm)
        db = Room.inMemoryDatabaseBuilder(app, LevanaDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        dao = ReadCountingDao(db.personalEventDao())
        koin = startKoin {
            allowOverride(true)
            androidContext(app)
            modules(allModules + module {
                single { db }
                single<PersonalEventDao> { dao }
            })
        }.koin
        runBlocking {
            preferences.saveSelectedCalendarIds(setOf(1L))
            preferences.saveCalendarHebrewMode(false)
            preferences.saveDevDateOverride(null)
        }
    }

    @After
    fun tearDown() {
        store.clear()
        idle(50)
        stopKoin()
        db.close()
    }

    // --- helpers -------------------------------------------------------------------------

    private inline fun <reified T : ViewModel> vm(crossinline make: () -> T): T =
        ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = make() as V
        })[T::class.java]

    private fun idle(ms: Long) {
        var left = ms
        while (left > 0) {
            val step = minOf(left, 50L)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(step))
            Thread.sleep(5)
            left -= step
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun awaitUntil(message: String, timeoutMs: Long = 8_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("timed out: $message")
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(10)
        }
    }

    private fun calendarAt(month: YearMonth = browsedMonth, date: LocalDate = selected): CalendarViewModel {
        val c = vm { koin.get<CalendarViewModel>() }
        awaitUntil("initial load") { !c.state.value.isLoading && c.state.value.monthDays.isNotEmpty() }
        c.onIntent(CalendarIntent.Resume)
        c.onIntent(CalendarIntent.LoadMonth(month))
        c.onIntent(CalendarIntent.SelectDay(date))
        awaitUntil("browse") {
            c.state.value.currentMonth == month && !c.state.value.isLoading &&
                c.state.value.gregorianMonthCache.size == 3
        }
        return c
    }

    private fun dayAt(date: LocalDate = selected): DayDetailViewModel {
        val d = vm { koin.get<DayDetailViewModel>() }
        d.onIntent(DayDetailIntent.Resume)
        d.onIntent(DayDetailIntent.LoadDay(date))
        awaitUntil("day load") { !d.state.value.isLoading && d.state.value.dayInfo?.gregorianDate == date }
        return d
    }

    private fun CalendarViewModel.marked(date: LocalDate) =
        state.value.monthDays.single { it.gregorianDate == date }.hasPersonalEvent

    private fun CalendarViewModel.dots(date: LocalDate) =
        state.value.monthDays.single { it.gregorianDate == date }.systemEventColors

    private fun DayDetailViewModel.customTitles() =
        state.value.calendarEvents.filterIsInstance<CalendarEvent.CustomEvent>().map { it.title }

    private fun DayDetailViewModel.birthdayNames() =
        state.value.calendarEvents.filterIsInstance<CalendarEvent.Birthday>().map { it.title }

    private fun observers(uri: Uri) = shadowOf(resolver).getContentObservers(uri).size

    private fun pauseBoth(c: CalendarViewModel, d: DayDetailViewModel) {
        c.onIntent(CalendarIntent.Pause); d.onIntent(DayDetailIntent.Pause)
    }

    private fun resumeBoth(c: CalendarViewModel, d: DayDetailViewModel) {
        c.onIntent(CalendarIntent.Resume); d.onIntent(DayDetailIntent.Resume)
    }

    // --- pause / resume ------------------------------------------------------------------

    @Test
    fun `changes while paused do not reload, then apply exactly once on resume`() {
        val c = calendarAt()
        val d = dayAt()
        idle(1_000)
        pauseBoth(c, d)
        val before = dao.reads.get()

        runBlocking { events.insert(personalEventOn("Paused", selected)) }
        FakeContactsProvider.rows += FakeContactsProvider.birthdayOn("Leah", selected)
        resolver.notifyChange(ContactsContract.Data.CONTENT_URI, null)
        FakeCalendarProvider.instances += FakeCalendarProvider.timedOn(7, "Meeting", 1L, 0x123456, selected)
        resolver.notifyChange(CalendarContract.CONTENT_URI, null)
        idle(2_000)

        assertEquals("no reload while paused", before, dao.reads.get())
        assertFalse("still stale while paused", c.marked(selected))
        assertTrue(d.customTitles().isEmpty())

        resumeBoth(c, d)
        awaitUntil("applied on resume") {
            c.marked(selected) && c.dots(selected) == listOf(0x123456) &&
                d.customTitles() == listOf("Paused") && d.birthdayNames() == listOf("Leah") &&
                d.state.value.systemEvents.map { it.title } == listOf("Meeting")
        }
        idle(2_000)
        assertEquals("exactly one calendar (3) + one day (1) reload", before + 4, dao.reads.get())
        assertEquals(browsedMonth, c.state.value.currentMonth)
        assertEquals(selected, c.state.value.selectedDate)
    }

    @Test
    fun `pause and resume without any change does not reload`() {
        val c = calendarAt()
        val d = dayAt()
        idle(1_000)
        val before = dao.reads.get()
        repeat(5) {
            pauseBoth(c, d)
            idle(100)
            resumeBoth(c, d)
            idle(500)
        }
        assertEquals(before, dao.reads.get())
    }

    @Test
    fun `navigating while paused then resuming refreshes the newly browsed month and keeps selection`() {
        val c = calendarAt()
        val d = dayAt()
        pauseBoth(c, d)
        val target = browsedMonth.plusMonths(3)
        val targetDate = target.atDay(5)
        runBlocking { events.insert(personalEventOn("Later", targetDate)) }
        idle(500)
        c.onIntent(CalendarIntent.LoadMonth(target))
        awaitUntil("navigated") { c.state.value.currentMonth == target && !c.state.value.isLoading }
        resumeBoth(c, d)
        awaitUntil("marked") { c.marked(targetDate) }
        idle(1_000)
        assertEquals(target, c.state.value.currentMonth)
        assertEquals(selected, c.state.value.selectedDate)
        assertEquals(selected, d.state.value.dayInfo?.gregorianDate)
    }

    // --- permissions ---------------------------------------------------------------------

    @Test
    fun `granting contacts and calendar permission starts observing and shows data on resume`() {
        shadowOf(app).denyPermissions(*contactPerms, calendarPerm)
        FakeContactsProvider.rows += FakeContactsProvider.birthdayOn("Sarah", selected)
        FakeCalendarProvider.instances += FakeCalendarProvider.timedOn(3, "Call", 1L, 0x0000FF, selected)
        val c = calendarAt()
        val d = dayAt()
        idle(500)
        assertEquals(0, observers(ContactsContract.AUTHORITY_URI))
        assertEquals(0, observers(CalendarContract.CONTENT_URI))
        assertFalse(c.marked(selected))
        assertTrue(c.dots(selected).isEmpty())

        // User grants in the system dialog / settings; the screen pauses and resumes.
        pauseBoth(c, d)
        shadowOf(app).grantPermissions(*contactPerms, calendarPerm)
        resumeBoth(c, d)
        awaitUntil("granted data shown") {
            c.marked(selected) && c.dots(selected) == listOf(0x0000FF) &&
                d.birthdayNames() == listOf("Sarah")
        }
        assertEquals(1, observers(ContactsContract.AUTHORITY_URI))
        assertEquals(1, observers(CalendarContract.CONTENT_URI))

        // Now live: a provider change is picked up without another resume.
        FakeContactsProvider.rows.clear()
        resolver.notifyChange(ContactsContract.Data.CONTENT_URI, null)
        awaitUntil("live after grant") { d.birthdayNames().isEmpty() && !c.marked(selected) }
        assertEquals(browsedMonth, c.state.value.currentMonth)
        assertEquals(selected, c.state.value.selectedDate)
    }

    @Test
    fun `revoking permissions stops observing and hides provider data on resume`() {
        FakeContactsProvider.rows += FakeContactsProvider.birthdayOn("Dina", selected)
        FakeCalendarProvider.instances += FakeCalendarProvider.timedOn(4, "Gym", 1L, 0x00FFFF, selected)
        val c = calendarAt()
        val d = dayAt()
        awaitUntil("visible") { c.marked(selected) && c.dots(selected).isNotEmpty() }
        assertEquals(1, observers(ContactsContract.AUTHORITY_URI))
        assertEquals(1, observers(CalendarContract.CONTENT_URI))

        pauseBoth(c, d)
        shadowOf(app).denyPermissions(*contactPerms, calendarPerm)
        resumeBoth(c, d)
        awaitUntil("hidden after revoke") {
            !c.marked(selected) && c.dots(selected).isEmpty() && d.birthdayNames().isEmpty()
        }
        assertEquals(0, observers(ContactsContract.AUTHORITY_URI))
        assertEquals(0, observers(CalendarContract.CONTENT_URI))

        // Room changes still flow after the observers were swapped out.
        runBlocking { events.insert(personalEventOn("After revoke", selected)) }
        awaitUntil("room still live") { c.marked(selected) && d.customTitles() == listOf("After revoke") }
    }

    // --- sharing / cleanup / threads -------------------------------------------------------

    @Test
    fun `observers are shared between view models and released when both are cleared`() {
        val c = calendarAt()
        val d = dayAt()
        idle(200)
        assertEquals(1, observers(ContactsContract.AUTHORITY_URI))
        assertEquals(1, observers(CalendarContract.CONTENT_URI))

        store.clear()
        idle(200)
        assertEquals(0, observers(ContactsContract.AUTHORITY_URI))
        assertEquals(0, observers(CalendarContract.CONTENT_URI))
        assertTrue(c.state.value.monthDays.isNotEmpty() && d.state.value.dayInfo != null)

        // A fresh pair of view models (re-subscription) loads once and still reacts.
        store = ViewModelStore()
        val c2 = calendarAt()
        val d2 = dayAt()
        idle(1_000)
        val settled = dao.reads.get()
        idle(1_000)
        assertEquals("no spurious refresh after re-subscription", settled, dao.reads.get())
        assertEquals(1, observers(ContactsContract.AUTHORITY_URI))
        runBlocking { events.insert(personalEventOn("Again", selected)) }
        awaitUntil("re-subscribed live") { c2.marked(selected) && d2.customTitles() == listOf("Again") }
    }

    @Test
    fun `provider notification from a background thread is delivered to the main thread`() {
        val c = calendarAt()
        val d = dayAt()
        FakeCalendarProvider.instances += FakeCalendarProvider.allDayOn(9, "Holiday", 1L, 0xABCDEF, selected)
        val t = Thread { resolver.notifyChange(CalendarContract.Events.CONTENT_URI, null) }
        t.start(); t.join()
        awaitUntil("bg notify") {
            c.dots(selected) == listOf(0xABCDEF) &&
                d.state.value.systemEvents.map { it.title } == listOf("Holiday")
        }
    }

    // --- Compose lifecycle wiring ----------------------------------------------------------

    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun `calendar screen wires its lifecycle pause and resume to both view models`() {
        // The screen's pager opens on today's month, so use a day in it.
        val month = YearMonth.now()
        val day = month.atDay(if (LocalDate.now().dayOfMonth == 14) 15 else 14)
        val c = vm { koin.get<CalendarViewModel>() }
        val d = vm { koin.get<DayDetailViewModel>() }
        val owner = Owner().apply { registry.currentState = Lifecycle.State.RESUMED }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                CalendarScreen(viewModel = c, dayDetailViewModel = d)
            }
        }
        compose.waitForIdle()
        c.onIntent(CalendarIntent.SelectDay(day))
        compose.waitForIdle()
        awaitUntil("screen loaded") {
            c.state.value.currentMonth == month && !c.state.value.isLoading &&
                d.state.value.dayInfo?.gregorianDate == day && !d.state.value.isLoading
        }
        idle(1_000)

        // Leave the screen (e.g. navigate to the event editor): RESUMED -> CREATED.
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.waitForIdle()
        val before = dao.reads.get()
        runBlocking { events.insert(personalEventOn("Edited elsewhere", day)) }
        idle(1_500)
        compose.waitForIdle()
        assertEquals("no reload while screen is not resumed", before, dao.reads.get())
        assertFalse(c.marked(day))

        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitForIdle()
        awaitUntil("refreshed on return") { c.marked(day) && d.customTitles() == listOf("Edited elsewhere") }
        assertEquals(month, c.state.value.currentMonth)
        assertEquals(day, c.state.value.selectedDate)
    }
}
