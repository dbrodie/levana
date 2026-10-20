package ninja.doskey.app.levana.ui.birthday

import android.Manifest
import android.app.Application
import android.net.Uri
import android.os.Looper
import android.provider.ContactsContract
import java.time.LocalDate
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

        assertEquals(12, vm.state.value.hebrewDay)
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

    private fun awaitOnMain(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }
}
