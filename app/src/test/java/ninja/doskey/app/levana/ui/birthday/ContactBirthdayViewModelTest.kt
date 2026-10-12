package ninja.doskey.app.levana.ui.birthday

import ninja.doskey.app.levana.data.ContactBirthdayRepository
import ninja.doskey.app.levana.data.HebrewDateMatcher
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

    private fun viewModel() = ContactBirthdayViewModel(
        ContactBirthdayRepository(RuntimeEnvironment.getApplication())
    )

    @Test
    fun newBirthdayStartsWithTheYearTheEditorShows() {
        val year = viewModel().state.value.hebrewYear
        assertEquals(DEFAULT_BIRTHDAY_YEAR, year)
        assertTrue(HebrewDateMatcher.isKnownYear(year))
    }

    @Test
    fun setHebrewYearUpdatesState() {
        val vm = viewModel()
        vm.onIntent(ContactBirthdayIntent.SetHebrewYear(5790))
        assertEquals(5790, vm.state.value.hebrewYear)
    }
}
