package ninja.doskey.app.levana.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ninja.doskey.app.levana.data.PlaceNames

/** Unknown/geocoded place names are retained verbatim. */
@Composable
fun placeName(canonical: String): String =
    PlaceNames.resources[canonical]?.let { stringResource(it) } ?: canonical
