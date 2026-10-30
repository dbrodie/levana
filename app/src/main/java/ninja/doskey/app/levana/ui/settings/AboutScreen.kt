package ninja.doskey.app.levana.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.ui.walkthrough.WalkthroughIntent
import ninja.doskey.app.levana.ui.walkthrough.activityWalkthroughViewModel
import ninja.doskey.app.levana.update.UpdateChecker
import org.koin.compose.koinInject
import ninja.doskey.app.levana.ui.contentWidth

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    updateChecker: UpdateChecker = koinInject()
) {
    val versionName = updateChecker.currentVersionName()
    val installSource = updateChecker.installerPackageName()
        ?: stringResource(R.string.about_install_source_none)
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).contentWidth()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_version)) },
            supportingContent = { Text(versionName) }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_install_source)) },
            supportingContent = { Text(installSource) }
        )
        HorizontalDivider()
        // Replays every screen; what has been seen stays as it is
        val walkthrough = activityWalkthroughViewModel()
        ListItem(
            headlineContent = { Text(stringResource(R.string.walkthrough_show_again)) },
            supportingContent = { Text(stringResource(R.string.walkthrough_show_again_summary)) },
            leadingContent = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
            modifier = Modifier.clickable { walkthrough.onIntent(WalkthroughIntent.Replay) }
        )
    }
}
