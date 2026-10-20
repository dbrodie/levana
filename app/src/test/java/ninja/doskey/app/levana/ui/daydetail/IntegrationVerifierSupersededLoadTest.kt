package ninja.doskey.app.levana.ui.daydetail

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.provider.CalendarContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.EventDataChanges
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.data.db.LevanaDatabase
import ninja.doskey.app.levana.data.runCancellable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Independent boundary checks (verify-bugfix-integration) of ProviderIo cancellation on
 * Dispatchers.Main.immediate: a superseded provider query that fails late must surface as
 * cancellation to its caller, so the caller's own error handling cannot publish stale state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class IntegrationVerifierSupersededLoadTest {

    /** Instances provider: the first query blocks, then fails; later queries answer "Fresh". */
    class ScriptedCalendarProvider : ContentProvider() {
        companion object {
            val calls = AtomicInteger()
            var entered = CountDownLatch(1)
            var release = CountDownLatch(1)
            var exited = CountDownLatch(1)
            var date: LocalDate = LocalDate.of(2026, 11, 10)
            var lateFailure: () -> Nothing = { throw SecurityException("permission revoked (stale)") }
        }

        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                           selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            if (calls.incrementAndGet() == 1) {
                entered.countDown()
                try {
                    release.await(10, TimeUnit.SECONDS)
                    lateFailure()
                } finally {
                    exited.countDown()
                }
            }
            val zone = ZoneId.systemDefault()
            val begin = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            return MatrixCursor(projection!!).apply {
                addRow(projection.map { column ->
                    when (column) {
                        CalendarContract.Instances.EVENT_ID -> 7L
                        CalendarContract.Instances.TITLE -> "Fresh"
                        CalendarContract.Instances.BEGIN -> begin
                        CalendarContract.Instances.END -> begin + 3_600_000
                        CalendarContract.Instances.ALL_DAY -> 0
                        CalendarContract.Instances.CALENDAR_ID -> 1L
                        CalendarContract.Instances.CALENDAR_COLOR -> 0xFF0000
                        else -> null
                    }
                }.toTypedArray())
            }
        }
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val store = ViewModelStore()
    private lateinit var db: LevanaDatabase

    @Before
    fun setUp() {
        ScriptedCalendarProvider.calls.set(0)
        ScriptedCalendarProvider.entered = CountDownLatch(1)
        ScriptedCalendarProvider.release = CountDownLatch(1)
        ScriptedCalendarProvider.exited = CountDownLatch(1)
        Robolectric.buildContentProvider(ScriptedCalendarProvider::class.java).create(CalendarContract.AUTHORITY)
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
        shadowOf(app).denyPermissions(Manifest.permission.READ_CONTACTS)
        db = Room.inMemoryDatabaseBuilder(app, LevanaDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        ScriptedCalendarProvider.release.countDown()
        store.clear()
        db.close()
    }

    private fun idleUntil(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition()) {
            assertTrue("timed out: $what", System.currentTimeMillis() < deadline)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))
            Thread.sleep(5)
        }
    }

    @Test
    fun `superseded day load whose calendar query fails late does not overwrite the newer day`() {
        val date = ScriptedCalendarProvider.date
        val prefs = PreferencesRepository(app, CityRepository(app))
        runBlocking { prefs.saveSelectedCalendarIds(setOf(1L)) }
        val noChanges = object : EventDataChanges {
            override val changes = emptyFlow<Unit>()
            override fun recheckPermissions() = Unit
        }
        val vm = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DayDetailViewModel(
                CalendarRepository(), ZmanimRepository(), prefs,
                PersonalEventRepository(db.personalEventDao()),
                ContactBirthdayRepository(app), SystemCalendarRepository(app), noChanges
            ) as T
        })[DayDetailViewModel::class.java]

        vm.onIntent(DayDetailIntent.LoadDay(date))
        idleUntil("first query blocked") { ScriptedCalendarProvider.entered.count == 0L }

        // The open day reloads (e.g. a refresh); this load supersedes the blocked one
        vm.onIntent(DayDetailIntent.LoadDay(date))
        idleUntil("newer load published") {
            !vm.state.value.isLoading && vm.state.value.systemEvents.map { it.title } == listOf("Fresh")
        }

        // The superseded query now fails; its caller catches SecurityException as "no events"
        ScriptedCalendarProvider.release.countDown()
        assertTrue(ScriptedCalendarProvider.exited.await(5, TimeUnit.SECONDS))
        repeat(30) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }

        assertEquals(
            "a stale failure must not replace the newer day's events",
            listOf("Fresh"), vm.state.value.systemEvents.map { it.title }
        )
        assertEquals(2, ScriptedCalendarProvider.calls.get())
    }

    @Test
    fun `on Main immediate a cancelled caller sees cancellation and an active caller the real error`() {
        val outcomes = mutableListOf<String>()
        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main.immediate)

        // Cancelled caller: work fails after cancellation
        val gate = CountDownLatch(1)
        val cancelled = scope.launch {
            try {
                runCancellable(Dispatchers.IO) { gate.await(5, TimeUnit.SECONDS); throw IllegalStateException("late") }
                outcomes += "cancelled:value"
            } catch (e: CancellationException) {
                outcomes += "cancelled:cancellation"
                throw e
            } catch (e: Exception) {
                outcomes += "cancelled:${e.javaClass.simpleName}"
            }
        }
        cancelled.cancel()
        gate.countDown()
        idleUntil("cancelled caller completes") { cancelled.isCompleted }

        // Cancelled caller: work succeeds after cancellation
        val gate2 = CountDownLatch(1)
        val cancelledOk = scope.launch {
            try {
                runCancellable(Dispatchers.IO) { gate2.await(5, TimeUnit.SECONDS); "late value" }
                outcomes += "cancelledOk:value"
            } catch (e: CancellationException) {
                outcomes += "cancelledOk:cancellation"
                throw e
            }
        }
        cancelledOk.cancel()
        gate2.countDown()
        idleUntil("second cancelled caller completes") { cancelledOk.isCompleted }

        // Active caller still receives the provider's real error
        val active = scope.launch {
            try {
                runCancellable(Dispatchers.IO) { throw SecurityException("revoked") }
            } catch (e: CancellationException) {
                outcomes += "active:cancellation"
                throw e
            } catch (e: SecurityException) {
                outcomes += "active:SecurityException"
            }
        }
        idleUntil("active caller completes") { active.isCompleted }

        assertEquals(
            listOf("cancelled:cancellation", "cancelledOk:cancellation", "active:SecurityException"),
            outcomes
        )
    }
}
