package ninja.doskey.app.levana.ui.events

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.HebrewMonth
import ninja.doskey.app.levana.domain.model.OccurrenceLabel

/** The label text, e.g. "Adar I" or "delayed from 30 Cheshvan", in the context's language. */
fun occurrenceLabelText(context: Context, label: OccurrenceLabel): String = when (label) {
    OccurrenceLabel.ADAR_I -> context.getString(R.string.occurrence_adar_i)
    OccurrenceLabel.ADAR_II -> context.getString(R.string.occurrence_adar_ii)
    OccurrenceLabel.SHEVAT_30 -> context.getString(R.string.occurrence_shevat_30)
    OccurrenceLabel.NISAN_1 -> context.getString(R.string.occurrence_nisan_1)
    OccurrenceLabel.CHESHVAN_29 -> context.getString(R.string.occurrence_cheshvan_29)
    OccurrenceLabel.KISLEV_1 -> context.getString(R.string.occurrence_kislev_1)
    OccurrenceLabel.KISLEV_29 -> context.getString(R.string.occurrence_kislev_29)
    OccurrenceLabel.TEVES_1 -> context.getString(R.string.occurrence_teves_1)
    is OccurrenceLabel.DelayedFrom30 -> context.getString(
        R.string.occurrence_delayed_from_30,
        monthName(label.month, isHebrew(context))
    )
}

/** "Title (Adar I)" / "Title (delayed from 30 Cheshvan)" when labelled, otherwise the title. */
@Composable
fun occurrenceTitle(title: String, label: OccurrenceLabel?): String =
    occurrenceTitle(LocalContext.current, title, label)

fun occurrenceTitle(context: Context, title: String, label: OccurrenceLabel?): String =
    if (label == null) {
        title
    } else {
        context.getString(R.string.occurrence_title_format, title, occurrenceLabelText(context, label))
    }

private fun isHebrew(context: Context): Boolean =
    context.resources.configuration.locales[0].language in setOf("he", "iw")

private val hebrewFormatter = HebrewDateFormatter().apply { isHebrewFormat = true }

// Reference years for month names only: 5785 is an ordinary year, 5784 a leap year
private fun monthName(month: HebrewMonth, hebrew: Boolean): String {
    if (!hebrew) return month.englishName
    val referenceYear = if (month == HebrewMonth.ADAR_I || month == HebrewMonth.ADAR_II) 5784 else 5785
    return hebrewFormatter.formatMonth(JewishDate(referenceYear, month.jewishDateValue, 1))
}
