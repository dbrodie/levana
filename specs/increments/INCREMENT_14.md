# Increment 14: Android Calendar Integration

## Summary

Read-only integration with Android's system calendars via `CalendarContract`. Users select which device calendars to display, and events from those calendars appear as colored dots on the Levana calendar grid and as a detailed list in the day detail view. The feature is gated behind `READ_CALENDAR` permission and degrades gracefully when denied.

## What Was Built

### SystemCalendarRepository
- `SystemCalendarRepository` queries Android's `CalendarContract` content providers
- `getDeviceCalendars()` — lists all calendars with id, name, account, and color
- `getEventsForDate()` — fetches events for a single date, filtered by selected calendar IDs
- `getEventColorsForDateRange()` — optimized batch query returning a map of dates to calendar colors (max 3 per date) for the calendar grid view
- Uses `CalendarContract.Instances` for date-range queries with proper timezone handling
- All queries accept a `Set<Long>` of selected calendar IDs for filtering

### Calendar Selection Screen
- `CalendarSelectionScreen` — lists all device calendars with checkboxes
- Shows calendar color dot, display name, and account name per row
- Permission request UI when `READ_CALENDAR` not yet granted
- Empty state when no calendars found on device
- `CalendarSelectionViewModel` / `CalendarSelectionState` / `CalendarSelectionIntent` — MVI pattern

### Calendar Grid Integration
- `CalendarViewModel` calls `getEventColorsForDateRange()` for visible month
- System event colors stored in `HebrewDay.systemEventColors`
- `DayCell` renders colored dots (6dp circles) for each system calendar event color
- Dots appear alongside existing holiday and personal event indicators

### Day Detail Integration
- `DayDetailViewModel` calls `getEventsForDate()` for the selected day
- `SystemEventsSection` in `DayDetailScreen` displays events in a card
- Each event shows title, time range (or "All day"), and calendar color dot
- Events are tappable — opens the event in the system calendar app via `ACTION_VIEW` intent

### Preferences Persistence
- Selected calendar IDs stored in DataStore as `StringSet`
- `PreferencesRepository.saveSelectedCalendarIds()` serializes `Set<Long>` to `Set<String>`
- Selection restored on app restart via `UserPreferences.selectedCalendarIds`

### Settings Entry Point
- `SystemCalendarsSection` added to `SettingsScreen`
- DateRange icon, "System Calendars" title, navigates to `CalendarSelectionRoute`

## Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Content provider | `CalendarContract.Instances` | Handles recurring events correctly (expanded instances, not master events) |
| Color dot limit | Max 3 per date | Prevents visual clutter on calendar cells |
| Permission handling | SecurityException catch in ViewModels | Feature hidden silently rather than crashing; permission requested only in selection screen |
| Calendar ID storage | DataStore StringSet | Lightweight, no Room migration needed; calendar IDs are external and may change |
| Read-only | No event creation/modification | Scope limited to display; system calendar app handles editing |

## Files Modified

- `AndroidManifest.xml` — Added `READ_CALENDAR` permission
- `MainActivity.kt` — Added `CalendarSelectionRoute` navigation
- `AppModules.kt` — Added `SystemCalendarRepository` and `CalendarSelectionViewModel` to Koin
- `HebrewDay.kt` — Added `systemEventColors: List<Int>` field
- `UserPreferences.kt` — Added `selectedCalendarIds: Set<Long>` field
- `PreferencesRepository.kt` — Added `SELECTED_CALENDAR_IDS` key and `saveSelectedCalendarIds()`
- `CalendarViewModel.kt` — Added system event color loading for both Gregorian and Hebrew month views
- `CalendarScreen.kt` — Added colored dot rendering in `DayCell`
- `DayDetailViewModel.kt` — Added system event loading
- `DayDetailState.kt` — Added `systemEvents` field
- `DayDetailScreen.kt` — Added `SystemEventsSection` composable
- `SettingsScreen.kt` — Added `SystemCalendarsSection` with navigation callback
- `Routes.kt` — Added `CalendarSelectionRoute`

## New Files

- `SystemCalendarRepository.kt` — CalendarContract queries for calendars and events
- `DeviceCalendar.kt` — Domain model for a device calendar (id, name, account, color)
- `SystemCalendarEvent.kt` — Domain model for a system calendar event (title, times, allDay, color)
- `ui/calendarselection/CalendarSelectionScreen.kt` — Calendar picker UI with permission flow
- `ui/calendarselection/CalendarSelectionViewModel.kt` — Selection state management
- `ui/calendarselection/CalendarSelectionState.kt` — State data class
- `ui/calendarselection/CalendarSelectionIntent.kt` — User intent sealed interface

## Acceptance Criteria

- [x] Calendar selection screen lists all device calendars
- [x] Selected calendars' events appear as colored dots on Levana calendar
- [x] Day detail shows event titles and times from selected calendars
- [x] Calendar colors from the system are respected
- [x] Denying calendar permission hides the feature gracefully
- [x] Selected calendars persist across restarts
- [x] `./gradlew build` passes
