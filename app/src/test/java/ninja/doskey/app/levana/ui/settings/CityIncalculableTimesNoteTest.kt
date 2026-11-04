package ninja.doskey.app.levana.ui.settings

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Cities settings put the high-latitude note on the selected city's row only (TASK-57.31). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CityIncalculableTimesNoteTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(noteCityId: String?) {
        compose.setContent {
            MaterialTheme {
                LocationsSettingsContent(
                    currentLocationName = "Here",
                    hasLocationPermission = true,
                    savedLocations = listOf(ST_PETERSBURG, LONDON),
                    onRequestPermission = {},
                    onDelete = {},
                    onAddLocation = {},
                    incalculableTimesCityId = noteCityId
                )
            }
        }
    }

    private fun noteRows(text: String) = compose.onAllNodes(hasText(text), useUnmergedTree = true)

    @Test
    fun noteOnTheSelectedSavedCityOnly() {
        show(ST_PETERSBURG.id)
        noteRows(NOTE).assertCountEquals(1)
        compose.onAllNodes(hasText("St. Petersburg") and hasText(NOTE, substring = true)).assertCountEquals(1)
    }

    @Test
    fun noNoteWhenTheSelectedCityHasAllTimes() {
        show(null)
        noteRows(NOTE).assertCountEquals(0)
    }

    @Test
    fun noteOnTheCurrentLocation() {
        show(CURRENT_LOCATION_ID)
        compose.onAllNodes(hasText("Here") and hasText(NOTE, substring = true)).assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = "iw")
    fun hebrewNote() {
        show(ST_PETERSBURG.id)
        noteRows("בחלק מהימים לא ניתן לחשב חלק מהזמנים במיקום זה").assertCountEquals(1)
    }

    private companion object {
        const val NOTE = "Some times can’t be calculated at this location on some days"
        val ST_PETERSBURG = SavedLocation(
            "spb-id", Location(59.9343, 30.3351, timezoneId = "Europe/Moscow", name = "St. Petersburg")
        )
        val LONDON = SavedLocation(
            "london-id", Location(51.5074, -0.1278, timezoneId = "Europe/London", name = "London")
        )
    }
}
