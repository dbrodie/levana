package ninja.doskey.app.levana.ui.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun CityPickerScreen(
    onLocationSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CityPickerViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val dismissKeyboard = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // Navigating away (city selected, back arrow, system back) pauses this entry
    // immediately, so the keyboard is gone before the next screen appears.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        dismissKeyboard()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CityPickerEvent.LocationSaved -> onLocationSaved()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = {
                viewModel.onIntent(CityPickerIntent.Search(it))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text(stringResource(R.string.city_search_placeholder)) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null)
            },
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.cities, key = { "${it.name},${it.country}" }) { city ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth().clickable {
                        dismissKeyboard()
                        viewModel.onIntent(CityPickerIntent.SelectCity(city))
                    }
                ) {
                    ListItem(
                        headlineContent = { Text(city.name, style = MaterialTheme.typography.titleSmall) },
                        supportingContent = { Text(city.country, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 24.dp)) }
        }
    }
}
