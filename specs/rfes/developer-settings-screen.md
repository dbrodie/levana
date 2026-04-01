# Developer Settings Sub-Screen

## Summary

Move developer settings out of the main Settings screen and into a dedicated sub-screen. Replace the inline Hebrew text-field date picker with a proper date dialog matching the existing GoToDate UX.

## Motivation

Developer settings (date override, force holiday theme) were embedded inline at the bottom of the main Settings screen, cluttering it for all users. Moving them behind a navigation row makes them accessible but unobtrusive. The existing date override UI (three bare text fields) was difficult to use; the GoToDateDialog pattern with bidirectional Hebrew/Gregorian pickers is significantly easier.

## Changes

### Navigation
- New `DeveloperSettingsRoute` in `Routes.kt`
- Wired in `MainActivity.kt` with "Developer Settings" app bar title

### Main Settings screen
- Developer settings section replaced with a single `ListItem` navigation row
- Tapping navigates to the new sub-screen

### Developer Settings sub-screen (`DeveloperSettingsScreen`)
- Date Override: `ListItem` showing current override as Hebrew date (or "Not set"); tap opens `GoToDateDialog`
- Force Holiday Theme: existing `HolidayThemePicker` dropdown, unchanged

### `GoToDateDialog` enhancement
- Added optional `onClear: (() -> Unit)?` parameter
- When set, a "Clear" button appears as the dialog's dismiss button
- Existing `GoToDateDialog` callers unaffected (parameter defaults to null)

### Calendar pager blank-screen fix
- **Root cause**: `CalendarIntent.LoadToday` loaded the target month in the ViewModel but did not send a scroll target to the `HorizontalPager`. The pager's `baseMonth` is anchored to real `LocalDate.now()` at composition time; when the dev date override points to a different month, the pager stayed on the real-today page while `currentMonth` changed, causing the `when` block to fall through to the blank `else` branch.
- **Fix**: `LoadToday` now sends `_gregorianScrollTarget` / `_hebrewScrollTarget` before loading the month, so the pager scrolls to the correct month alongside the data load.
