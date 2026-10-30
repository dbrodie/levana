package ninja.doskey.app.levana.ui.datepicker

import android.app.Application
import android.os.Looper
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.AppDataStores
import ninja.doskey.app.levana.data.CalendarRepository
import ninja.doskey.app.levana.data.CityRepository
import ninja.doskey.app.levana.data.PreferencesRepository
import ninja.doskey.app.levana.device.DeviceContext
import ninja.doskey.app.levana.device.fixedClock
import ninja.doskey.app.levana.domain.model.HolidayCategory
import ninja.doskey.app.levana.domain.model.IsraelMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/** The picker's device context (TASK-5) and holidays from the app's calendar repository. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DatePickerViewModelTest {

    private lateinit var app: Application
    private lateinit var preferences: PreferencesRepository
    private val realDay = LocalDate.of(2026, 11, 17)
    private val newYork = ZoneId.of("America/New_York")

    private val purim5787 = JewishDate(5787, JewishDate.ADAR_II, 14).localDate

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        AppDataStores.clearPreferences(app)
        preferences = PreferencesRepository(app, CityRepository(app))
    }

    @After
    fun tearDown() = AppDataStores.clearPreferences(app)

    private fun viewModel(): DatePickerViewModel {
        val device = DeviceContext(preferences.preferences) { fixedClock(realDay, zone = newYork) }
        return DatePickerViewModel(CalendarRepository(), preferences, device)
    }

    private fun DatePickerViewModel.opened(date: LocalDate, leading: LeadingCalendar, session: Long = 1): DatePickerState {
        onIntent(DatePickerIntent.Open(date, leading, session))
        val deadline = System.currentTimeMillis() + 10_000
        while (state.value?.session != session) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))
            assertTrue("timed out", System.currentTimeMillis() < deadline)
            Thread.sleep(5)
        }
        return state.value!!
    }

    @Test
    fun todayIsTheDeviceDateIncludingTheDeveloperOverride() {
        assertEquals(realDay, viewModel().opened(purim5787, LeadingCalendar.HEBREW).today)
        val override = LocalDate.of(2027, 3, 1)
        runBlocking { preferences.saveDevDateOverride(override) }
        assertEquals(override, viewModel().opened(purim5787, LeadingCalendar.HEBREW).today)
    }

    @Test
    fun theShownMonthCarriesItsHolidaysAndTheSelectedDayItsChip() {
        val state = viewModel().opened(purim5787, LeadingCalendar.HEBREW)
        assertEquals(29, state.days.size)
        val purim = state.days.first { it.gregorianDate == purim5787 }
        assertEquals(HolidayCategory.RABBINIC, purim.holidayCategory)
        assertEquals("Purim", state.selectedDay?.holidayLabel)
    }

    @Test
    fun monthLengthsFollowTheHebrewCalendar() {
        val vm = viewModel()
        assertEquals(30, vm.opened(JewishDate(5787, JewishDate.ADAR, 1).localDate, LeadingCalendar.HEBREW).days.size)
        vm.onIntent(DatePickerIntent.PickYear(5786))
        assertEquals(29, vm.state.value!!.days.size)
        vm.onIntent(DatePickerIntent.SetLeading(LeadingCalendar.GREGORIAN))
        assertEquals(28, vm.state.value!!.days.size)
    }

    @Test
    fun israelAndDiasporaFollowTheResolvedSetting() {
        // 16 Nissan is the second day of Pesach in the Diaspora, Chol HaMoed in Israel
        val pesach2 = JewishDate(5787, JewishDate.NISSAN, 16).localDate
        runBlocking { preferences.saveIsraelMode(IsraelMode.DIASPORA) }
        val diaspora = viewModel().opened(pesach2, LeadingCalendar.GREGORIAN)
        assertEquals(HolidayCategory.TORAH, diaspora.selectedDay?.holidayCategory)
        assertEquals("Pesach", diaspora.selectedDay?.holidayLabel)

        runBlocking { preferences.saveIsraelMode(IsraelMode.ISRAEL) }
        val israel = viewModel().opened(pesach2, LeadingCalendar.GREGORIAN)
        assertEquals("Chol HaMoed Pesach", israel.selectedDay?.holidayLabel)
    }

    @Test
    fun aSelectionOutsideTheShownMonthStillHasItsHoliday() {
        val vm = viewModel()
        vm.opened(purim5787, LeadingCalendar.HEBREW)
        vm.onIntent(DatePickerIntent.NextMonth)
        assertEquals("Purim", vm.state.value!!.selectedDay?.holidayLabel)
        vm.onIntent(DatePickerIntent.SelectDay(JewishDate(5787, JewishDate.NISSAN, 3).localDate))
        assertNull(vm.state.value!!.selectedDay?.holidayCategory)
    }

    @Test
    fun reopeningStartsAFreshSession() {
        val vm = viewModel()
        vm.opened(purim5787, LeadingCalendar.HEBREW, session = 1)
        vm.onIntent(DatePickerIntent.ToggleYearList)
        val second = vm.opened(realDay, LeadingCalendar.GREGORIAN, session = 2)
        assertEquals(realDay, second.selection)
        assertEquals(LeadingCalendar.GREGORIAN, second.leading)
        assertEquals(false, second.yearListOpen)
    }
}
