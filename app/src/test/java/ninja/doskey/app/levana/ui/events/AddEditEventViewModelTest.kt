package ninja.doskey.app.levana.ui.events

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ninja.doskey.app.levana.data.EventSerializer
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.data.PersonalEventRepository
import ninja.doskey.app.levana.device.deviceContextOn
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.db.PersonalEventDao
import ninja.doskey.app.levana.domain.model.SupportedHebrewYears
import ninja.doskey.app.levana.ui.EditorDayClampCases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ninja.doskey.app.levana.ui.pickDay
import ninja.doskey.app.levana.ui.pickMonth
import ninja.doskey.app.levana.ui.pickYear

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

    // ── TASK-24: supported years and out-of-range stored dates ─────────────

    @Test
    fun enteredYearStaysInTheSupportedYears() {
        val vm = viewModel()
        for ((entered, held) in listOf(
            5600 to 5600, 6000 to 6000, 5599 to 5600, 6001 to 6000,
            -5 to 5600, 0 to 5600, 123456 to 6000
        )) {
            vm.pickYear(entered)
            assertEquals("entered $entered", held, vm.state.value.hebrewYear)
        }
        // A prefill with an unusable year lands in the range too
        vm.onIntent(AddEditEventIntent.PreFillDate(1, JewishDate.TISHREI, 1))
        assertEquals(SupportedHebrewYears.FIRST, vm.state.value.hebrewYear)
    }

    @Test
    fun prefilledKnownYearOutsideTheRangeIsKept() {
        val vm = viewModel()
        vm.onIntent(AddEditEventIntent.PreFillDate(3, JewishDate.KISLEV, 5500))
        assertEquals(5500, vm.state.value.hebrewYear)
    }

    @Test
    fun defaultYearOutsideTheRangeIsMovedIntoIt() {
        // 2300 is Hebrew year 6060
        assertEquals(SupportedHebrewYears.LAST, viewModel(today = LocalDate.of(2300, 3, 1)).state.value.hebrewYear)
    }

    private val ordinaryYearBelowRange = (5501..5599).first { !HebrewDateMatcher.isEventLeapYear(it) }

    /** Stored dates (day, month, year) whose year is outside 5600..6000. */
    private val outOfRange = listOf(
        Triple(14, JewishDate.KISLEV, 5500),
        Triple(14, JewishDate.KISLEV, 6100),
        Triple(14, JewishDate.KISLEV, 5599),
        Triple(14, JewishDate.KISLEV, 6001),
        Triple(14, JewishDate.KISLEV, HebrewDateMatcher.MIN_KNOWN_YEAR),
        Triple(14, JewishDate.KISLEV, HebrewDateMatcher.MAX_KNOWN_YEAR),
        Triple(30, JewishDate.IYAR, 5500),
        Triple(14, JewishDate.ADAR_II, ordinaryYearBelowRange),
        Triple(14, JewishDate.ADAR, 0),
        Triple(30, JewishDate.CHESHVAN, 0),
        Triple(14, JewishDate.ADAR_II, 1),
        Triple(14, JewishDate.KISLEV, -1),
        Triple(14, JewishDate.KISLEV, 99999)
    )

    private fun storedAt(date: Triple<Int, Int, Int>, yahrzeit: Boolean = false) = PersonalEvent(
        id = 1,
        title = "Stored",
        hebrewDay = date.first,
        hebrewMonth = date.second,
        hebrewYear = date.third,
        useYahrzeitRules = yahrzeit
    )

    private fun PersonalEvent.date() = Triple(hebrewDay, hebrewMonth, hebrewYear)

    @Test
    fun outOfRangeStoredDateOpensAndSavesUnchanged() {
        for (date in outOfRange) for (yahrzeit in listOf(false, true)) {
            val dao = FakeDao(listOf(storedAt(date, yahrzeit)))
            val vm = viewModel(dao, leapToday)
            vm.onIntent(AddEditEventIntent.LoadEvent(1))
            if (HebrewDateMatcher.isKnownYear(date.third)) {
                // A known year is shown as stored
                assertEquals("$date", date.third, vm.state.value.hebrewYear)
            }
            assertEquals("$date", date, dao.events.single().date())
            vm.onIntent(AddEditEventIntent.Save)
            assertEquals("$date", date, dao.events.single().date())
        }
    }

    @Test
    fun editingOtherFieldsKeepsAnOutOfRangeDate() {
        for (date in outOfRange) {
            val dao = FakeDao(listOf(storedAt(date)))
            val vm = viewModel(dao, leapToday)
            vm.onIntent(AddEditEventIntent.LoadEvent(1))
            vm.onIntent(AddEditEventIntent.SetTitle("Renamed"))
            vm.onIntent(AddEditEventIntent.SetNotes("Notes"))
            vm.onIntent(AddEditEventIntent.SetUseYahrzeitRules(true))
            vm.onIntent(AddEditEventIntent.Save)
            val saved = dao.events.single()
            assertEquals("Renamed", saved.title)
            assertEquals("$date", date, saved.date())
        }
    }

    // ── TASK-45: the date picker ────────────────────────────────────────────

    @Test
    fun confirmingThePickerOnTheShownDateKeepsAnOutOfRangeDate() {
        for (date in outOfRange) {
            val dao = FakeDao(listOf(storedAt(date)))
            val vm = viewModel(dao, leapToday)
            vm.onIntent(AddEditEventIntent.LoadEvent(1))
            vm.onIntent(AddEditEventIntent.OpenDatePicker)
            assertTrue(vm.state.value.showDatePicker)
            // The picker opens on the shown date; OK without a new pick returns it
            vm.onIntent(AddEditEventIntent.SetHebrewDate(vm.state.value.gregorianDate))
            assertFalse(vm.state.value.showDatePicker)
            vm.onIntent(AddEditEventIntent.Save)
            assertEquals("$date", date, dao.events.single().date())
        }
    }

    @Test
    fun pickingAnotherDayInAnOutOfRangeYearSavesItInThatYear() {
        val dao = FakeDao(listOf(storedAt(Triple(12, JewishDate.NISSAN, 5500))))
        val vm = viewModel(dao, leapToday)
        vm.onIntent(AddEditEventIntent.LoadEvent(1))
        vm.onIntent(AddEditEventIntent.SetHebrewDate(vm.state.value.gregorianDate.plusDays(1)))
        vm.onIntent(AddEditEventIntent.Save)
        assertEquals(Triple(13, JewishDate.NISSAN, 5500), dao.events.single().date())
    }

    @Test
    fun aPickedDateIsShownAndSaved() {
        val dao = FakeDao()
        val vm = viewModel(dao, leapToday)
        vm.onIntent(AddEditEventIntent.SetTitle("Purim party"))
        vm.onIntent(AddEditEventIntent.OpenDatePicker)
        // Purim 5787, in Adar II of a leap year
        vm.onIntent(AddEditEventIntent.SetHebrewDate(LocalDate.of(2027, 3, 23)))
        assertEquals(Triple(14, JewishDate.ADAR_II, 5787), vm.state.value.let { Triple(it.hebrewDay, it.hebrewMonth, it.hebrewYear) })
        assertEquals(LocalDate.of(2027, 3, 23), vm.state.value.gregorianDate)
        vm.onIntent(AddEditEventIntent.Save)
        assertEquals(Triple(14, JewishDate.ADAR_II, 5787), dao.events.single().date())
    }

    @Test
    fun dismissingThePickerKeepsTheDate() {
        val vm = viewModel(today = leapToday)
        val before = vm.state.value
        vm.onIntent(AddEditEventIntent.OpenDatePicker)
        vm.onIntent(AddEditEventIntent.DismissDatePicker)
        assertEquals(before, vm.state.value)
    }

    @Test
    fun changingTheDayKeepsAStoredYearOutsideTheRange() {
        for (year in listOf(5500, 6100)) {
            val dao = FakeDao(listOf(storedAt(Triple(14, JewishDate.KISLEV, year))))
            val vm = viewModel(dao, leapToday)
            vm.onIntent(AddEditEventIntent.LoadEvent(1))
            vm.pickDay(15)
            vm.onIntent(AddEditEventIntent.Save)
            assertEquals(Triple(15, JewishDate.KISLEV, year), dao.events.single().date())
        }
    }

    @Test
    fun steppingTheYearOfAnOutOfRangeDateEntersTheRange() {
        // The stepper offers +1 from 5500 and -1 from 6100
        for ((year, stepped, expected) in listOf(Triple(5500, 5501, 5600), Triple(6100, 6099, 6000))) {
            val dao = FakeDao(listOf(storedAt(Triple(14, JewishDate.KISLEV, year))))
            val vm = viewModel(dao, leapToday)
            vm.onIntent(AddEditEventIntent.LoadEvent(1))
            vm.pickYear(stepped)
            assertEquals(expected, vm.state.value.hebrewYear)
            vm.onIntent(AddEditEventIntent.Save)
            assertEquals(Triple(14, JewishDate.KISLEV, expected), dao.events.single().date())
        }
    }

    @Test
    fun storedDatesAtTheBoundsAreNormalisedLikeAnyInRangeDate() {
        // 30 Iyar never exists: an in-range year saves 29 (spec 1e)
        for (year in listOf(SupportedHebrewYears.FIRST, SupportedHebrewYears.LAST)) {
            val dao = FakeDao(listOf(storedAt(Triple(30, JewishDate.IYAR, year))))
            val vm = viewModel(dao, leapToday)
            vm.onIntent(AddEditEventIntent.LoadEvent(1))
            assertEquals(year, vm.state.value.hebrewYear)
            vm.onIntent(AddEditEventIntent.Save)
            assertEquals(Triple(29, JewishDate.IYAR, year), dao.events.single().date())
        }
    }

    @Test
    fun importedOutOfRangeEventsOpenAndSaveUnchanged() {
        // Import accepts year 0 and every year the calendar handles (spec 2a)
        val importable = outOfRange.filter { it.third == 0 || HebrewDateMatcher.isKnownYear(it.third) }
        val events = importable.map { storedAt(it).copy(id = 0) }
        val files = listOf(
            EventSerializer.fromJson(EventSerializer.toJson(events)),
            EventSerializer.fromCsv(EventSerializer.toCsv(events))
        )
        for (imported in files) {
            assertEquals(importable, imported.map { it.date() })
            for (event in imported) {
                val dao = FakeDao(listOf(event.copy(id = 1)))
                val vm = viewModel(dao, leapToday)
                vm.onIntent(AddEditEventIntent.LoadEvent(1))
                vm.onIntent(AddEditEventIntent.Save)
                assertEquals(event.date(), dao.events.single().date())
            }
        }
    }

    @Test
    fun adarIIBecomesAdarWhenTheYearBecomesRegular() {
        val vm = viewModel(today = leapToday)
        vm.pickMonth(JewishDate.ADAR_II)
        vm.pickYear(5788)
        assertEquals(JewishDate.ADAR, vm.state.value.hebrewMonth)
    }

    // ── Spec 1e: day clamp ──────────────────────────────────────────────────

    private fun editor(vm: AddEditEventViewModel = viewModel()) = object : EditorDayClampCases.Editor {
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

    @Test
    fun savedDayIsTheShownDay() {
        val dao = FakeDao()
        val vm = viewModel(dao)
        vm.onIntent(AddEditEventIntent.SetTitle("New"))
        vm.pickYear(EditorDayClampCases.ordinaryYear)
        vm.pickDay(30)
        vm.pickMonth(JewishDate.IYAR)
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
