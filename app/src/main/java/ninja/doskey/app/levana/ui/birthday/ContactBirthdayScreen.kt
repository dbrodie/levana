package ninja.doskey.app.levana.ui.birthday

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.ui.datepicker.HebrewDateField
import ninja.doskey.app.levana.ui.datepicker.HebrewDatePickerDialog
import ninja.doskey.app.levana.ui.datepicker.LeadingCalendar
import ninja.doskey.app.levana.ui.events.formatHebrewDate
import org.koin.androidx.compose.koinViewModel

@Composable
fun ContactBirthdayScreen(
    contactLookupKey: String,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactBirthdayViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(contactLookupKey) {
        if (contactLookupKey.isNotBlank()) {
            viewModel.onIntent(
                ContactBirthdayIntent.LoadBirthday(contactLookupKey)
            )
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        // The contact is looked up off the main thread by the view model
        if (uri != null) {
            viewModel.onIntent(ContactBirthdayIntent.ContactPicked(uri))
        }
    }

    LaunchedEffect(state.saveComplete) {
        if (state.saveComplete) onSaved()
    }

    // Auto-launch contact picker for new birthdays
    LaunchedEffect(Unit) {
        if (contactLookupKey.isBlank() &&
            state.contactLookupKey.isBlank()
        ) {
            contactPickerLauncher.launch(null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.birthday_date_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))

        // Contact info
        if (state.contactLookupKey.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme
                        .surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val photo = rememberContactPhoto(state.contactPhotoUri).value
                    if (photo != null) {
                        Image(
                            bitmap = photo,
                            contentDescription = state.contactName,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = state.contactName,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = { contactPickerLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.birthday_pick_contact))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HebrewDateField(
            label = stringResource(R.string.calendar_hebrew_date),
            hebrewDate = formatHebrewDate(state.hebrewDay, state.hebrewMonth, state.hebrewYear),
            gregorianDate = state.gregorianDate,
            onClick = { viewModel.onIntent(ContactBirthdayIntent.OpenDatePicker) }
        )

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.onIntent(ContactBirthdayIntent.Save)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving &&
                state.contactLookupKey.isNotBlank()
        ) {
            Text(stringResource(if (state.isEditing) R.string.event_update else R.string.event_save))
        }
    }

    if (state.showDatePicker) {
        HebrewDatePickerDialog(
            initialDate = state.gregorianDate,
            leading = LeadingCalendar.HEBREW,
            onConfirm = { viewModel.onIntent(ContactBirthdayIntent.SetHebrewDate(it)) },
            onDismiss = { viewModel.onIntent(ContactBirthdayIntent.DismissDatePicker) }
        )
    }
}
