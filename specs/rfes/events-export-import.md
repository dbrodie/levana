# RFE: Events Export / Import

## Summary

Add export and import functionality for custom personal events (`PersonalEvent`), accessible via a three-dot overflow menu on the Events screen. Supports JSON and CSV formats.

## Motivation

Users have no way to back up or transfer their custom Hebrew calendar events. Contact birthdays already benefit from device-level contacts backup, but `PersonalEvent` rows stored in the app's Room database are siloed with no recovery path if the app is uninstalled or the device is lost.

## Scope

### In scope
- Export all `PersonalEvent` rows to a user-chosen file (JSON or CSV)
- Import events from a JSON or CSV file produced by this feature
- Deduplicate on import: skip rows where `(title, hebrewDay, hebrewMonth, hebrewYear)` already exists in the database
- Show result feedback (snackbar for export, dialog for import)

### Out of scope
- Contact birthdays (these are backed up through the Android contacts sync system)
- Selective export (partial event lists)
- Encryption or password-protected archives

## UI Design

### Entry point
A `MoreVert` (`Icons.Filled.MoreVert`) `IconButton` is injected into the app-level `CenterAlignedTopAppBar` (defined in `MainActivity`) via a composable slot (`topBarActionsContent`). The button is registered when `EventsScreen` enters the composition and unregistered when it leaves.

The button is **only visible on the Events tab (page 1)**; switching to the Birthdays tab hides it. This is achieved by checking `pagerState.currentPage == 1` inside the registered composable — because `PagerState` is Compose observable state, the TopAppBar reacts to tab changes automatically.

### Overflow menu items
1. **Export as JSON** — opens Android's file-save picker pre-named `levana_events.json`
2. **Export as CSV** — opens Android's file-save picker pre-named `levana_events.csv`
3. **Import events** — opens Android's file-open picker (accepts JSON, CSV, or any file)

### Feedback
- Export: Snackbar at the bottom of the screen (`"Exported N events"` or error message)
- Import: `AlertDialog` showing `"Imported N event(s), skipped M duplicate(s)"` or an error description

## File Format Specification

### JSON
Top-level array of event objects. The `id` field is omitted (re-assigned on import).

```json
[
  {
    "title": "Pesach seder",
    "hebrewDay": 15,
    "hebrewMonth": 1,
    "hebrewYear": 5784,
    "notes": "Family gathering",
    "useYahrzeitRules": false
  }
]
```

### CSV
RFC 4180 format. Header row required. Fields with commas, double-quotes, or newlines are quoted.

```
title,hebrewDay,hebrewMonth,hebrewYear,notes,useYahrzeitRules
"Pesach seder",15,1,5784,"Family gathering",false
```

## Architecture

### New files

| File | Purpose |
|------|---------|
| `domain/model/PersonalEventDto.kt` | `@Serializable` data class mirroring `PersonalEvent` without the Room `id` |
| `data/EventSerializer.kt` | Pure-Kotlin object: `toJson`, `fromJson`, `toCsv`, `fromCsv` |

### Modified files

| File | Change |
|------|--------|
| `ui/events/EventsState.kt` | Add `exportMessage: String?`, `importResult: ImportResult?` |
| `ui/events/EventsIntent.kt` | Add `ShowExportResult`, `DismissExportResult`, `ImportEvents`, `DismissImportResult` |
| `ui/events/EventsViewModel.kt` | Handle import intents; call serializer; deduplicate against DB |
| `ui/events/EventsScreen.kt` | Add overflow menu via slot, SAF launchers, snackbar host, import result dialog; add `onRegisterTopBarActions`/`onUnregisterTopBarActions` parameters |
| `MainActivity.kt` | Add `topBarActionsContent` composable slot; wire it into `CenterAlignedTopAppBar` actions; pass slot callbacks to `EventsScreen` |

### File I/O
ContentResolver I/O (opening input/output streams from SAF URIs) lives in the composable, consistent with the existing contact-photo bitmap decoding pattern in `EventsScreen`. The serializer and ViewModel remain free of Android platform types.

### Import deduplication logic (ViewModel)
1. Parse content with `EventSerializer`
2. Fetch all existing events via `personalEventRepository.getAllOnce()`
3. Build a `Set<Triple<String,Int,Int,Int>>` of `(title, hebrewDay, hebrewMonth, hebrewYear)` — using a data class for readability
4. For each parsed event: if its key is in the set → `skipped++`; else → `insert()` + `imported++`
5. Update state with `ImportResult(imported, skipped)`

## Dependencies

No new external dependencies. Uses:
- `org.jetbrains.kotlinx:kotlinx-serialization-json` (already in `libs.versions.toml`)
- Android SAF (`ActivityResultContracts.CreateDocument`, `ActivityResultContracts.OpenDocument`)

## Testing

1. Build: `./gradlew :app:assembleDebug`
2. Navigate to Events screen → on Birthdays tab, confirm no three-dot icon appears
3. Switch to Events tab → confirm `MoreVert` icon appears in the TopAppBar
4. Add several events; export as JSON and inspect the file contents
5. Export as CSV and verify it opens correctly in a spreadsheet
6. Clear all events; import the JSON → verify events restored, count matches
7. Import the same JSON again → verify all events reported as duplicates (skipped)
8. Import a malformed file → verify an error dialog appears without a crash
