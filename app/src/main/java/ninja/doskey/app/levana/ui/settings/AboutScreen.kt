package ninja.doskey.app.levana.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.update.UpdateChecker
import org.koin.compose.koinInject

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    updateChecker: UpdateChecker = koinInject()
) {
    val versionName = updateChecker.currentVersionName()
    val installSource = updateChecker.installerPackageName()
        ?: stringResource(R.string.about_install_source_none)
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_version)) },
            supportingContent = { Text(versionName) }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_install_source)) },
            supportingContent = { Text(installSource) }
        )
    }
}
