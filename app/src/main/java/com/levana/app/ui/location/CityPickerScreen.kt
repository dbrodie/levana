package com.levana.app.ui.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun CityPickerScreen(
    onLocationSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CityPickerViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                .padding(16.dp),
            placeholder = { Text("Search cities...") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null)
            },
            singleLine = true
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.cities, key = { "${it.name},${it.country}" }) { city ->
                ListItem(
                    headlineContent = { Text(city.name) },
                    supportingContent = {
                        Text(
                            city.country,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.onIntent(CityPickerIntent.SelectCity(city))
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
