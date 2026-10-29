package ninja.doskey.app.levana.verifyio

import android.provider.ContactsContract
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.data.SystemCalendarRepository
import ninja.doskey.app.levana.device.deviceContextOn
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.CalendarEvent
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayIntent
import ninja.doskey.app.levana.ui.birthday.ContactBirthdayViewModel
import ninja.doskey.app.levana.ui.calendarselection.CalendarSelectionViewModel
import ninja.doskey.app.levana.ui.daydetail.DayDetailIntent
import ninja.doskey.app.levana.ui.daydetail.DayDetailViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * View-model level checks that only use APIs present before the fix, so the
 * same file also runs against the baseline to show the old behaviour fails.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class VerifyProviderMainThreadTest {

    private val contacts = ContactsWorld()
    private val calendar = CalendarWorld()
    private lateinit var contactsProvider: ScriptedProvider
    private lateinit var calendarProvider: ScriptedProvider

    @Before
    fun setUp() {
        grantAll()
        contactsProvider = installContacts(contacts)
        calendarProvider = installCalendar(calendar)
    }

    private fun prefs() = PreferencesRepository(app(), CityRepository(app()))

    private fun assertAllOffMain(p: ScriptedProvider) {
        assertTrue("provider never called", p.calls.isNotEmpty())
        p.calls.forEach {
            assertNotEquals("${it.kind} ${it.uri} ran on main", mainThread, it.thread)
        }
    }

    private fun timeMs(block: () -> Unit): Long {
        val s = System.nanoTime(); block(); return (System.nanoTime() - s) / 1_000_000
    }

    @Test
    fun calendarSelectionLoadsOffMainWhileMainStaysResponsive() {
        calendar.calendars += CalendarWorld.Cal(2, "Work", "a@b", 0xFF0000)
        calendar.calendars += CalendarWorld.Cal(1, "Home", "a@b", 0x00FF00)
        val gate = CountDownLatch(1)
        calendarProvider.gate = gate

        lateinit var vm: CalendarSelectionViewModel
        val ms = timeMs { vm = CalendarSelectionViewModel(SystemCalendarRepository(app()), prefs()) }
        assertTrue("constructor blocked ${ms}ms", ms < 1_000)

        awaitEntered(calendarProvider, 1)
        assertTrue(vm.state.value.isLoading)
        assertMainResponsive()
        assertTrue("still loading while provider blocked", vm.state.value.isLoading)

        gate.countDown()
        idleUntil(what = "calendars") { !vm.state.value.isLoading }
        assertTrue(vm.state.value.hasPermission)
        assertEquals(setOf("Work", "Home"), vm.state.value.calendars.map { it.name }.toSet())
        assertAllOffMain(calendarProvider)
    }

    @Test
    fun contactBirthdayLoadRunsOffMain() {
        contacts.add(7, "k7", "Rivka", Triple(12, 3, 5750))
        contactsProvider.gate = CountDownLatch(1)
        val vm = ContactBirthdayViewModel(ContactBirthdayRepository(app()), deviceContextOn(LocalDate.of(2026, 1, 1)))

        val ms = timeMs { vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7")) }
        assertTrue("LoadBirthday blocked ${ms}ms", ms < 1_000)
        awaitEntered(contactsProvider, 1)
        assertMainResponsive()
        assertFalse(vm.state.value.isEditing)

        contactsProvider.gate!!.countDown()
        idleUntil(what = "birthday load") { vm.state.value.isEditing }
        assertEquals("Rivka", vm.state.value.contactName)
        assertEquals(12, vm.state.value.hebrewDay)
        assertEquals(3, vm.state.value.hebrewMonth)
        assertEquals(5750, vm.state.value.hebrewYear)
        assertAllOffMain(contactsProvider)
    }

    @Test
    fun editingABirthdaySavesOffMainAndUpdatesTheProvider() {
        val c = contacts.add(7, "k7", "Rivka", Triple(12, 3, 5750))
        val vm = ContactBirthdayViewModel(ContactBirthdayRepository(app()), deviceContextOn(LocalDate.of(2026, 1, 1)))
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("k7"))
        idleUntil(what = "load") { vm.state.value.isEditing }
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(14))

        contactsProvider.gate = CountDownLatch(1)
        val before = contactsProvider.entered.get()
        val ms = timeMs { vm.onIntent(ContactBirthdayIntent.Save) }
        assertTrue("Save blocked ${ms}ms", ms < 1_000)
        awaitEntered(contactsProvider, before + 1)
        assertTrue("isSaving while provider blocked", vm.state.value.isSaving)
        assertFalse(vm.state.value.saveComplete)
        assertMainResponsive()

        contactsProvider.gate!!.countDown()
        idleUntil(what = "save") { vm.state.value.saveComplete }
        assertFalse(vm.state.value.isSaving)
        assertEquals(Triple(14, 3, 5750), c.birthday)
        assertTrue(contactsProvider.calls.any { it.kind == "batch" })
        assertAllOffMain(contactsProvider)
    }

    @Test
    fun addingABirthdayToANewContactInsertsOffMain() {
        val c = contacts.add(8, "k8", "Moshe")
        val vm = ContactBirthdayViewModel(ContactBirthdayRepository(app()), deviceContextOn(LocalDate.of(2026, 1, 1)))
        vm.onIntent(ContactBirthdayIntent.ContactSelected("k8", "Moshe", null))
        vm.onIntent(ContactBirthdayIntent.SetHebrewYear(5760))
        vm.onIntent(ContactBirthdayIntent.SetHebrewMonth(1))
        vm.onIntent(ContactBirthdayIntent.SetHebrewDay(5))
        vm.onIntent(ContactBirthdayIntent.Save)
        idleUntil(what = "save") { vm.state.value.saveComplete }
        assertEquals(Triple(5, 1, 5760), c.birthday)
        assertAllOffMain(contactsProvider)
    }

    @Test
    fun saveFailureIsReportedNotCrashing() {
        contacts.add(7, "k7", "Rivka", Triple(12, 3, 5750))
        contactsProvider.batchFailure = { IllegalStateException("provider exploded") }
        val vm = ContactBirthdayViewModel(ContactBirthdayRepository(app()), deviceContextOn(LocalDate.of(2026, 1, 1)))
        vm.onIntent(ContactBirthdayIntent.ContactSelected("k7", "Rivka", null))
        vm.onIntent(ContactBirthdayIntent.Save)
        idleUntil(what = "save failure") { !vm.state.value.isSaving }
        assertFalse(vm.state.value.saveComplete)
        assertEquals("provider exploded", vm.state.value.errorMessage)
        assertAllOffMain(contactsProvider)
    }

    @Test
    fun dayDetailReadsBirthdaysAndSystemEventsOffMain() {
        val date = LocalDate.of(2026, 3, 10)
        val jd = com.kosherjava.zmanim.hebrewcalendar.JewishDate(java.util.GregorianCalendar(2026, 2, 10))
        contacts.add(7, "k7", "Rivka", Triple(jd.jewishDayOfMonth, jd.jewishMonth, 5750))
        val begin = date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        calendar.instances += CalendarWorld.Instance(1, "Dentist", begin, begin + 3_600_000, false, 1, 0xFF)
        val prefs = prefs()
        runBlocking { prefs.saveSelectedCalendarIds(setOf(1L)) }
        contactsProvider.gate = CountDownLatch(1)
        calendarProvider.gate = CountDownLatch(1)

        val vm = DayDetailViewModel(
            CalendarRepository(), ZmanimRepository(), prefs,
            PersonalEventRepository(FakeDao()),
            ContactBirthdayRepository(app()), SystemCalendarRepository(app()),
            object : ninja.doskey.app.levana.data.EventDataChanges {
                override val changes = kotlinx.coroutines.flow.emptyFlow<Unit>()
                override fun recheckPermissions() = Unit
            }
        )
        val ms = timeMs { vm.onIntent(DayDetailIntent.LoadDay(date)) }
        assertTrue("LoadDay blocked ${ms}ms", ms < 1_000)
        awaitEntered(contactsProvider, 1)
        assertMainResponsive()
        contactsProvider.gate!!.countDown()
        awaitEntered(calendarProvider, 1)
        assertMainResponsive()
        calendarProvider.gate!!.countDown()

        idleUntil(what = "day detail") { !vm.state.value.isLoading && vm.state.value.dayInfo != null }
        assertEquals(listOf("Dentist"), vm.state.value.systemEvents.map { it.title })
        assertEquals(1, vm.state.value.calendarEvents.filterIsInstance<CalendarEvent.Birthday>().size)
        assertAllOffMain(contactsProvider)
        assertAllOffMain(calendarProvider)
        // The contact data query passes a CancellationSignal through
        assertTrue(contactsProvider.calls.first { it.uri == ContactsContract.Data.CONTENT_URI }.hadSignal)
    }
}
