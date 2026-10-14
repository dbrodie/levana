package ninja.doskey.app.levana.ui.birthday

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.data.TodayProvider
import ninja.doskey.app.levana.domain.model.ContactBirthday
import ninja.doskey.app.levana.ui.EditorDayClampCases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ContactBirthdayViewModelTest {

    private fun viewModel(today: LocalDate = LocalDate.now()) = ContactBirthdayViewModel(
        ContactBirthdayRepository(RuntimeEnvironment.getApplication()),
        TodayProvider { today }
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
        vm.onIntent(ContactBirthdayIntent.SetHebrewYear(5790))
        assertEquals(5790, vm.state.value.hebrewYear)
    }

    @Test
    fun yearCanNeverLeaveTheRangeTheCalendarHandles() {
        val vm = viewModel()
        for (bad in listOf(0, 1, -1, 3761, Int.MIN_VALUE)) {
            vm.onIntent(ContactBirthdayIntent.SetHebrewYear(bad))
            assertEquals(HebrewDateMatcher.MIN_KNOWN_YEAR, vm.state.value.hebrewYear)
        }
        for (bad in listOf(10000, Int.MAX_VALUE)) {
            vm.onIntent(ContactBirthdayIntent.SetHebrewYear(bad))
            assertEquals(HebrewDateMatcher.MAX_KNOWN_YEAR, vm.state.value.hebrewYear)
        }
    }

    @Test
    fun adarIIBecomesAdarWhenTheYearBecomesRegular() {
        val vm = viewModel()
        vm.onIntent(ContactBirthdayIntent.SetHebrewYear(5787))
        vm.onIntent(ContactBirthdayIntent.SetHebrewMonth(JewishDate.ADAR_II))
        assertEquals(JewishDate.ADAR_II, vm.state.value.hebrewMonth)
        vm.onIntent(ContactBirthdayIntent.SetHebrewYear(5788))
        assertEquals(JewishDate.ADAR, vm.state.value.hebrewMonth)
    }

    // ── Spec 1e: day clamp ──────────────────────────────────────────────────

    private fun editor(vm: ContactBirthdayViewModel = viewModel()) = object : EditorDayClampCases.Editor {
        override fun setDay(day: Int) = vm.onIntent(ContactBirthdayIntent.SetHebrewDay(day))
        override fun setMonth(month: Int) = vm.onIntent(ContactBirthdayIntent.SetHebrewMonth(month))
        override fun setYear(year: Int) = vm.onIntent(ContactBirthdayIntent.SetHebrewYear(year))
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
        override fun getBirthdayForContact(contactLookupKey: String) = stored
        override fun setBirthday(contactLookupKey: String, day: Int, month: Int, year: Int) {
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
        val vm = ContactBirthdayViewModel(repository, TodayProvider { LocalDate.of(2027, 3, 1) })
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
}
