package com.levana.app.ui.birthday

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levana.app.ui.events.HebrewDatePicker
import com.levana.app.ui.events.formatHebrewDate
import org.koin.androidx.compose.koinViewModel

@Composable
fun ContactBirthdayScreen(
    contactLookupKey: String,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactBirthdayViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
        if (uri != null) {
            resolveContact(context, uri)?.let { (lookupKey, name, photo) ->
                viewModel.onIntent(
                    ContactBirthdayIntent.ContactSelected(
                        lookupKey,
                        name,
                        photo
                    )
                )
            }
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
        Text(
            text = if (state.isEditing) {
                "Edit Birthday"
            } else {
                "Add Birthday"
            },
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Contact info
        if (state.contactLookupKey.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    val photoBitmap = remember(
                        state.contactPhotoUri
                    ) {
                        state.contactPhotoUri?.let { uriStr ->
                            try {
                                context.contentResolver.openInputStream(
                                    Uri.parse(uriStr)
                                )?.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
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
                Text("Pick Contact")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hebrew date picker
        Text(
            text = "Hebrew Birthday",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        BirthdayDatePicker(
            day = state.hebrewDay,
            month = state.hebrewMonth,
            year = state.hebrewYear,
            onDayChange = {
                viewModel.onIntent(ContactBirthdayIntent.SetHebrewDay(it))
            },
            onMonthChange = {
                viewModel.onIntent(
                    ContactBirthdayIntent.SetHebrewMonth(it)
                )
            },
            onYearChange = {
                viewModel.onIntent(
                    ContactBirthdayIntent.SetHebrewYear(it)
                )
            }
        )

        // Date preview
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatHebrewDate(
                state.hebrewDay,
                state.hebrewMonth,
                state.hebrewYear
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
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
            Text(if (state.isEditing) "Update" else "Save")
        }
    }
}

@Composable
private fun BirthdayDatePicker(
    day: Int,
    month: Int,
    year: Int,
    onDayChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit
) {
    // Use year=0 to mean "unknown year"; for the picker, use a
    // reference year for leap-year month list
    val pickerYear = if (year == 0) 5784 else year

    HebrewDatePicker(
        day = day,
        month = month,
        year = pickerYear,
        onDayChange = onDayChange,
        onMonthChange = onMonthChange,
        onYearChange = { newYear ->
            onYearChange(newYear)
        }
    )
}

private fun resolveContact(
    context: android.content.Context,
    contactUri: Uri
): Triple<String, String, String?>? {
    val projection = arrayOf(
        ContactsContract.Contacts.LOOKUP_KEY,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
    )
    context.contentResolver.query(
        contactUri,
        projection,
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val lookupKey = cursor.getString(0) ?: return null
            val name = cursor.getString(1) ?: "Unknown"
            val photo = cursor.getString(2)
            return Triple(lookupKey, name, photo)
        }
    }
    return null
}
