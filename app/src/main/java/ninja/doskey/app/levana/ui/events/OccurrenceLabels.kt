package ninja.doskey.app.levana.ui.events

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.OccurrenceLabel

@StringRes
fun OccurrenceLabel.stringRes(): Int = when (this) {
    OccurrenceLabel.ADAR_I -> R.string.occurrence_adar_i
    OccurrenceLabel.ADAR_II -> R.string.occurrence_adar_ii
    OccurrenceLabel.SHEVAT_30 -> R.string.occurrence_shevat_30
    OccurrenceLabel.NISAN_1 -> R.string.occurrence_nisan_1
    OccurrenceLabel.CHESHVAN_29 -> R.string.occurrence_cheshvan_29
    OccurrenceLabel.KISLEV_1 -> R.string.occurrence_kislev_1
    OccurrenceLabel.KISLEV_29 -> R.string.occurrence_kislev_29
    OccurrenceLabel.TEVES_1 -> R.string.occurrence_teves_1
}

/** "Title (Adar I)" when the event occurs twice this year, otherwise the title. */
@Composable
fun occurrenceTitle(title: String, label: OccurrenceLabel?): String =
    if (label == null) {
        title
    } else {
        stringResource(R.string.occurrence_title_format, title, stringResource(label.stringRes()))
    }

fun occurrenceTitle(context: Context, title: String, label: OccurrenceLabel?): String =
    if (label == null) {
        title
    } else {
        context.getString(R.string.occurrence_title_format, title, context.getString(label.stringRes()))
    }
