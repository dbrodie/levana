package ninja.doskey.app.levana.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ninja.doskey.app.levana.data.ZmanimRepository
import ninja.doskey.app.levana.domain.model.displayName
import ninja.doskey.app.levana.R
import org.koin.androidx.compose.koinViewModel
import ninja.doskey.app.levana.ui.contentWidth

@Composable
fun HalachicTimesSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier = modifier.verticalScroll(rememberScrollState()).contentWidth()) {
        HalachicTimesSection(
            selectedZmanim = state.selectedZmanim,
            onToggle = { name, enabled ->
                viewModel.onIntent(SettingsIntent.ToggleZman(name, enabled))
            }
        )
    }
}

@Composable
internal fun HalachicTimesSection(
    selectedZmanim: Set<String>,
    onToggle: (name: String, enabled: Boolean) -> Unit
) {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.zmanim_selection_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val hebrewUi = LocalConfiguration.current.locales[0].language in setOf("he", "iw")
        // Single line: the name in the app language. Keys are the English names
        ZmanimRepository.ZMAN_NAMES.forEach { zmanName ->
            val name = zmanName.english
            val checked = name in selectedZmanim
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                SettingsCheckboxRow(
                    title = zmanName.displayName(hebrewUi).primary,
                    checked = checked,
                    onCheckedChange = { onToggle(name, it) }
                )
            }
        }
    }
}
