package ninja.doskey.app.levana.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * An accessibility label naming the item an action applies to, e.g. "Delete event Sarah".
 * [target] is user text and is only passed as a format argument. A blank target falls back
 * to the [generic] label.
 */
@Composable
internal fun targetedLabel(@StringRes targeted: Int, target: String, @StringRes generic: Int): String =
    if (target.isBlank()) stringResource(generic) else stringResource(targeted, target)
