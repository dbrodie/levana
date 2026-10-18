package ninja.doskey.app.levana.ui.location

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.domain.model.Location
import java.util.TimeZone

@Composable
fun ManualLocationScreen(onSave: (Location) -> Unit, modifier: Modifier = Modifier) {
    var name by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var timezone by rememberSaveable {
        mutableStateOf(TimeZone.getDefault().id)
    }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
        Text(stringResource(R.string.manual_location_details), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Text(stringResource(R.string.manual_location_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.manual_location_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = latitude,
            onValueChange = { latitude = it },
            label = { Text(stringResource(R.string.manual_location_latitude)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = longitude,
            onValueChange = { longitude = it },
            label = { Text(stringResource(R.string.manual_location_longitude)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = timezone,
            onValueChange = { timezone = it },
            label = { Text(stringResource(R.string.manual_location_timezone)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val latitudeError = stringResource(R.string.manual_location_error_latitude)
        val longitudeError = stringResource(R.string.manual_location_error_longitude)
        val nameError = stringResource(R.string.manual_location_error_name)
        Button(
            onClick = {
                val lat = latitude.toDoubleOrNull()
                val lon = longitude.toDoubleOrNull()
                if (lat == null || lat !in -90.0..90.0) {
                    error = latitudeError
                } else if (lon == null || lon !in -180.0..180.0) {
                    error = longitudeError
                } else if (name.isBlank()) {
                    error = nameError
                } else {
                    error = null
                    onSave(
                        Location(
                            latitude = lat,
                            longitude = lon,
                            timezoneId = timezone,
                            name = name.trim()
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.manual_location_save))
        }
            }
        }
    }
}
