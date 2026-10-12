package ninja.doskey.app.levana.ui.events

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.data.EventSerializer
import ninja.doskey.app.levana.data.db.PersonalEvent
import ninja.doskey.app.levana.data.HebrewDateMatcher
import ninja.doskey.app.levana.domain.model.ContactBirthday
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val hebrewFormatter = HebrewDateFormatter().apply {
    isHebrewFormat = true
}

@Composable
fun EventsScreen(
    onAddEvent: () -> Unit,
    onEditEvent: (Long) -> Unit,
    onAddBirthday: () -> Unit,
    onEditBirthday: (String) -> Unit,
    onRegisterTopBarActions: (@Composable () -> Unit) -> Unit,
    onUnregisterTopBarActions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(EventsIntent.LoadEvents)
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.onIntent(EventsIntent.ContactsPermissionGranted)
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val message = try {
            val content = EventSerializer.toJson(state.customEvents)
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: error("Could not open output file")
            outputStream.use { stream ->
                stream.write(content.toByteArray())
            }
            context.getString(R.string.events_exported, state.customEvents.size)
        } catch (e: Exception) {
            context.getString(R.string.events_export_failed, e.message.orEmpty())
        }
        viewModel.onIntent(EventsIntent.ShowExportResult(message))
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val message = try {
            val content = EventSerializer.toCsv(state.customEvents)
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: error("Could not open output file")
            outputStream.use { stream ->
                stream.write(content.toByteArray())
            }
            context.getString(R.string.events_exported, state.customEvents.size)
        } catch (e: Exception) {
            context.getString(R.string.events_export_failed, e.message.orEmpty())
        }
        viewModel.onIntent(EventsIntent.ShowExportResult(message))
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: run {
                viewModel.onIntent(
                    EventsIntent.ShowExportResult(context.getString(R.string.events_import_failed_read))
                )
                return@rememberLauncherForActivityResult
            }
            val isCsv = detectCsv(uri, content)
            viewModel.onIntent(EventsIntent.ImportEvents(content, isCsv))
        } catch (e: Exception) {
            viewModel.onIntent(
                EventsIntent.ShowExportResult(context.getString(R.string.events_import_failed, e.message.orEmpty()))
            )
        }
    }

    // Overflow menu state — defined here so the lambda registered below can capture it
    val showOverflowMenuState = remember { mutableStateOf(false) }

    // Register the MoreVert button into the parent's TopAppBar for this screen's lifetime.
    // The button is only visible on the Events tab (page 1), not the Birthdays tab.
    DisposableEffect(Unit) {
        onRegisterTopBarActions {
            if (pagerState.currentPage == 1) Box {
                IconButton(onClick = { showOverflowMenuState.value = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.events_more_options))
                }
                DropdownMenu(
                    expanded = showOverflowMenuState.value,
                    onDismissRequest = { showOverflowMenuState.value = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.events_export_json)) },
                        onClick = {
                            showOverflowMenuState.value = false
                            exportJsonLauncher.launch("levana_events.json")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.events_export_csv)) },
                        onClick = {
                            showOverflowMenuState.value = false
                            exportCsvLauncher.launch("levana_events.csv")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.events_import)) },
                        onClick = {
                            showOverflowMenuState.value = false
                            importLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "*/*"
                                )
                            )
                        }
                    )
                }
            }
        }
        onDispose { onUnregisterTopBarActions() }
    }

    // Show export result as a snackbar
    LaunchedEffect(state.exportMessage) {
        val msg = state.exportMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.onIntent(EventsIntent.DismissExportResult)
    }

    // Import result dialog
    state.importResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(EventsIntent.DismissImportResult) },
            title = { Text(stringResource(if (result.error != null) R.string.events_import_failed_title else R.string.events_import_complete_title)) },
            text = {
                Text(
                    if (result.error != null) {
                        result.error
                    } else {
                        stringResource(R.string.events_imported, result.imported) +
                            if (result.skipped > 0) {
                                stringResource(R.string.events_import_skipped, result.skipped)
                            } else {
                                ""
                            }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(EventsIntent.DismissImportResult) }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 0) {
                        if (state.hasContactsPermission) {
                            onAddBirthday()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.WRITE_CONTACTS
                                )
                            )
                        }
                    } else {
                        onAddEvent()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.events_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    text = { Text(stringResource(R.string.events_tab_birthdays)) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    text = { Text(stringResource(R.string.events_tab_events)) }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> BirthdaysTab(
                        birthdays = state.birthdays,
                        hasPermission = state.hasContactsPermission,
                        onEditBirthday = onEditBirthday,
                        onDeleteBirthday = { lookupKey ->
                            viewModel.onIntent(
                                EventsIntent.DeleteBirthday(lookupKey)
                            )
                        },
                        onRequestPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.WRITE_CONTACTS
                                )
                            )
                        }
                    )
                    1 -> CustomEventsTab(
                        events = state.customEvents,
                        isLoading = state.isLoading,
                        onEditEvent = onEditEvent,
                        onDeleteEvent = { event ->
                            viewModel.onIntent(
                                EventsIntent.DeleteCustomEvent(event)
                            )
                        }
                    )
                }
            }
        }
    }
}

/** Returns true if the content looks like CSV rather than JSON. */
private fun detectCsv(uri: Uri, content: String): Boolean {
    val path = uri.lastPathSegment?.lowercase() ?: ""
    if (path.endsWith(".json")) return false
    if (path.endsWith(".csv")) return true
    return !content.trimStart().startsWith("[")
}

@Composable
private fun BirthdaysTab(
    birthdays: List<ContactBirthday>,
    hasPermission: Boolean,
    onEditBirthday: (String) -> Unit,
    onDeleteBirthday: (String) -> Unit,
    onRequestPermission: () -> Unit
) {
    if (!hasPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.events_contacts_permission_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.events_contacts_permission_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRequestPermission) {
                    Text(stringResource(R.string.common_grant_permission))
                }
            }
        }
        return
    }

    if (birthdays.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.events_no_birthdays),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.events_no_birthdays_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        items(birthdays, key = { it.contactLookupKey }) { birthday ->
            BirthdayCard(
                birthday = birthday,
                onClick = { onEditBirthday(birthday.contactLookupKey) },
                onDelete = { onDeleteBirthday(birthday.contactLookupKey) }
            )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun BirthdayCard(birthday: ContactBirthday, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.events_remove_birthday_title)) },
            text = {
                Text(
                    stringResource(R.string.events_remove_birthday_message, birthday.contactName)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.events_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            val photoBitmap = remember(birthday.contactPhotoUri) {
                birthday.contactPhotoUri?.let { uriStr ->
                    try {
                        context.contentResolver.openInputStream(
                            Uri.parse(uriStr)
                        )?.use { BitmapFactory.decodeStream(it) }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = birthday.contactName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Cake,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = birthday.contactName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatHebrewDate(
                        birthday.hebrewDay,
                        birthday.hebrewMonth,
                        birthday.hebrewYear
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.design_birthday),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CustomEventsTab(
    events: List<PersonalEvent>,
    isLoading: Boolean,
    onEditEvent: (Long) -> Unit,
    onDeleteEvent: (PersonalEvent) -> Unit
) {
    if (events.isEmpty() && !isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.events_no_custom),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.events_no_custom_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        items(events, key = { it.id }) { event ->
            EventCard(
                event = event,
                onClick = { onEditEvent(event.id) },
                onDelete = { onDeleteEvent(event) }
            )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun EventCard(event: PersonalEvent, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.events_delete_title)) },
            text = { Text(stringResource(R.string.events_delete_message, event.title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (event.useYahrzeitRules) {
                    Icons.Outlined.WbTwilight
                } else {
                    Icons.Filled.Event
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatHebrewDate(
                        event.hebrewDay,
                        event.hebrewMonth,
                        event.hebrewYear
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.useYahrzeitRules) {
                    Text(
                        text = stringResource(R.string.events_yahrzeit_rules),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * A stored recurring date in Hebrew, e.g. "י׳ כסלו תשפ״ד". Any year the
 * calendar library cannot handle (0, 1, -1, …) is an unknown year and gives
 * the year-less form "י׳ כסלו", never raw numbers.
 */
internal fun formatHebrewDate(day: Int, month: Int, year: Int): String {
    val yearKnown = HebrewDateMatcher.isKnownYear(year)
    if (yearKnown) {
        try {
            return hebrewFormatter.format(JewishDate(year, month, day))
        } catch (_: Exception) {
            // A day or month that year does not have: build it from parts below
        }
    }
    return try {
        require(day in 1..30)
        val dayAndMonth =
            "${hebrewFormatter.formatHebrewNumber(day)} ${hebrewMonthName(month, year)}"
        if (yearKnown) {
            "$dayAndMonth ${hebrewFormatter.formatHebrewNumber(year)}"
        } else {
            dayAndMonth
        }
    } catch (_: Exception) {
        if (yearKnown) "$day/$month/$year" else "$day/$month"
    }
}

// Reference years for month names only: 5785 is a regular year, 5784 a leap year
private const val REGULAR_REFERENCE_YEAR = 5785
private const val LEAP_REFERENCE_YEAR = 5784

/**
 * Month name for a stored date. With an unknown or regular year, month 12 is
 * plain "אדר"; only a known leap year makes it "אדר א׳". Month 13 is always
 * "אדר ב׳".
 */
private fun hebrewMonthName(month: Int, year: Int): String {
    val referenceYear = if (
        month == JewishDate.ADAR_II || HebrewDateMatcher.isEventLeapYear(year)
    ) {
        LEAP_REFERENCE_YEAR
    } else {
        REGULAR_REFERENCE_YEAR
    }
    return hebrewFormatter.formatMonth(JewishDate(referenceYear, month, 1))
}
