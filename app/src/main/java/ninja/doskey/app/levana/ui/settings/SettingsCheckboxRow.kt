package ninja.doskey.app.levana.ui.settings

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * A settings row with a trailing checkbox. The whole row is one toggleable element
 * (Role.Checkbox), so a tap anywhere toggles it once and TalkBack reads one checkbox
 * with its label. The Checkbox itself only displays the state. [leadingContent] is
 * for decorative content (e.g. a color dot) and should not add semantics.
 */
@Composable
internal fun SettingsCheckboxRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    leadingContent: (@Composable () -> Unit)? = null
) {
    val colors = if (enabled) {
        ListItemDefaults.colors()
    } else {
        // Material 3 disabled content: onSurface at 38% opacity.
        val disabled = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ListItemDefaults.colors(headlineColor = disabled, supportingColor = disabled)
    }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = description?.let { { Text(it) } },
        leadingContent = leadingContent,
        trailingContent = {
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        },
        colors = colors,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange
        )
    )
}
