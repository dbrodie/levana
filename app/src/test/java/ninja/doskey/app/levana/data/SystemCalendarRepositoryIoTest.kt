package ninja.doskey.app.levana.data

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.Looper
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ninja.doskey.app.levana.ui.calendarselection.CalendarSelectionIntent
import ninja.doskey.app.levana.ui.calendarselection.CalendarSelectionViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Calendar provider queries, including calendar selection, never run on the main thread. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SystemCalendarRepositoryIoTest {

    private val app get() = RuntimeEnvironment.getApplication()
    private val mainThread get() = Looper.getMainLooper().thread

    @Before
    fun setUp() {
        CalendarsProvider.reset()
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
        Robolectric.setupContentProvider(CalendarsProvider::class.java, CalendarContract.AUTHORITY)
    }

    @After
    fun tearDown() {
        CalendarsProvider.hold?.countDown()
    }

    @Test
    fun allQueriesRunOffTheCallingMainThread() = runBlocking<Unit> {
        assertEquals(mainThread, Thread.currentThread())
        val repository = SystemCalendarRepository(app)
        val day = LocalDate.of(2026, 10, 20)

        val calendars = repository.getDeviceCalendars()
        val colors = repository.getEventColorsForDateRange(day, day, setOf(CAL_ID))
        val events = repository.getEventsForDate(day, setOf(CAL_ID))

        assertEquals(listOf("Family"), calendars.map { it.name })
        assertEquals(mapOf(day to listOf(COLOR)), colors)
        assertEquals(1, events.size)
        assertEquals(3, CalendarsProvider.threads.size)
        CalendarsProvider.threads.forEach { assertNotSame(mainThread, it) }
    }

    @Test
    fun cancellingAMonthLoadReleasesTheCallerAndCancelsTheQuery() {
        CalendarsProvider.hold = CountDownLatch(1) // never released by the test
        val repository = SystemCalendarRepository(app)
        val day = LocalDate.of(2026, 10, 20)

        runBlocking {
            val load = async(start = CoroutineStart.UNDISPATCHED) {
                repository.getEventColorsForDateRange(day, day, setOf(CAL_ID))
            }
            assertTrue(CalendarsProvider.queryStarted.await(5, TimeUnit.SECONDS))
            load.cancel()
            withTimeout(2_000) { load.join() }
            assertTrue(load.isCancelled)
        }
        assertTrue(CalendarsProvider.signalCancelled.await(0, TimeUnit.SECONDS))
    }

    @Test
    fun calendarSelectionStaysResponsiveWhileCalendarsLoad() {
        CalendarsProvider.hold = CountDownLatch(1)
        val viewModel = CalendarSelectionViewModel(
            SystemCalendarRepository(app),
            PreferencesRepository(app, CityRepository(app))
        )

        // The load is in flight on another thread; the main thread is free to
        // handle a toggle in the meantime
        assertTrue(CalendarsProvider.queryStarted.await(5, TimeUnit.SECONDS))
        assertTrue(viewModel.state.value.isLoading)
        viewModel.onIntent(CalendarSelectionIntent.ToggleCalendar(CAL_ID))
        assertEquals(setOf(CAL_ID), viewModel.state.value.selectedIds)

        CalendarsProvider.hold!!.countDown()
        awaitOnMain { !viewModel.state.value.isLoading }

        assertEquals(listOf("Family"), viewModel.state.value.calendars.map { it.name })
        assertNotSame(mainThread, CalendarsProvider.threads.single())
    }

    @Test
    fun aSlowerEarlierLoadNeverReplacesANewerCalendarList() {
        CalendarsProvider.hold = CountDownLatch(1)
        CalendarsProvider.holdFirstOnly = true
        val viewModel = CalendarSelectionViewModel(
            SystemCalendarRepository(app),
            PreferencesRepository(app, CityRepository(app))
        )
        assertTrue(CalendarsProvider.queryStarted.await(5, TimeUnit.SECONDS))

        // A repeated permission refresh queries again and finishes first
        CalendarsProvider.calendarName = "Work"
        viewModel.onIntent(CalendarSelectionIntent.PermissionGranted)
        awaitOnMain { !viewModel.state.value.isLoading }
        assertEquals(listOf("Work"), viewModel.state.value.calendars.map { it.name })

        CalendarsProvider.hold!!.countDown()
        holdsFor { viewModel.state.value.calendars.map { it.name } == listOf("Work") }

        assertEquals(2, CalendarsProvider.threads.size)
    }

    @Test
    fun aPendingLoadNeverRestoresPermissionAfterANewerLoadFoundNone() {
        CalendarsProvider.hold = CountDownLatch(1)
        val viewModel = CalendarSelectionViewModel(
            SystemCalendarRepository(app),
            PreferencesRepository(app, CityRepository(app))
        )
        assertTrue(CalendarsProvider.queryStarted.await(5, TimeUnit.SECONDS))

        shadowOf(app).denyPermissions(Manifest.permission.READ_CALENDAR)
        viewModel.onIntent(CalendarSelectionIntent.Load)
        assertFalse(viewModel.state.value.hasPermission)
        assertFalse(viewModel.state.value.isLoading)

        CalendarsProvider.hold!!.countDown()
        holdsFor { !viewModel.state.value.hasPermission }

        assertTrue(viewModel.state.value.calendars.isEmpty())
    }

    @Test
    fun aSupersededLoadThatLaterFailsCannotHideTheNewerList() {
        // The first query ignores its cancellation and then reports revocation
        CalendarsProvider.hold = CountDownLatch(1)
        CalendarsProvider.holdFirstOnly = true
        CalendarsProvider.ignoreSignal = true
        CalendarsProvider.failAfterHold = { SecurityException("permission revoked") }
        val viewModel = CalendarSelectionViewModel(
            SystemCalendarRepository(app),
            PreferencesRepository(app, CityRepository(app))
        )
        assertTrue(CalendarsProvider.queryStarted.await(5, TimeUnit.SECONDS))

        CalendarsProvider.calendarName = "Work"
        viewModel.onIntent(CalendarSelectionIntent.PermissionGranted)
        awaitOnMain { !viewModel.state.value.isLoading }

        CalendarsProvider.hold!!.countDown()
        holdsFor {
            viewModel.state.value.hasPermission &&
                viewModel.state.value.calendars.map { it.name } == listOf("Work")
        }
    }

    /** Lets any stale completion run for a second; [condition] must hold throughout. */
    private fun holdsFor(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 1_000
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue("stale load published", condition())
            Thread.sleep(10)
        }
    }

    private fun awaitOnMain(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }

    /** One calendar with one all-day event on 20 October 2026. */
    class CalendarsProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor = query(uri, projection, selection, selectionArgs, sortOrder, null)

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
            cancellationSignal: CancellationSignal?
        ): Cursor {
            threads += Thread.currentThread()
            // Captured when the query starts, like a provider snapshot
            val name = calendarName
            val held = if (holdFirstOnly && threads.size > 1) null else hold
            held?.let { latch ->
                if (!ignoreSignal) {
                    cancellationSignal?.setOnCancelListener {
                        signalCancelled.countDown()
                        latch.countDown()
                    }
                }
                queryStarted.countDown()
                latch.await(5, TimeUnit.SECONDS)
                failAfterHold?.let { throw it() }
                cancellationSignal?.throwIfCanceled()
            }
            val columns = projection!!
            val start = LocalDate.of(2026, 10, 20).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val values = mapOf(
                CalendarContract.Calendars._ID to CAL_ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME to name,
                CalendarContract.Calendars.ACCOUNT_NAME to "me",
                CalendarContract.Calendars.CALENDAR_COLOR to COLOR,
                CalendarContract.Instances.EVENT_ID to 1L,
                CalendarContract.Instances.TITLE to "Trip",
                CalendarContract.Instances.BEGIN to start,
                CalendarContract.Instances.END to start + 86_400_000L,
                CalendarContract.Instances.ALL_DAY to 1,
                CalendarContract.Instances.CALENDAR_ID to CAL_ID
            )
            return MatrixCursor(columns).apply { addRow(columns.map { values[it] }) }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ) = 0

        companion object {
            val threads = CopyOnWriteArrayList<Thread>()
            @Volatile var hold: CountDownLatch? = null
            @Volatile var holdFirstOnly = false
            @Volatile var calendarName = "Family"
            @Volatile var ignoreSignal = false
            @Volatile var failAfterHold: (() -> RuntimeException)? = null
            @Volatile var queryStarted = CountDownLatch(1)
            @Volatile var signalCancelled = CountDownLatch(1)

            fun reset() {
                threads.clear()
                hold = null
                holdFirstOnly = false
                ignoreSignal = false
                failAfterHold = null
                calendarName = "Family"
                queryStarted = CountDownLatch(1)
                signalCancelled = CountDownLatch(1)
            }
        }
    }

    private companion object {
        const val CAL_ID = 7L
        const val COLOR = 0xFF2196F3.toInt()
    }
}
