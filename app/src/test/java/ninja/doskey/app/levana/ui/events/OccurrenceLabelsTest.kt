package ninja.doskey.app.levana.ui.events

import android.content.Context
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.OccurrenceLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class OccurrenceLabelsTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun englishDelayedLabels() {
        assertEquals(
            "Birthday (delayed from 30 Cheshvan)",
            occurrenceTitle(context, "Birthday", OccurrenceLabel.DelayedFrom30(HebrewMonth.CHESHVAN))
        )
        assertEquals("delayed from 30 Kislev", occurrenceLabelText(context, OccurrenceLabel.DelayedFrom30(HebrewMonth.KISLEV)))
        assertEquals("delayed from 30 Adar I", occurrenceLabelText(context, OccurrenceLabel.DelayedFrom30(HebrewMonth.ADAR_I)))
        assertEquals("delayed from 30 Adar", occurrenceLabelText(context, OccurrenceLabel.DelayedFrom30(HebrewMonth.ADAR)))
        assertEquals("Birthday", occurrenceTitle(context, "Birthday", null))
        assertEquals("Yahrzeit (Adar II)", occurrenceTitle(context, "Yahrzeit", OccurrenceLabel.ADAR_II))
    }

    @Test
    @Config(qualifiers = "iw")
    fun hebrewDelayedLabels() {
        assertEquals("נדחה מל׳ חשון", occurrenceLabelText(context, OccurrenceLabel.DelayedFrom30(HebrewMonth.CHESHVAN)))
        assertEquals("נדחה מל׳ כסלו", occurrenceLabelText(context, OccurrenceLabel.DelayedFrom30(HebrewMonth.KISLEV)))
        assertEquals("נדחה מל׳ אדר א׳", occurrenceLabelText(context, OccurrenceLabel.DelayedFrom30(HebrewMonth.ADAR_I)))
        assertEquals("נדחה מל׳ אדר", occurrenceLabelText(context, OccurrenceLabel.DelayedFrom30(HebrewMonth.ADAR)))
        assertEquals("יום הולדת (נדחה מל׳ חשון)", occurrenceTitle(context, "יום הולדת", OccurrenceLabel.DelayedFrom30(HebrewMonth.CHESHVAN)))
    }
}
