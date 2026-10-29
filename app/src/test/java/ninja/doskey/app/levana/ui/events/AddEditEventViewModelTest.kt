package ninja.doskey.app.levana.ui.events

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.device.deviceContextOn
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
import ninja.doskey.app.levana.ui.EditorDayClampCases
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class AddEditEventViewModelTest {

    private class FakeDao(initial: List<PersonalEvent> = emptyList()) : PersonalEventDao {
        val events = initial.toMutableList()
        override fun getAll(): Flow<List<PersonalEvent>> = flowOf(events)
        override suspend fun getAllOnce(): List<PersonalEvent> = events
        override suspend fun getById(id: Long): PersonalEvent? = events.firstOrNull { it.id == id }
        override suspend fun getByHebrewDate(month: Int, day: Int): List<PersonalEvent> = emptyList()
        override suspend fun insert(event: PersonalEvent): Long {
            events.add(event)
            return events.size.toLong()
        }
        override suspend fun update(event: PersonalEvent) {
            events.replaceAll { if (it.id == event.id) event else it }
        }
        override suspend fun delete(event: PersonalEvent) = Unit
    }

    // 1 March 2027 is in leap year 5787; 1 March 2026 is in regular year 5786
    private val leapToday = LocalDate.of(2027, 3, 1)
    private val regularToday = LocalDate.of(2026, 3, 1)

    private fun viewModel(dao: FakeDao = FakeDao(), today: LocalDate = LocalDate.now()) =
        AddEditEventViewModel(PersonalEventRepository(dao), deviceContextOn(today))

    private fun stored(month: Int, year: Int, yahrzeit: Boolean = false) = PersonalEvent(
        id = 1,
        title = "Stored",
        hebrewDay = 14,
        hebrewMonth = month,
        hebrewYear = year,
        useYahrzeitRules = yahrzeit
    )

    @Test
    fun newEventStartsOnTheCurrentHebrewYear() {
        assertEquals(
            HebrewDateMatcher.hebrewYearOf(LocalDate.now()),
            viewModel().state.value.hebrewYear
        )
    }

    @Test
    fun newEventHonoursTheDateOverride() {
        assertEquals(5787, viewModel(today = leapToday).state.value.hebrewYear)
        assertEquals(5786, viewModel(today = regularToday).state.value.hebrewYear)
    }

    @Test
    fun prefilledDateWinsOverTheDefaultYear() {
        val vm = viewModel(today = leapToday)
        vm.onIntent(AddEditEventIntent.PreFillDate(3, JewishDate.KISLEV, 5790))
        assertEquals(5790, vm.state.value.hebrewYear)
        assertEquals(JewishDate.KISLEV, vm.state.value.hebrewMonth)
        assertEquals(3, vm.state.value.hebrewDay)
    }

    @Test
    fun storedKnownYearIsShownAsStored() {
        val vm = viewModel(FakeDao(listOf(stored(JewishDate.ADAR, 5784))), leapToday)
        vm.onIntent(AddEditEventIntent.LoadEvent(1))
        assertEquals(5784, vm.state.value.hebrewYear)
        assertEquals(JewishDate.ADAR, vm.state.value.hebrewMonth)
    }

    @Test
    fun storedUnknownYearOpensOnTheCurrentYear() {
        for (unknown in listOf(0, 1, -1, 99999)) {
            val vm = viewModel(FakeDao(listOf(stored(JewishDate.KISLEV, unknown))), leapToday)
            vm.onIntent(AddEditEventIntent.LoadEvent(1))
            assertEquals(5787, vm.state.value.hebrewYear)
            assertEquals(JewishDate.KISLEV, vm.state.value.hebrewMonth)
        }
    }

    @Test
    fun unknownYearAdarOpensWhereItFallsThisYear() {
        // Leap current year: Adar II, so saving unchanged does not move it to Adar I
        val leap = viewModel(FakeDao(listOf(stored(JewishDate.ADAR, 0))), leapToday)
        leap.onIntent(AddEditEventIntent.LoadEvent(1))
        assertEquals(JewishDate.ADAR_II, leap.state.value.hebrewMonth)

        // Regular current year: Adar, also for a stored Adar II
        val regular = viewModel(FakeDao(listOf(stored(JewishDate.ADAR_II, 0))), regularToday)
        regular.onIntent(AddEditEventIntent.LoadEvent(1))
        assertEquals(JewishDate.ADAR, regular.state.value.hebrewMonth)
        assertEquals(5786, regular.state.value.hebrewYear)
    }

    @Test
    fun savingAnUnknownYearAdarEventKeepsItInAdarII() {
        val dao = FakeDao(listOf(stored(JewishDate.ADAR, 0)))
        val vm = viewModel(dao, leapToday)
        vm.onIntent(AddEditEventIntent.LoadEvent(1))
        vm.onIntent(AddEditEventIntent.Save)

        val saved = dao.events.single()
        for (target in 5780..5800) {
            assertEquals(
                HebrewDateMatcher.occurrences(14, JewishDate.ADAR, 0, false, target),
                HebrewDateMatcher.occurrences(saved.hebrewDay, saved.hebrewMonth, saved.hebrewYear, false, target)
            )
        }
    }

    @Test
    fun yearCanNeverLeaveTheRangeTheCalendarHandles() {
        val vm = viewModel()
        vm.onIntent(AddEditEventIntent.SetHebrewYear(-5))
        assertEquals(HebrewDateMatcher.MIN_KNOWN_YEAR, vm.state.value.hebrewYear)
        vm.onIntent(AddEditEventIntent.SetHebrewYear(123456))
        assertEquals(HebrewDateMatcher.MAX_KNOWN_YEAR, vm.state.value.hebrewYear)
        vm.onIntent(AddEditEventIntent.PreFillDate(1, JewishDate.TISHREI, 1))
        assertEquals(HebrewDateMatcher.MIN_KNOWN_YEAR, vm.state.value.hebrewYear)
    }

    @Test
    fun adarIIBecomesAdarWhenTheYearBecomesRegular() {
        val vm = viewModel(today = leapToday)
        vm.onIntent(AddEditEventIntent.SetHebrewMonth(JewishDate.ADAR_II))
        vm.onIntent(AddEditEventIntent.SetHebrewYear(5788))
        assertEquals(JewishDate.ADAR, vm.state.value.hebrewMonth)
    }

    // ── Spec 1e: day clamp ──────────────────────────────────────────────────

    private fun editor(vm: AddEditEventViewModel = viewModel()) = object : EditorDayClampCases.Editor {
        override fun setDay(day: Int) = vm.onIntent(AddEditEventIntent.SetHebrewDay(day))
        override fun setMonth(month: Int) = vm.onIntent(AddEditEventIntent.SetHebrewMonth(month))
        override fun setYear(year: Int) = vm.onIntent(AddEditEventIntent.SetHebrewYear(year))
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

    @Test
    fun savedDayIsTheShownDay() {
        val dao = FakeDao()
        val vm = viewModel(dao)
        vm.onIntent(AddEditEventIntent.SetTitle("New"))
        vm.onIntent(AddEditEventIntent.SetHebrewYear(EditorDayClampCases.ordinaryYear))
        vm.onIntent(AddEditEventIntent.SetHebrewDay(30))
        vm.onIntent(AddEditEventIntent.SetHebrewMonth(JewishDate.IYAR))
        vm.onIntent(AddEditEventIntent.Save)
        assertEquals(29, dao.events.single().hebrewDay)
    }

    @Test
    fun prefilledDayIsClamped() {
        val vm = viewModel()
        vm.onIntent(AddEditEventIntent.PreFillDate(30, JewishDate.IYAR, 5785))
        assertEquals(29, vm.state.value.hebrewDay)
    }

    private fun storedDay30(month: Int, year: Int) = stored(month, year).copy(hebrewDay = 30)

    @Test
    fun openingAStoredDay30ThatExistsKeepsIt() {
        val full = EditorDayClampCases.fullYear(JewishDate.CHESHVAN)
        val dao = FakeDao(listOf(storedDay30(JewishDate.CHESHVAN, full)))
        val vm = viewModel(dao, leapToday)
        vm.onIntent(AddEditEventIntent.LoadEvent(1))
        assertEquals(30, vm.state.value.hebrewDay)
        assertEquals(full, vm.state.value.hebrewYear)
        vm.onIntent(AddEditEventIntent.Save)
        assertEquals(30, dao.events.single().hebrewDay)

        // 30 Adar I of a leap year
        val leap = EditorDayClampCases.leapYear
        val vm2 = viewModel(FakeDao(listOf(storedDay30(JewishDate.ADAR, leap))), leapToday)
        vm2.onIntent(AddEditEventIntent.LoadEvent(1))
        assertEquals(30, vm2.state.value.hebrewDay)
    }

    @Test
    fun openingAStoredDay30ThatDoesNotExistShows29AndSaves29() {
        val short = EditorDayClampCases.shortYear(JewishDate.CHESHVAN)
        for (stored in listOf(storedDay30(JewishDate.CHESHVAN, short), storedDay30(JewishDate.IYAR, 5785))) {
            val dao = FakeDao(listOf(stored))
            val vm = viewModel(dao, leapToday)
            vm.onIntent(AddEditEventIntent.LoadEvent(1))
            assertEquals(29, vm.state.value.hebrewDay)
            assertEquals(stored.hebrewMonth, vm.state.value.hebrewMonth)
            assertEquals(stored.hebrewYear, vm.state.value.hebrewYear)
            // Nothing is written until Save; Save stores what is shown
            assertEquals(30, dao.events.single().hebrewDay)
            vm.onIntent(AddEditEventIntent.Save)
            assertEquals(29, dao.events.single().hebrewDay)
        }
    }

    @Test
    fun openingAnUnknownYearDay30CheshvanKeepsDay30() {
        val vm = viewModel(FakeDao(listOf(storedDay30(JewishDate.CHESHVAN, 0))), leapToday)
        vm.onIntent(AddEditEventIntent.LoadEvent(1))
        assertEquals(30, vm.state.value.hebrewDay)
        assertEquals(30, EditorDayClampCases.days(vm.state.value.hebrewYear, JewishDate.CHESHVAN))
    }
}
