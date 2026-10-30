package ninja.doskey.app.levana.ui.birthday

import android.net.Uri
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.data.PickedContact
import ninja.doskey.app.levana.device.deviceContextOn
import ninja.doskey.app.levana.domain.model.ContactBirthday
import ninja.doskey.app.levana.domain.model.SupportedHebrewYears
import ninja.doskey.app.levana.ui.EditorDayClampCases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ninja.doskey.app.levana.ui.pickDay
import ninja.doskey.app.levana.ui.pickMonth
import ninja.doskey.app.levana.ui.pickYear

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ContactBirthdayViewModelTest {

    private fun viewModel(today: LocalDate = LocalDate.now()) = ContactBirthdayViewModel(
        ContactBirthdayRepository(RuntimeEnvironment.getApplication()),
        deviceContextOn(today)
    )

    @Test
    fun newBirthdayStartsOnTheCurrentHebrewYear() {
        val year = viewModel().state.value.hebrewYear
        assertEquals(HebrewDateMatcher.hebrewYearOf(LocalDate.now()), year)
        assertTrue(HebrewDateMatcher.isKnownYear(year))
    }

    @Test
    fun newBirthdayHonoursTheDateOverride() {
        // 1 March 2030 is in 5790; 1 October 2030 is already 5791
        assertEquals(5790, viewModel(LocalDate.of(2030, 3, 1)).state.value.hebrewYear)
        assertEquals(5791, viewModel(LocalDate.of(2030, 10, 1)).state.value.hebrewYear)
    }

    @Test
    fun setHebrewYearUpdatesState() {
        val vm = viewModel()
        vm.pickYear(5790)
        assertEquals(5790, vm.state.value.hebrewYear)
    }

    @Test
    fun enteredYearStaysInTheSupportedYears() {
        val vm = viewModel()
        for ((entered, held) in listOf(5600 to 5600, 6000 to 6000, 5599 to 5600, 6001 to 6000)) {
            vm.pickYear(entered)
            assertEquals("entered $entered", held, vm.state.value.hebrewYear)
        }
        for (bad in listOf(0, 1, -1, 3761, Int.MIN_VALUE)) {
            vm.pickYear(bad)
            assertEquals(SupportedHebrewYears.FIRST, vm.state.value.hebrewYear)
        }
        for (bad in listOf(10000, Int.MAX_VALUE)) {
            vm.pickYear(bad)
            assertEquals(SupportedHebrewYears.LAST, vm.state.value.hebrewYear)
        }
    }

    @Test
    fun adarIIBecomesAdarWhenTheYearBecomesRegular() {
        val vm = viewModel()
        vm.pickYear(5787)
        vm.pickMonth(JewishDate.ADAR_II)
        assertEquals(JewishDate.ADAR_II, vm.state.value.hebrewMonth)
        vm.pickYear(5788)
        assertEquals(JewishDate.ADAR, vm.state.value.hebrewMonth)
    }

    // ── Spec 1e: day clamp ──────────────────────────────────────────────────

    private fun editor(vm: ContactBirthdayViewModel = viewModel()) = object : EditorDayClampCases.Editor {
        override fun setDay(day: Int) = vm.pickDay(day)
        override fun setMonth(month: Int) = vm.pickMonth(month)
        override fun setYear(year: Int) = vm.pickYear(year)
        override val day get() = vm.state.value.hebrewDay
        override val month get() = vm.state.value.hebrewMonth
        override val year get() = vm.state.value.hebrewYear
    }

    @Test
    fun day30ThenA29DayMonthIs29() = EditorDayClampCases.switchingToA29DayMonthClampsTo29 { editor() }

    @Test
    fun yearChangeThatShortensTheMonthClamps() = EditorDayClampCases.yearChangeThatShortensTheMonthClamps { editor() }

    @Test
    fun adarI30ThenAnOrdinaryYear() = EditorDayClampCases.adarI30ThenAnOrdinaryYear { editor() }

    @Test
    fun steppingBackDoesNotRestore30() = EditorDayClampCases.steppingBackToA30DayMonthDoesNotRestore30 { editor() }

    @Test
    fun dayCanNeverExceedTheMonth() = EditorDayClampCases.dayCanNeverExceedTheMonth { editor() }

    /** A repository returning one stored birthday and recording what Save writes. */
    private class FakeRepository(private val stored: ContactBirthday) :
        ContactBirthdayRepository(RuntimeEnvironment.getApplication()) {
        var saved: Triple<Int, Int, Int>? = null
        override suspend fun getBirthdayForContact(contactLookupKey: String) = stored
        override suspend fun setBirthday(contactLookupKey: String, day: Int, month: Int, year: Int) {
            saved = Triple(day, month, year)
        }
    }

    private fun birthday(day: Int, month: Int, year: Int) = ContactBirthday(
        contactLookupKey = "key",
        contactName = "Name",
        contactPhotoUri = null,
        hebrewDay = day,
        hebrewMonth = month,
        hebrewYear = year
    )

    private fun open(stored: ContactBirthday): Pair<ContactBirthdayViewModel, FakeRepository> {
        val repository = FakeRepository(stored)
        val vm = ContactBirthdayViewModel(repository, deviceContextOn(LocalDate.of(2027, 3, 1)))
        vm.onIntent(ContactBirthdayIntent.LoadBirthday("key"))
        return vm to repository
    }

    @Test
    fun openingAStoredDay30ThatExistsKeepsIt() {
        val full = EditorDayClampCases.fullYear(JewishDate.KISLEV)
        val (vm, repository) = open(birthday(30, JewishDate.KISLEV, full))
        assertEquals(30, vm.state.value.hebrewDay)
        assertEquals(full, vm.state.value.hebrewYear)
        vm.onIntent(ContactBirthdayIntent.Save)
        assertEquals(Triple(30, JewishDate.KISLEV, full), repository.saved)
    }

    @Test
    fun openingAStoredDay30ThatDoesNotExistShows29AndSaves29() {
        val short = EditorDayClampCases.shortYear(JewishDate.KISLEV)
        val (vm, repository) = open(birthday(30, JewishDate.KISLEV, short))
        assertEquals(29, vm.state.value.hebrewDay)
        assertEquals(null, repository.saved)
        vm.onIntent(ContactBirthdayIntent.Save)
        assertEquals(Triple(29, JewishDate.KISLEV, short), repository.saved)
    }

    @Test
    fun openingAnUnknownYearDay30KislevKeepsDay30() {
        val (vm, _) = open(birthday(30, JewishDate.KISLEV, 0))
        assertEquals(30, vm.state.value.hebrewDay)
        assertEquals(30, EditorDayClampCases.days(vm.state.value.hebrewYear, JewishDate.KISLEV))
    }

    // ── TASK-24: out-of-range stored dates ──────────────────────────────────

    private val ordinaryYearBelowRange = (5501..5599).first { !HebrewDateMatcher.isEventLeapYear(it) }

    /** Stored dates (day, month, year) whose year is outside 5600..6000. */
    private val outOfRange = listOf(
        Triple(14, JewishDate.KISLEV, 5500),
        Triple(14, JewishDate.KISLEV, 6100),
        Triple(14, JewishDate.KISLEV, 5599),
        Triple(14, JewishDate.KISLEV, 6001),
        Triple(30, JewishDate.IYAR, 5500),
        Triple(14, JewishDate.ADAR_II, ordinaryYearBelowRange),
        Triple(14, JewishDate.ADAR, 0),
        Triple(30, JewishDate.KISLEV, 0),
        Triple(10, JewishDate.KISLEV, 1),
        Triple(10, JewishDate.KISLEV, -1),
        Triple(10, JewishDate.KISLEV, 99999)
    )

    private fun birthday(date: Triple<Int, Int, Int>) = birthday(date.first, date.second, date.third)

    @Test
    fun outOfRangeStoredBirthdayOpensAndSavesUnchanged() {
        for (date in outOfRange) {
            val (vm, repository) = open(birthday(date))
            if (HebrewDateMatcher.isKnownYear(date.third)) {
                assertEquals("$date", date.third, vm.state.value.hebrewYear)
            }
            assertEquals(null, repository.saved)
            vm.onIntent(ContactBirthdayIntent.Save)
            assertEquals("$date", date, repository.saved)
        }
    }

    @Test
    fun confirmingThePickerOnTheShownDateKeepsAnOutOfRangeBirthday() {
        for (date in outOfRange) {
            val (vm, repository) = open(birthday(date))
            vm.onIntent(ContactBirthdayIntent.OpenDatePicker)
            assertTrue(vm.state.value.showDatePicker)
            // The picker opens on the shown date; OK without a new pick returns it (TASK-45)
            vm.onIntent(ContactBirthdayIntent.SetHebrewDate(vm.state.value.gregorianDate))
            assertFalse(vm.state.value.showDatePicker)
            vm.onIntent(ContactBirthdayIntent.Save)
            assertEquals("$date", date, repository.saved)
        }
    }

    @Test
    fun pickingAnotherDayInAnOutOfRangeYearSavesItInThatYear() {
        val (vm, repository) = open(birthday(12, JewishDate.NISSAN, 5500))
        vm.onIntent(ContactBirthdayIntent.SetHebrewDate(vm.state.value.gregorianDate.plusDays(1)))
        vm.onIntent(ContactBirthdayIntent.Save)
        assertEquals(Triple(13, JewishDate.NISSAN, 5500), repository.saved)
    }

    @Test
    fun changingTheDayKeepsAStoredYearOutsideTheRange() {
        for (year in listOf(5500, 6100)) {
            val (vm, repository) = open(birthday(14, JewishDate.KISLEV, year))
            vm.pickDay(15)
            vm.onIntent(ContactBirthdayIntent.Save)
            assertEquals(Triple(15, JewishDate.KISLEV, year), repository.saved)
        }
    }

    @Test
    fun steppingTheYearOfAnOutOfRangeBirthdayEntersTheRange() {
        for ((year, stepped, expected) in listOf(Triple(5500, 5501, 5600), Triple(6100, 6099, 6000))) {
            val (vm, repository) = open(birthday(14, JewishDate.KISLEV, year))
            vm.pickYear(stepped)
            assertEquals(expected, vm.state.value.hebrewYear)
            vm.onIntent(ContactBirthdayIntent.Save)
            assertEquals(Triple(14, JewishDate.KISLEV, expected), repository.saved)
        }
    }

    @Test
    fun storedBirthdaysAtTheBoundsAreNormalisedLikeAnyInRangeDate() {
        for (year in listOf(SupportedHebrewYears.FIRST, SupportedHebrewYears.LAST)) {
            val (vm, repository) = open(birthday(30, JewishDate.IYAR, year))
            assertEquals(year, vm.state.value.hebrewYear)
            vm.onIntent(ContactBirthdayIntent.Save)
            assertEquals(Triple(29, JewishDate.IYAR, year), repository.saved)
        }
    }

    // ── TASK-51: picking a contact that already has a birthday ──────────────

    /** Contacts by lookup key, each with an optional stored birthday; records what Save writes. */
    private class ContactsRepository(private val stored: Map<String, ContactBirthday?>) :
        ContactBirthdayRepository(RuntimeEnvironment.getApplication()) {
        var saved: Pair<String, Triple<Int, Int, Int>>? = null
        override suspend fun resolveContact(contactUri: Uri) =
            contactUri.lastPathSegment!!.let { PickedContact(it, "Contact $it", null) }
        override suspend fun getBirthdayForContact(contactLookupKey: String) = stored[contactLookupKey]
        override suspend fun setBirthday(contactLookupKey: String, day: Int, month: Int, year: Int) {
            saved = contactLookupKey to Triple(day, month, year)
        }
    }

    private fun addMode(vararg stored: Pair<String, ContactBirthday?>): Pair<ContactBirthdayViewModel, ContactsRepository> {
        val repository = ContactsRepository(stored.toMap())
        return ContactBirthdayViewModel(repository, deviceContextOn(LocalDate.of(2027, 3, 1))) to repository
    }

    private fun ContactBirthdayViewModel.pick(key: String) =
        onIntent(ContactBirthdayIntent.ContactPicked(Uri.parse("content://com.android.contacts/contacts/lookup/$key")))

    private val ContactBirthdayViewModel.date get() = state.value.let { Triple(it.hebrewDay, it.hebrewMonth, it.hebrewYear) }

    private fun stored(key: String, day: Int, month: Int, year: Int) =
        ContactBirthday(key, "Contact $key", null, day, month, year)

    @Test
    fun pickingAContactWithABirthdayEditsItWithANotice() {
        val (vm, repository) = addMode("kR" to stored("kR", 12, JewishDate.SIVAN, 5750))
        val addDate = vm.date
        assertFalse(vm.state.value.isEditing)

        vm.pick("kR")

        assertTrue(vm.state.value.isEditing)
        assertEquals("kR", vm.state.value.contactLookupKey)
        assertEquals(Triple(12, JewishDate.SIVAN, 5750), vm.date)
        assertEquals(StoredBirthdayNotice(12, JewishDate.SIVAN, 5750, replacedChosenDate = false), vm.state.value.storedBirthdayNotice)
        assertFalse(vm.state.value.storedBirthdayUnchecked)
        assertTrue(addDate != vm.date)
        vm.onIntent(ContactBirthdayIntent.Save)
        assertEquals("kR" to Triple(12, JewishDate.SIVAN, 5750), repository.saved)
    }

    @Test
    fun selectingAContactWithABirthdayEditsItToo() {
        val (vm, _) = addMode("kR" to stored("kR", 12, JewishDate.SIVAN, 5750))
        vm.onIntent(ContactBirthdayIntent.ContactSelected("kR", "Contact kR", null))
        assertTrue(vm.state.value.isEditing)
        assertEquals(Triple(12, JewishDate.SIVAN, 5750), vm.date)
    }

    @Test
    fun pickingAContactWithoutABirthdayStaysInAddMode() {
        val (vm, repository) = addMode("kN" to null)
        val addDate = vm.date

        vm.pick("kN")

        assertFalse(vm.state.value.isEditing)
        assertEquals(null, vm.state.value.storedBirthdayNotice)
        assertFalse(vm.state.value.storedBirthdayUnchecked)
        assertEquals("kN", vm.state.value.contactLookupKey)
        assertEquals(addDate, vm.date)
        vm.onIntent(ContactBirthdayIntent.Save)
        assertEquals("kN" to addDate, repository.saved)
    }

    @Test
    fun switchingContactsSwitchesTheModeAndNeverCarriesADateAcross() {
        val (vm, repository) = addMode(
            "kA" to stored("kA", 12, JewishDate.SIVAN, 5750),
            "kN" to null,
            "kC" to stored("kC", 5, JewishDate.KISLEV, 5760)
        )
        val addDate = vm.date

        vm.pick("kA")
        assertTrue(vm.state.value.isEditing)
        // An edit to A's birthday belongs to A
        vm.pickDay(20)

        vm.pick("kN")
        assertFalse(vm.state.value.isEditing)
        assertEquals(null, vm.state.value.storedBirthdayNotice)
        assertEquals(addDate, vm.date)

        vm.pick("kC")
        assertTrue(vm.state.value.isEditing)
        assertEquals(Triple(5, JewishDate.KISLEV, 5760), vm.date)
        assertEquals(StoredBirthdayNotice(5, JewishDate.KISLEV, 5760, replacedChosenDate = false), vm.state.value.storedBirthdayNotice)

        vm.pick("kA")
        assertEquals(Triple(12, JewishDate.SIVAN, 5750), vm.date)
        vm.onIntent(ContactBirthdayIntent.Save)
        assertEquals("kA" to Triple(12, JewishDate.SIVAN, 5750), repository.saved)
    }

    @Test
    fun aDateChosenBeforeThePickIsReplacedSaidAndRestoredForAContactWithout() {
        val (vm, _) = addMode("kR" to stored("kR", 12, JewishDate.SIVAN, 5750), "kN" to null)
        vm.pickYear(5790)
        vm.pickMonth(JewishDate.AV)
        vm.pickDay(9)
        val chosen = vm.date

        vm.pick("kR")
        assertEquals(Triple(12, JewishDate.SIVAN, 5750), vm.date)
        assertTrue(vm.state.value.storedBirthdayNotice!!.replacedChosenDate)

        vm.pick("kN")
        assertEquals(chosen, vm.date)
        assertFalse(vm.state.value.isEditing)
    }

    @Test
    fun aChosenDateEqualToTheStoredOneIsNotReportedAsReplaced() {
        val (vm, _) = addMode("kR" to stored("kR", 12, JewishDate.SIVAN, 5750))
        vm.pickYear(5750)
        vm.pickMonth(JewishDate.SIVAN)
        vm.pickDay(12)
        vm.pick("kR")
        assertFalse(vm.state.value.storedBirthdayNotice!!.replacedChosenDate)
    }

    @Test
    fun anOutOfRangeBirthdayOfAPickedContactIsSavedUnchanged() {
        for (date in outOfRange) {
            val (vm, repository) = addMode("kR" to stored("kR", date.first, date.second, date.third))
            vm.pickDay(3) // a change before the pick does not touch the stored date
            vm.pick("kR")
            assertTrue("$date", vm.state.value.isEditing)
            if (HebrewDateMatcher.isKnownYear(date.third)) {
                assertEquals("$date", date.third, vm.state.value.hebrewYear)
            }
            assertEquals("$date", date, vm.state.value.storedBirthdayNotice!!.let { Triple(it.day, it.month, it.year) })
            vm.onIntent(ContactBirthdayIntent.Save)
            assertEquals("$date", "kR" to date, repository.saved)
        }
    }

    @Test
    fun anOutOfRangeBirthdayIsNotCarriedToAContactWithout() {
        val (vm, repository) = addMode("kR" to stored("kR", 14, JewishDate.KISLEV, 5500), "kN" to null)
        val addDate = vm.date
        vm.pick("kR")
        vm.pick("kN")
        vm.onIntent(ContactBirthdayIntent.Save)
        assertEquals("kN" to addDate, repository.saved)
    }
}
