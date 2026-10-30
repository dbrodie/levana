package ninja.doskey.app.levana.ui.walkthrough

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.R

/** The large rounded card that holds a screen's live mini-UI. */
@Composable
internal fun IllustrationCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.heightIn(min = 300.dp).padding(20.dp),
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

/**
 * The part of an illustration that only shows sample data. TalkBack skips it: the page's
 * title and body say what it shows.
 */
@Composable
internal fun Decorative(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clearAndSetSemantics { }, content = content)
}

/**
 * A screen's permission ask: why, then Allow and Not now; once granted, the granted state;
 * after a refusal, a note that it can be allowed later, with Allow still offered.
 */
@Composable
internal fun PermissionPanel(
    permission: WalkthroughPermission,
    status: PermissionStatus,
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Row {
                Icon(
                    permission.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp).size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(permission.explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (status == PermissionStatus.GRANTED) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(permission.granted),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                if (status == PermissionStatus.DENIED) {
                    Text(
                        stringResource(R.string.walkthrough_permission_denied),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 32.dp, top = 8.dp),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onNotNow) { Text(stringResource(R.string.walkthrough_not_now)) }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onAllow) { Text(stringResource(permission.allow)) }
                }
            }
        }
    }
}

/** Up to nine dots; the current page is a wider pill. TalkBack hears the page from the title. */
@Composable
internal fun PageDots(count: Int, page: Int, reducedMotion: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(8.dp).clearAndSetSemantics { },
    ) {
        repeat(count) { index ->
            val current = index == page
            val width by animateDpAsState(
                targetValue = if (current) 24.dp else 8.dp,
                animationSpec = if (reducedMotion) snap() else spring(),
                label = "dot",
            )
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

/** "3 of 12", for runs too long for dots. */
@Composable
internal fun PageCounter(count: Int, page: Int) {
    Text(
        stringResource(R.string.walkthrough_counter, page + 1, count),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clearAndSetSemantics { },
    )
}
