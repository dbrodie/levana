package ninja.doskey.app.levana.ui.calendar

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.os.Looper
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
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
import ninja.doskey.app.levana.domain.model.HebrewYearMonth
import ninja.doskey.app.levana.ui.daydetail.DayDetailIntent
import ninja.doskey.app.levana.ui.daydetail.DayDetailViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
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

/**
 * Verifier end-to-end tests: the production Koin graph, a real in-memory Room database and
 * fake contacts/calendar providers that notify like the real ones. Only baseline-existing
 * APIs are used, so this file also runs against the baseline to demonstrate the bug.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class VerifyReactiveRefreshEndToEndTest {

    private lateinit var app: Application
    private lateinit var resolver: ContentResolver
    private lateinit var db: LevanaDatabase
    private lateinit var dao: ReadCountingDao
    private lateinit var koin: Koin
    private val store = ViewModelStore()

    private val events get() = koin.get<PersonalEventRepository>()
    private val preferences get() = koin.get<PreferencesRepository>()

    private val browsedMonth = YearMonth.now().plusMonths(5)
    private val selected = browsedMonth.atDay(14)

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        resolver = app.contentResolver
        FakeContactsProvider.reset()
        FakeCalendarProvider.reset()
        Robolectric.buildContentProvider(FakeContactsProvider::class.java)
            .create(ContactsContract.AUTHORITY)
        Robolectric.buildContentProvider(FakeCalendarProvider::class.java)
            .create(CalendarContract.AUTHORITY)
        shadowOf(app).grantPermissions(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALENDAR
        )
        db = Room.inMemoryDatabaseBuilder(app, LevanaDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        dao = ReadCountingDao(db.personalEventDao())
        koin = startKoin {
            allowOverride(true)
            androidContext(app)
            modules(
                allModules + module {
                    single { db }
                    single<PersonalEventDao> { dao }
                }
            )
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
        // Interleave virtual main-looper time with real time for DataStore/IO work.
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

    private fun calendarAt(month: YearMonth, date: LocalDate): CalendarViewModel {
        val c = vm { koin.get<CalendarViewModel>() }
        awaitUntil("initial load") { !c.state.value.isLoading && c.state.value.monthDays.isNotEmpty() }
        c.onIntent(CalendarIntent.LoadMonth(month))
        c.onIntent(CalendarIntent.SelectDay(date))
        awaitUntil("browse") {
            c.state.value.currentMonth == month && !c.state.value.isLoading &&
                c.state.value.gregorianMonthCache.keys ==
                setOf(month.minusMonths(1), month, month.plusMonths(1))
        }
        return c
    }

    private fun dayAt(date: LocalDate): DayDetailViewModel {
        val d = vm { koin.get<DayDetailViewModel>() }
        d.onIntent(DayDetailIntent.LoadDay(date))
        awaitUntil("day load") { !d.state.value.isLoading && d.state.value.dayInfo?.gregorianDate == date }
        return d
    }

    private fun CalendarViewModel.marked(date: LocalDate) =
        state.value.monthDays.single { it.gregorianDate == date }.hasPersonalEvent

    private fun CalendarViewModel.cachedMarked(month: YearMonth, date: LocalDate) =
        state.value.gregorianMonthCache.getValue(month).single { it.gregorianDate == date }.hasPersonalEvent

    private fun CalendarViewModel.dots(date: LocalDate) =
        state.value.monthDays.single { it.gregorianDate == date }.systemEventColors

    private fun DayDetailViewModel.customTitles() =
        state.value.calendarEvents.filterIsInstance<CalendarEvent.CustomEvent>().map { it.title }

    private fun DayDetailViewModel.birthdayNames() =
        state.value.calendarEvents.filterIsInstance<CalendarEvent.Birthday>().map { it.title }

    private fun DayDetailViewModel.systemTitles() = state.value.systemEvents.map { it.title }

    private fun notify(uri: Uri) = resolver.notifyChange(uri, null)

    private fun assertPreserved(c: CalendarViewModel, month: YearMonth = browsedMonth, date: LocalDate = selected) {
        assertEquals("browsed month preserved", month, c.state.value.currentMonth)
        assertEquals("selected date preserved", date, c.state.value.selectedDate)
        assertFalse("not stuck loading", c.state.value.isLoading)
    }

    // --- Room ----------------------------------------------------------------------------

    @Test
    fun `room insert edit delete refresh browsed month, both cached neighbours and open day`() {
        val c = calendarAt(browsedMonth, selected)
        val d = dayAt(selected)
        val prevDate = browsedMonth.minusMonths(1).atDay(9)
        val nextDate = browsedMonth.plusMonths(1).atDay(21)
        assertFalse(c.marked(selected))

        val id = runBlocking { events.insert(personalEventOn("Siyum", selected)) }
        runBlocking { events.insert(personalEventOn("Prev", prevDate)) }
        runBlocking { events.insert(personalEventOn("Next", nextDate)) }

        awaitUntil("insert reflected") { c.marked(selected) && d.customTitles() == listOf("Siyum") }
        awaitUntil("neighbours reflected") {
            c.cachedMarked(browsedMonth.minusMonths(1), prevDate) &&
                c.cachedMarked(browsedMonth.plusMonths(1), nextDate) &&
                c.cachedMarked(browsedMonth, selected)
        }
        assertPreserved(c)

        runBlocking { events.update(personalEventOn("Siyum HaShas", selected, id)) }
        awaitUntil("edit reflected") { d.customTitles() == listOf("Siyum HaShas") }

        // Edit that moves the event to another day of the browsed month.
        val moved = browsedMonth.atDay(3)
        runBlocking { events.update(personalEventOn("Siyum HaShas", moved, id)) }
        awaitUntil("move reflected") { !c.marked(selected) && c.marked(moved) && d.customTitles().isEmpty() }

        runBlocking { events.delete(personalEventOn("Siyum HaShas", moved, id)) }
        awaitUntil("delete reflected") { !c.marked(moved) }
        assertPreserved(c)
    }

    @Test
    fun `hebrew mode browsed month and its cached neighbours refresh on room change`() {
        runBlocking { preferences.saveCalendarHebrewMode(true) }
        val c = vm { koin.get<CalendarViewModel>() }
        awaitUntil("initial hebrew load") {
            c.state.value.calendarHebrewMode && !c.state.value.isLoading && c.state.value.monthDays.isNotEmpty()
        }
        val hym = HebrewYearMonth.from(selected)
        c.onIntent(CalendarIntent.LoadHebrewMonth(hym))
        c.onIntent(CalendarIntent.SelectDay(selected))
        awaitUntil("hebrew browse") {
            c.state.value.hebrewYearMonth == hym && !c.state.value.isLoading &&
                c.state.value.hebrewMonthCache.keys == setOf(hym.previous(), hym, hym.next())
        }
        val nextHym = hym.next()
        val nextDays = c.state.value.hebrewMonthCache.getValue(nextHym)
        val nextDate = nextDays[nextDays.size / 2].gregorianDate

        runBlocking { events.insert(personalEventOn("Hebrew", selected)) }
        runBlocking { events.insert(personalEventOn("HebrewNext", nextDate)) }

        awaitUntil("hebrew month + neighbour marked") {
            c.state.value.monthDays.single { it.gregorianDate == selected }.hasPersonalEvent &&
                c.state.value.hebrewMonthCache.getValue(nextHym)
                    .single { it.gregorianDate == nextDate }.hasPersonalEvent
        }
        assertEquals(hym, c.state.value.hebrewYearMonth)
        assertEquals(selected, c.state.value.selectedDate)
        assertFalse(c.state.value.isLoading)
    }

    // --- Contacts ------------------------------------------------------------------------

    @Test
    fun `contact birthday added and removed through the provider refreshes calendar and open day`() {
        val c = calendarAt(browsedMonth, selected)
        val d = dayAt(selected)
        assertFalse(c.marked(selected))

        FakeContactsProvider.rows += FakeContactsProvider.birthdayOn("Rivka", selected)
        notify(ContactsContract.Data.CONTENT_URI)
        awaitUntil("birthday shown") { c.marked(selected) && d.birthdayNames() == listOf("Rivka") }
        assertPreserved(c)

        FakeContactsProvider.rows.clear()
        notify(ContactsContract.AUTHORITY_URI)
        awaitUntil("birthday removed") { !c.marked(selected) && d.birthdayNames().isEmpty() }
        assertPreserved(c)
    }

    // --- Device calendar -----------------------------------------------------------------

    @Test
    fun `selected device calendar instances added and removed refresh dots and open day`() {
        val c = calendarAt(browsedMonth, selected)
        val d = dayAt(selected)
        assertEquals(emptyList<Int>(), c.dots(selected))

        FakeCalendarProvider.instances += FakeCalendarProvider.timedOn(1, "Dentist", 1L, 0xFF0000, selected)
        FakeCalendarProvider.instances += FakeCalendarProvider.allDayOn(2, "Hidden", 2L, 0x00FF00, selected)
        notify(CalendarContract.Events.CONTENT_URI)
        awaitUntil("instance shown") {
            c.dots(selected) == listOf(0xFF0000) && d.systemTitles() == listOf("Dentist")
        }
        assertPreserved(c)

        FakeCalendarProvider.instances.removeIf { it.eventId == 1L }
        notify(CalendarContract.CONTENT_URI)
        awaitUntil("instance removed") { c.dots(selected).isEmpty() && d.systemTitles().isEmpty() }
        assertPreserved(c)
    }

    // --- Bursts / loops / navigation races ----------------------------------------------

    @Test
    fun `a burst of changes is coalesced and no reload loop follows`() {
        val c = calendarAt(browsedMonth, selected)
        val d = dayAt(selected)
        idle(1_000)
        val before = dao.reads.get()
        val contactsBefore = FakeContactsProvider.queries.get()

        runBlocking { repeat(25) { events.insert(personalEventOn("Burst $it", selected)) } }
        repeat(25) { notify(ContactsContract.Data.CONTENT_URI) }
        repeat(25) { notify(CalendarContract.CONTENT_URI) }

        awaitUntil("burst reflected") { c.marked(selected) && d.customTitles().size == 25 }
        idle(2_000)
        val afterBurst = dao.reads.get()
        // One coalesced refresh is 3 calendar reads (month + 2 neighbours) + 1 day read.
        assertTrue("burst caused ${afterBurst - before} reads", afterBurst - before in 4..8)

        idle(5_000)
        assertEquals("no reload loop after settling", afterBurst, dao.reads.get())
        val contactsAfter = FakeContactsProvider.queries.get()
        idle(3_000)
        assertEquals("no contacts re-query loop", contactsAfter, FakeContactsProvider.queries.get())
        assertTrue(contactsAfter > contactsBefore)
        assertPreserved(c)
    }

    @Test
    fun `change during month navigation lands on the newly browsed month`() {
        val c = calendarAt(browsedMonth, selected)
        val target = browsedMonth.plusMonths(2)
        val targetDate = target.atDay(10)
        c.onIntent(CalendarIntent.LoadMonth(target))
        runBlocking { events.insert(personalEventOn("Race", targetDate)) }
        awaitUntil("race reflected") {
            c.state.value.currentMonth == target && !c.state.value.isLoading && c.marked(targetDate)
        }
        assertEquals(selected, c.state.value.selectedDate)
        idle(1_000)
        assertEquals(target, c.state.value.currentMonth)
        assertEquals(
            setOf(target.minusMonths(1), target, target.plusMonths(1)),
            c.state.value.gregorianMonthCache.keys
        )
    }

    @Test
    fun `open day follows the newly selected date when data changes`() {
        val d = dayAt(selected)
        val other = selected.plusDays(1)
        d.onIntent(DayDetailIntent.LoadDay(other))
        awaitUntil("other day") { d.state.value.dayInfo?.gregorianDate == other && !d.state.value.isLoading }
        runBlocking { events.insert(personalEventOn("Other", other)) }
        awaitUntil("other day refreshed") { d.customTitles() == listOf("Other") }
        assertEquals(other, d.state.value.dayInfo?.gregorianDate)
    }
}
