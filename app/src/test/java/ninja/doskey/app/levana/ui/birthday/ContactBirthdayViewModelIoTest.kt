package ninja.doskey.app.levana.ui.birthday

import android.Manifest
import android.app.Application
import android.net.Uri
import android.os.Looper
import android.provider.ContactsContract
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import ninja.doskey.app.levana.data.PickedContact
import ninja.doskey.app.levana.domain.model.ContactBirthday
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.FakeContactsProvider
import ninja.doskey.app.levana.data.TodayProvider
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

/** The birthday editor keeps the main thread free while contacts are read and written. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ContactBirthdayViewModelIoTest {

    private val mainThread get() = Looper.getMainLooper().thread

    @Before
    fun setUp() {
        FakeContactsProvider.reset()
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
        Robolectric.setupContentProvider(FakeContactsProvider::class.java, ContactsContract.AUTHORITY)
    }

    @After
    fun tearDown() {
        FakeContactsProvider.hold?.countDown()
    }

    private fun viewModel() = ContactBirthdayViewModel(
        ContactBirthdayRepository(RuntimeEnvironment.getApplication()),
        TodayProvider { LocalDate.of(2026, 10, 20) }
    )

    @Test
    fun loadingAStoredBirthdayLeavesTheMainThreadFree() {
        FakeContactsProvider.stored = Triple(12, 3, 5785)
        FakeContactsProvider.hold = CountDownLatch(1)
        val vm = viewModel()

        vm.onIntent(ContactBirthdayIntent.LoadBirthday(FakeContactsProvider.KEY))
        assertTrue(FakeContactsProvider.queryStarted.await(5, TimeUnit.SECONDS))

        // Query blocked on another thread; the editor still reacts on main
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(7))
        assertEquals(7, vm.state.value.hebrewDay)
        assertFalse(vm.state.value.isEditing)

        FakeContactsProvider.hold!!.countDown()
        awaitOnMain { vm.state.value.isEditing }

        // The day edited during the load is kept; the rest comes from the store
        assertEquals(7, vm.state.value.hebrewDay)
        assertEquals(3, vm.state.value.hebrewMonth)
        assertEquals(5785, vm.state.value.hebrewYear)
        assertEquals(FakeContactsProvider.NAME, vm.state.value.contactName)
        FakeContactsProvider.calls.forEach { assertNotSame(mainThread, it.thread) }
    }

    @Test
    fun pickedContactIsResolvedOffTheMainThread() {
        val vm = viewModel()

        vm.onIntent(ContactBirthdayIntent.ContactPicked(Uri.parse("content://com.android.contacts/contacts/1")))
        awaitOnMain { vm.state.value.contactLookupKey.isNotBlank() }

        assertEquals(FakeContactsProvider.KEY, vm.state.value.contactLookupKey)
        assertEquals(FakeContactsProvider.NAME, vm.state.value.contactName)
        assertEquals(FakeContactsProvider.PHOTO, vm.state.value.contactPhotoUri)
        assertNotSame(mainThread, FakeContactsProvider.calls.single().thread)
    }

    @Test
    fun saveWritesOffTheMainThreadAndCompletes() {
        val vm = viewModel()
        vm.onIntent(ContactBirthdayIntent.ContactSelected(FakeContactsProvider.KEY, FakeContactsProvider.NAME, null))
        vm.onIntent(ContactBirthdayIntent.SetHebrewYear(5790))
        vm.onIntent(ContactBirthdayIntent.SetHebrewMonth(8))
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(5))

        vm.onIntent(ContactBirthdayIntent.Save)
        assertTrue(vm.state.value.isSaving)
        awaitOnMain { vm.state.value.saveComplete }

        assertEquals(Triple(5, 8, 5790), FakeContactsProvider.stored)
        assertFalse(vm.state.value.isSaving)
        assertTrue(FakeContactsProvider.calls.any { it.kind == "insert" })
        FakeContactsProvider.calls.forEach { assertNotSame(mainThread, it.thread) }
    }

    @Test
    fun saveFailureIsShownNotThrown() {
        val vm = ContactBirthdayViewModel(
            object : ContactBirthdayRepository(RuntimeEnvironment.getApplication()) {
                override suspend fun setBirthday(contactLookupKey: String, day: Int, month: Int, year: Int) =
                    throw SecurityException("Permission denied")
            },
            TodayProvider { LocalDate.of(2026, 10, 20) }
        )
        vm.onIntent(ContactBirthdayIntent.ContactSelected(FakeContactsProvider.KEY, FakeContactsProvider.NAME, null))

        vm.onIntent(ContactBirthdayIntent.Save)
        awaitOnMain { vm.state.value.errorMessage != null }

        assertEquals("Permission denied", vm.state.value.errorMessage)
        assertFalse(vm.state.value.isSaving)
        assertFalse(vm.state.value.saveComplete)
    }

    /** Lookups that complete only when the test says so. */
    private class GatedRepository : ContactBirthdayRepository(RuntimeEnvironment.getApplication()) {
        val picks = mutableMapOf<Uri, CompletableDeferred<PickedContact?>>()
        val storedByKey = mutableMapOf<String, CompletableDeferred<ContactBirthday?>>()
        val loads = mutableListOf<String>()
        var saved: Pair<String, Triple<Int, Int, Int>>? = null
        val stored get() = stored("key")
        fun stored(key: String) = storedByKey.getOrPut(key) { CompletableDeferred() }
        override suspend fun resolveContact(contactUri: Uri): PickedContact? =
            picks.getOrPut(contactUri) { CompletableDeferred() }.await()
        override suspend fun getBirthdayForContact(contactLookupKey: String): ContactBirthday? {
            loads += contactLookupKey
            return stored(contactLookupKey).await()
        }
        override suspend fun setBirthday(contactLookupKey: String, day: Int, month: Int, year: Int) {
            saved = contactLookupKey to Triple(day, month, year)
        }
    }

    private fun rivka(key: String = "key", day: Int = 12) =
        ContactBirthday(key, "Rivka", null, day, 3, 5750)

    private val ContactBirthdayState.date get() = Triple(hebrewDay, hebrewMonth, hebrewYear)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    // ── Contact choice against a pending stored load ────────────────────────

    @Test
    fun aPickWhileTheStoredBirthdayLoadsWins() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)
        val uri = Uri.parse("content://com.android.contacts/contacts/2")

        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key"))
        vm.onIntent(ContactBirthdayIntent.ContactPicked(uri))
        repository.picks.getValue(uri).complete(PickedContact("kB", "Batya", null))
        awaitOnMain { vm.state.value.contactName == "Batya" }
        repository.stored.complete(rivka())
        idle()

        assertEquals("kB", vm.state.value.contactLookupKey)
        assertEquals("Batya", vm.state.value.contactName)
        assertFalse(vm.state.value.isEditing)
    }

    @Test
    fun aStoredLoadFinishingBeforeAnEarlierIssuedPickResolvesIsDropped() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)
        val uri = Uri.parse("content://com.android.contacts/contacts/2")

        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key"))
        vm.onIntent(ContactBirthdayIntent.ContactPicked(uri))
        repository.stored.complete(rivka())
        idle()
        assertEquals("", vm.state.value.contactName)

        repository.picks.getValue(uri).complete(PickedContact("kB", "Batya", null))
        awaitOnMain { vm.state.value.contactName == "Batya" }
        assertFalse(vm.state.value.isEditing)
    }

    @Test
    fun aDirectSelectionWhileTheStoredBirthdayLoadsWins() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)

        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key"))
        vm.onIntent(ContactBirthdayIntent.ContactSelected("kB", "Batya", null))
        repository.stored.complete(rivka())
        idle()

        assertEquals("kB", vm.state.value.contactLookupKey)
        assertFalse(vm.state.value.isEditing)
    }

    @Test
    fun repeatingTheLoadAfterAContactChoiceDoesNotBringTheStoredOneBack() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)

        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key"))
        vm.onIntent(ContactBirthdayIntent.ContactSelected("kB", "Batya", null))
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key")) // rotation
        repository.stored.complete(rivka())
        idle()

        assertEquals("kB", vm.state.value.contactLookupKey)
        assertEquals(listOf("key"), repository.loads)
    }

    // ── Repeated loads of the same record ───────────────────────────────────

    @Test
    fun aRepeatedLoadWhilePendingKeepsTheEditAndQueriesOnce() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)

        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key"))
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(20))
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key"))
        repository.stored.complete(rivka())
        awaitOnMain { vm.state.value.isEditing }

        assertEquals(Triple(20, 3, 5750), vm.state.value.date)
        assertEquals(listOf("key"), repository.loads)
    }

    @Test
    fun aRepeatedLoadAfterCompletionKeepsLaterEdits() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)

        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key"))
        repository.stored.complete(rivka())
        awaitOnMain { vm.state.value.isEditing }
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(25))
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key"))
        idle()

        assertEquals(Triple(25, 3, 5750), vm.state.value.date)
        assertEquals(listOf("key"), repository.loads)
    }

    // ── Loads of a different record ─────────────────────────────────────────

    @Test
    fun aLoadOfAnotherRecordReplacesThePendingOne() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)

        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k1"))
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(20))
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k2"))
        repository.stored("k2").complete(rivka("k2", day = 5))
        awaitOnMain { vm.state.value.isEditing }
        repository.stored("k1").complete(rivka("k1", day = 9))
        idle()

        // The edit belonged to the replaced record; k1's late result is dropped
        assertEquals("k2", vm.state.value.contactLookupKey)
        assertEquals(Triple(5, 3, 5750), vm.state.value.date)
        assertEquals(listOf("k1", "k2"), repository.loads)
    }

    @Test
    fun saveDuringAPendingLoadSavesWhatIsShownAndStillCompletes() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k1"))
        repository.stored("k1").complete(rivka("k1"))
        awaitOnMain { vm.state.value.isEditing }

        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k2"))
        vm.onIntent(ContactBirthdayIntent.Save)
        awaitOnMain { vm.state.value.saveComplete }
        repository.stored("k2").complete(rivka("k2", day = 5))
        idle()

        assertEquals("k1" to Triple(12, 3, 5750), repository.saved)
        assertEquals("k1", vm.state.value.contactLookupKey)
        assertTrue(vm.state.value.saveComplete)
    }

    private fun gatedViewModel(repository: GatedRepository) =
        ContactBirthdayViewModel(repository, TodayProvider { LocalDate.of(2026, 10, 20) })

    @Test
    fun aSlowEarlierPickNeverReplacesALaterPick() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)
        val first = Uri.parse("content://com.android.contacts/contacts/1")
        val second = Uri.parse("content://com.android.contacts/contacts/2")

        vm.onIntent(ContactBirthdayIntent.ContactPicked(first))
        vm.onIntent(ContactBirthdayIntent.ContactPicked(second))
        repository.picks.getValue(second).complete(PickedContact("kB", "Batya", null))
        awaitOnMain { vm.state.value.contactName == "Batya" }
        repository.picks.getValue(first).complete(PickedContact("kA", "Avi", null))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("Batya", vm.state.value.contactName)
        assertEquals("kB", vm.state.value.contactLookupKey)
    }

    @Test
    fun aPendingPickDoesNotReplaceAContactChosenAfterIt() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)
        val uri = Uri.parse("content://com.android.contacts/contacts/1")

        vm.onIntent(ContactBirthdayIntent.ContactPicked(uri))
        vm.onIntent(ContactBirthdayIntent.ContactSelected("kB", "Batya", null))
        repository.picks.getValue(uri).complete(PickedContact("kA", "Avi", null))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("Batya", vm.state.value.contactName)
    }

    @Test
    fun dateFieldsEditedWhileLoadingWinOverTheStoredDate() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)

        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key"))
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(20))
        vm.onIntent(ContactBirthdayIntent.SetHebrewYear(5790))
        repository.stored.complete(ContactBirthday("key", "Rivka", null, 12, 3, 5750))
        awaitOnMain { vm.state.value.isEditing }

        // Edited day and year kept, untouched month from the store
        assertEquals(20, vm.state.value.hebrewDay)
        assertEquals(3, vm.state.value.hebrewMonth)
        assertEquals(5790, vm.state.value.hebrewYear)
        assertEquals("Rivka", vm.state.value.contactName)
    }

    @Test
    fun anUneditedLoadStillShowsTheStoredDate() {
        val repository = GatedRepository()
        val vm = gatedViewModel(repository)

        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key"))
        repository.stored.complete(ContactBirthday("key", "Rivka", null, 12, 3, 5750))
        awaitOnMain { vm.state.value.isEditing }

        assertEquals(Triple(12, 3, 5750), vm.state.value.let { Triple(it.hebrewDay, it.hebrewMonth, it.hebrewYear) })
    }

    private fun awaitOnMain(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }
}
