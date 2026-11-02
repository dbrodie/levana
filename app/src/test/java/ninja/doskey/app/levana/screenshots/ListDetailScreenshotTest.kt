package ninja.doskey.app.levana.screenshots

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.PaneLayoutRepository
import ninja.doskey.app.levana.domain.model.PaneSplit
import ninja.doskey.app.levana.domain.model.PaneStop
import ninja.doskey.app.levana.ui.panes.PANE_DIVIDER_TAG
import ninja.doskey.app.levana.ui.panes.listWidth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Month and Agenda with the day panel beside the list (Option E, TASK-52.6) at the window
 * sizes of 840dp and up: the Fold8 inner display in landscape and the Fold8 Ultra's inner
 * display both ways. Each divider stop and the hidden panel, in English and Hebrew, light and
 * dark. The selected day is the Shabbat after the harness's today, so the panel shows its
 * parsha and havdalah.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ListDetailScreenshotTest(private val size: FoldableSize, variant: Variant) : ScreenshotTest(variant, size.qualifiers) {

    @Test
    fun month() {
        launchCalendar()
        compose.onAllNodes(day(SHABBAT) and hasClickAction())[0].performClick()
        await(day(SHABBAT) and isSelected())
        everySplit { capture("listdetail_month_${size.tag}_$it") }
    }

    @Test
    fun agenda() {
        launchCalendar()
        openDrawer()
        clickable(string(R.string.nav_agenda)).performClick()
        awaitText(string(R.string.agenda_calendar_choose))
        // The day's date rail shows it in the panel
        await(day(SHABBAT) and hasClickAction())
        compose.onAllNodes(day(SHABBAT) and hasClickAction())[0].performClick()
        await(day(SHABBAT) and isSelected())
        everySplit { capture("listdetail_agenda_${size.tag}_$it") }
    }

    /** Runs [block] at each divider stop and with the panel hidden, named by the split. */
    private fun everySplit(block: (String) -> Unit) {
        for (split in SPLITS) {
            runBlocking { koin.get<PaneLayoutRepository>().save(split) }
            await(hasTestTag(PANE_DIVIDER_TAG) and dividerState(split))
            block(if (split.collapsed) "collapsed" else split.stop.name.lowercase())
        }
    }

    private fun dividerState(split: PaneSplit): SemanticsMatcher {
        val expected = if (split.collapsed) {
            string(R.string.pane_divider_hidden)
        } else {
            val percent = (listWidth(split.stop, size.widthDp.dp).value / size.widthDp * 100).roundToInt()
            string(R.string.pane_divider_state, percent)
        }
        return SemanticsMatcher("divider at $expected") { it.config.getOrNull(SemanticsProperties.StateDescription) == expected }
    }

    private fun day(date: LocalDate) = hasContentDescription(
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(app.resources.configuration.locales[0])),
        substring = true,
    )

    companion object {
        /** The Shabbat after the harness's today (Tuesday 17 November 2026). */
        private val SHABBAT: LocalDate = TODAY.plusDays(4)

        private val SPLITS = PaneStop.entries.map { PaneSplit(it) } + PaneSplit(collapsed = true)

        /** The sizes at or above 840dp, where Month and Agenda show two panes. */
        val EXPANDED = listOf(
            FoldableSize.FOLD8_INNER_LANDSCAPE,
            FoldableSize.FOLD8_ULTRA_INNER_PORTRAIT,
            FoldableSize.FOLD8_ULTRA_INNER_LANDSCAPE,
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun params() = EXPANDED.flatMap { size -> Variant.entries.map { arrayOf<Any>(size, it) } }
    }
}
