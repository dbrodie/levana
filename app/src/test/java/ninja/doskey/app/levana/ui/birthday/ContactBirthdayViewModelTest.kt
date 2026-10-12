package ninja.doskey.app.levana.ui.birthday

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate
import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.data.TodayProvider
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
}
