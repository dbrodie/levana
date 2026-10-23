package ninja.doskey.app.levana.ui.settings

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import ninja.doskey.app.levana.domain.model.Location
import ninja.doskey.app.levana.domain.model.SavedLocation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SavedLocationDeleteLabelTest {

    @get:Rule
    val compose = createComposeRule()

    private val deleted = mutableListOf<String>()

    private fun show(saved: List<SavedLocation> = listOf(JERUSALEM, NEW_YORK)) {
        compose.setContent {
            MaterialTheme {
                LocationsSettingsContent(
                    currentLocationName = "Here",
                    hasLocationPermission = true,
                    savedLocations = saved,
                    onRequestPermission = {},
                    onDelete = { deleted += it },
                    onAddLocation = {}
                )
            }
        }
    }

    private fun isButton() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    @Test
    fun deleteButtonsNameTheirLocationAndTargetIt() {
        show()

        compose.onNodeWithContentDescription("Delete saved location Jerusalem").assert(isButton())
        compose.onAllNodesWithContentDescription("Delete").assertCountEquals(0)
        compose.onAllNodes(hasText("Delete saved location Jerusalem")).assertCountEquals(0)

        compose.onNodeWithContentDescription("Delete saved location New York").performClick()
        assertEquals(listOf("ny-id"), deleted)
    }

    @Test
    fun blankNameFallsBackToGenericLabel() {
        show(listOf(SavedLocation("blank", JERUSALEM.location.copy(name = ""))))
        compose.onNodeWithContentDescription("Delete").assert(isButton()).performClick()
        assertEquals(listOf("blank"), deleted)
    }

    @Test
    @Config(qualifiers = "iw")
    fun hebrewLabel() {
        show()
        compose.onNodeWithContentDescription("מחיקת המיקום השמור Jerusalem").assert(isButton()).performClick()
        assertEquals(listOf("jlm-id"), deleted)
    }

    private companion object {
        val JERUSALEM = SavedLocation("jlm-id", Location.JERUSALEM.copy(name = "Jerusalem"))
        val NEW_YORK = SavedLocation(
            "ny-id",
            Location(latitude = 40.7, longitude = -74.0, timezoneId = "America/New_York", name = "New York")
        )
    }
}
