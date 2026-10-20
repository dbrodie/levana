package ninja.doskey.app.levana.verifyio

import android.net.Uri
import android.provider.ContactsContract
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.TodayProvider
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayIntent
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Required: a later contact selection wins over any earlier pending lookup;
 * date fields edited while the stored birthday loads keep the user's values,
 * untouched fields take the stored values.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class VerifyBirthdayEditorRaceTest {

    private val contacts = ContactsWorld()
    private lateinit var provider: ScriptedProvider
    private val slowStoredLoad = CountDownLatch(1)

    @Before
    fun setUp() {
        grantAll()
        provider = installContacts(contacts)
        // Only the stored-birthday query for k7 is slow; everything else answers at once
        provider.responder = { uri, proj, sel, args ->
            if (uri == ContactsContract.Data.CONTENT_URI && args?.getOrNull(1) == "k7") {
                slowStoredLoad.await(5, TimeUnit.SECONDS)
            }
            contacts.respond(uri, proj, sel, args)
        }
        contacts.add(7, "k7", "Rivka", Triple(12, JewishDate.NISSAN, 5750))
        contacts.add(2, "kB", "Batya")
    }

    private fun vm() = ContactBirthdayViewModel(
        ContactBirthdayRepository(app()), TodayProvider { LocalDate.of(2026, 1, 1) }
    )

    private fun finishLoad(vm: ContactBirthdayViewModel, done: () -> Boolean) {
        slowStoredLoad.countDown()
        Thread.sleep(50)
        idleUntil(what = "load settles") { done() }
        Thread.sleep(50)
        idleUntil { true }
    }

    @Test
    fun contactPickedWhileTheStoredBirthdayLoadsWins() {
        val vm = vm()
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(provider, 1)
        vm.onIntent(ContactBirthdayIntent.ContactPicked(Uri.parse("content://com.android.contacts/contacts/2")))
        idleUntil(what = "pick") { vm.state.value.contactName == "Batya" }
        finishLoad(vm) { true }
        assertEquals("later pick wins over pending stored load", "Batya", vm.state.value.contactName)
        assertEquals("kB", vm.state.value.contactLookupKey)
    }

    @Test
    fun contactSelectedWhileTheStoredBirthdayLoadsWins() {
        val vm = vm()
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(provider, 1)
        vm.onIntent(ContactBirthdayIntent.ContactSelected("kB", "Batya", null))
        finishLoad(vm) { true }
        assertEquals("kB", vm.state.value.contactLookupKey)
    }

    @Test
    fun onlyTheEditedMonthIsKeptOtherFieldsComeFromStorage() {
        val vm = vm()
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(provider, 1)
        vm.onIntent(ContactBirthdayIntent.SetHebrewMonth(JewishDate.IYAR))
        finishLoad(vm) { vm.state.value.isEditing }
        assertEquals(12, vm.state.value.hebrewDay)
        assertEquals(JewishDate.IYAR, vm.state.value.hebrewMonth)
        assertEquals(5750, vm.state.value.hebrewYear)
        assertEquals("Rivka", vm.state.value.contactName)
    }

    @Test
    fun editedDayAndYearAreKeptAndSaved() {
        val c = contacts.contacts.first { it.lookupKey == "k7" }
        val vm = vm()
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(provider, 1)
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(20))
        vm.onIntent(ContactBirthdayIntent.SetHebrewYear(5760))
        finishLoad(vm) { vm.state.value.isEditing }
        assertEquals(20, vm.state.value.hebrewDay)
        assertEquals(JewishDate.NISSAN, vm.state.value.hebrewMonth)
        assertEquals(5760, vm.state.value.hebrewYear)
        vm.onIntent(ContactBirthdayIntent.Save)
        idleUntil(what = "save") { vm.state.value.saveComplete }
        assertEquals(Triple(20, JewishDate.NISSAN, 5760), c.birthday)
    }

    @Test
    fun editMadeBeforeARepeatedLoadRequestIsStillKept() {
        // Rotation re-runs the screen's LaunchedEffect, issuing LoadBirthday again
        val vm = vm()
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        awaitEntered(provider, 1)
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(20))
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        finishLoad(vm) { vm.state.value.isEditing }
        assertEquals("edit made during the (still pending) load", 20, vm.state.value.hebrewDay)
    }

    @Test
    fun anUntouchedEditorTakesEveryStoredValue() {
        // An untouched editor that loads after completion uses stored values
        val vm = vm()
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        finishLoad(vm) { vm.state.value.isEditing }
        assertEquals(12, vm.state.value.hebrewDay)
        assertEquals(JewishDate.NISSAN, vm.state.value.hebrewMonth)
        assertTrue(vm.state.value.isEditing)
    }
}
