package ninja.doskey.app.levana.verifyio

import android.provider.ContactsContract
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.TodayProvider
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayIntent
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Same/different record requests, selection and Save against a pending stored load. */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class VerifyBirthdayLoadLifecycleTest {

    private val contacts = ContactsWorld()
    private lateinit var provider: ScriptedProvider
    private val gates = ConcurrentHashMap<String, CountDownLatch>()
    private val storedQueries = ConcurrentHashMap<String, AtomicInteger>()
    private lateinit var k7: ContactsWorld.Contact
    private lateinit var k8: ContactsWorld.Contact

    @Before
    fun setUp() {
        grantAll()
        provider = installContacts(contacts)
        provider.responder = { uri, proj, sel, args ->
            val key = args?.getOrNull(1)
            if (uri == ContactsContract.Data.CONTENT_URI && key != null && args[0] == BIRTHDAY_MIME) {
                storedQueries.getOrPut(key) { AtomicInteger() }.incrementAndGet()
                gates[key]?.await(5, TimeUnit.SECONDS)
            }
            contacts.respond(uri, proj, sel, args)
        }
        k7 = contacts.add(7, "k7", "Rivka", Triple(12, JewishDate.NISSAN, 5750))
        k8 = contacts.add(8, "k8", "Sarah", Triple(3, JewishDate.TISHREI, 5740))
        contacts.add(2, "kB", "Batya")
    }

    private fun vm() = ContactBirthdayViewModel(
        ContactBirthdayRepository(app()), TodayProvider { LocalDate.of(2026, 1, 1) }
    )

    private fun queries(key: String) = storedQueries[key]?.get() ?: 0

    private fun settle() {
        repeat(3) { Thread.sleep(40); idleUntil { true } }
    }

    private fun load(vm: ContactBirthdayViewModel, key: String) {
        vm.onIntent(ContactBirthdayIntent.LoadBirthday(key))
        idleUntil(what = "load $key") { vm.state.value.isEditing && vm.state.value.contactLookupKey == key }
    }

    // ── Same record ─────────────────────────────────────────────────────

    @Test
    fun sameRecordRequestedAgainAfterCompletionKeepsLaterEdits() {
        val vm = vm()
        load(vm, "k7")
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(20))
        vm.onIntent(ContactBirthdayIntent.SetHebrewMonth(JewishDate.IYAR))
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        settle()
        assertEquals(20, vm.state.value.hebrewDay)
        assertEquals(JewishDate.IYAR, vm.state.value.hebrewMonth)
        assertEquals(5750, vm.state.value.hebrewYear)
        assertEquals("not re-queried", 1, queries("k7"))
    }

    @Test
    fun sameRecordRequestedAgainWhilePendingKeepsFieldEditsAndQueriesOnce() {
        gates["k7"] = CountDownLatch(1)
        val vm = vm()
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(provider, 1)
        vm.onIntent(ContactBirthdayIntent.SetHebrewYear(5760))
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        gates["k7"]!!.countDown()
        idleUntil(what = "load") { vm.state.value.isEditing }
        settle()
        assertEquals("stored day", 12, vm.state.value.hebrewDay)
        assertEquals("stored month", JewishDate.NISSAN, vm.state.value.hebrewMonth)
        assertEquals("edited year", 5760, vm.state.value.hebrewYear)
        assertEquals(1, queries("k7"))
    }

    // ── Newer contact choice vs stored load ─────────────────────────────

    @Test
    fun selectionAfterTheLoadHasResumedButBeforeItAppliesStillWins() {
        gates["k7"] = CountDownLatch(1)
        val vm = vm()
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(provider, 1)
        gates["k7"]!!.countDown()
        // Let the IO finish and queue its resumption on main without running it
        Thread.sleep(150)
        vm.onIntent(ContactBirthdayIntent.ContactSelected("kB", "Batya", null))
        settle()
        assertEquals("kB", vm.state.value.contactLookupKey)
        assertFalse(vm.state.value.isEditing)
    }

    @Test
    fun selectionThenRotationReRequestOfTheRouteRecordKeepsTheSelection() {
        gates["k7"] = CountDownLatch(1)
        val vm = vm()
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(provider, 1)
        vm.onIntent(ContactBirthdayIntent.ContactSelected("kB", "Batya", null))
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        gates["k7"]!!.countDown()
        settle()
        assertEquals("kB", vm.state.value.contactLookupKey)
        assertEquals("Batya", vm.state.value.contactName)
    }

    @Test
    fun selectionAfterCompletionReplacesTheContactAndKeepsTheDate() {
        val vm = vm()
        load(vm, "k7")
        vm.onIntent(ContactBirthdayIntent.ContactSelected("kB", "Batya", null))
        settle()
        assertEquals("kB", vm.state.value.contactLookupKey)
        assertEquals(12, vm.state.value.hebrewDay)
    }

    // ── Different record ────────────────────────────────────────────────

    @Test
    fun aDifferentRecordRequestedWhilePendingWinsOverTheSlowFirst() {
        gates["k7"] = CountDownLatch(1)
        val vm = vm()
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(provider, 1)
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k8"))
        idleUntil(what = "k8") { vm.state.value.contactLookupKey == "k8" }
        gates["k7"]!!.countDown()
        settle()
        assertEquals("k8", vm.state.value.contactLookupKey)
        assertEquals("Sarah", vm.state.value.contactName)
        assertEquals(3, vm.state.value.hebrewDay)
        assertEquals(JewishDate.TISHREI, vm.state.value.hebrewMonth)
        assertEquals(5740, vm.state.value.hebrewYear)
    }

    @Test
    fun aDifferentRecordAfterCompletionTakesItsOwnStoredDateNotEarlierEdits() {
        val vm = vm()
        load(vm, "k7")
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(20))
        load(vm, "k8")
        settle()
        assertEquals(3, vm.state.value.hebrewDay)
        assertEquals(JewishDate.TISHREI, vm.state.value.hebrewMonth)
        assertEquals(5740, vm.state.value.hebrewYear)
    }

    @Test
    fun returningToTheFirstRecordAfterAnotherLoadsItAgain() {
        val vm = vm()
        load(vm, "k7")
        load(vm, "k8")
        load(vm, "k7")
        assertEquals("Rivka", vm.state.value.contactName)
        assertEquals(2, queries("k7"))
    }

    // ── Save while a load is pending ────────────────────────────────────

    @Test
    fun saveWhileTheFirstLoadIsPendingDoesNothingAndTheLoadStillApplies() {
        gates["k7"] = CountDownLatch(1)
        val vm = vm()
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(provider, 1)
        vm.onIntent(ContactBirthdayIntent.Save)
        assertFalse(vm.state.value.isSaving)
        gates["k7"]!!.countDown()
        idleUntil(what = "load") { vm.state.value.isEditing }
        assertEquals(Triple(12, JewishDate.NISSAN, 5750), k7.birthday)
        assertFalse(vm.state.value.saveComplete)
    }

    @Test
    fun saveWhileADifferentRecordLoadsSavesWhatIsShownAndThePendingLoadNeverApplies() {
        val vm = vm()
        load(vm, "k7")
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(20))
        gates["k8"] = CountDownLatch(1)
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k8"))
        idleUntil(what = "k8 query") { queries("k8") == 1 }
        vm.onIntent(ContactBirthdayIntent.Save)
        idleUntil(what = "save") { vm.state.value.saveComplete }
        gates["k8"]!!.countDown()
        settle()
        assertEquals(Triple(20, JewishDate.NISSAN, 5750), k7.birthday)
        assertEquals(Triple(3, JewishDate.TISHREI, 5740), k8.birthday)
        assertEquals("k7", vm.state.value.contactLookupKey)
        assertEquals(20, vm.state.value.hebrewDay)
    }

    @Test
    fun aDifferentRecordLoadAfterAFailedSaveStillApplies() {
        val vm = vm()
        load(vm, "k7")
        provider.batchFailure = { IllegalStateException("boom") }
        vm.onIntent(ContactBirthdayIntent.Save)
        idleUntil(what = "fail") { vm.state.value.errorMessage != null }
        provider.batchFailure = null
        load(vm, "k8")
        // Observation only: the load copies the current state, so the old save
        // error stays. Not reachable in the app (one view model per route key).
        println("VERIFY-STALE-ERROR after load of k8: errorMessage=${vm.state.value.errorMessage} isSaving=${vm.state.value.isSaving}")
        assertFalse(vm.state.value.isSaving)
        assertTrue(vm.state.value.isEditing)
        assertEquals("Sarah", vm.state.value.contactName)
    }
}
