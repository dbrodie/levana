# Fix: calendar and day panel react to event changes

## Problem

`CalendarViewModel` and `DayDetailViewModel` read personal events (Room),
Hebrew contact birthdays (Contacts provider) and device calendar instances
(Calendar provider) only when a month/day is loaded or when preferences
change. Adding, editing or deleting a personal event, changing a contact's
Hebrew birthday, or a change to an event in a selected device calendar leaves
stale indicator dots in the month grid and a stale open day panel until the
user navigates away and back. Granting a permission (e.g. from another
screen or system settings) also leaves the calendar without that source's
data until a reload happens to occur.

## Behaviour after the fix

- When personal events, contact data or calendar data change, the visible
  month, its cached neighbouring months and the selected/open day are reloaded.
- The browsed month (Gregorian or Hebrew) and the selected date are retained;
  a refresh never scrolls the calendar or jumps back to today. The existing
  content stays on screen while the refresh runs (no loading flash).
- Changes that arrive while the calendar screen is not resumed are remembered
  and applied once, on the next resume, instead of reloading in the background.
- On resume, contact/calendar permissions are rechecked. A newly granted
  permission starts observing that provider and refreshes; a revoked one stops
  observing it (the loaders already degrade to empty data on
  `SecurityException`/missing permission).
- Bursts of notifications (e.g. a calendar sync) are debounced into a single
  reload.

## Design

### `EventDataChanges` (data layer)

`data/EventDataChanges.kt` — one signal, `changes: Flow<Unit>`, meaning
"event data shown by the calendar may have changed", plus
`recheckPermissions()`.

`AndroidEventDataChanges` (Koin `single`) merges:

- Room: `PersonalEventRepository.getAll()` (Room invalidation-backed Flow),
  `distinctUntilChanged()`, first emission dropped (it is the current state).
- Contacts: a `ContentObserver` on `ContactsContract.AUTHORITY_URI`
  (descendants) — only while contacts permission is held.
- Calendar: a `ContentObserver` on `CalendarContract.CONTENT_URI`
  (descendants, covers events and instances) — only while calendar
  permission is held.

Permission state is a `StateFlow`; the sources are rebuilt with
`flatMapLatest`, so a permission change unregisters the previous observers
before registering the new set, and emits one change. Equal permission state
on recheck emits nothing. Observers are registered in a `callbackFlow` and
unregistered in `awaitClose`, so cancellation always cleans up;
`registerContentObserver` failures (`SecurityException`) are swallowed and the
source stays silent.

The merged flow is shared with `shareIn(WhileSubscribed())` on an
application-lifetime scope so both view models share one observer per
provider; when no view model collects, observers are unregistered.

`ContentObserver` callbacks run on a binder thread and only `trySend` into a
conflated channel; all state handling happens in the collector
(`viewModelScope`, main thread).

### `ChangeRefresher` (ui layer)

Small helper used by both view models. It collects `changes` (debounced
300 ms) in `viewModelScope`:

- resumed → invoke the view model's refresh;
- not resumed → set a pending flag; `onResume()` rechecks permissions and runs
  a single refresh if pending. `onPause()` clears the resumed flag.

It never calls into data sources itself; refresh simply re-runs the view
model's existing load path, so it stays compatible with the loaders becoming
`suspend`/background-dispatched in a separate fix. Reading data does not
signal changes, so a refresh cannot trigger another refresh.

### View models / screen

- `CalendarIntent.Resume` / `Pause`, `DayDetailIntent.Resume` / `Pause`.
- `CalendarViewModel.refresh()` reloads the current Gregorian or Hebrew month
  (and neighbours) keeping current content and selection.
- `DayDetailViewModel.refresh()` reloads `lastLoadedDate` (if any) without
  setting `isLoading`.
- `CalendarScreen` sends Resume/Pause to both view models from a
  `LifecycleResumeEffect`.

## Tests

- `EventDataChangesTest` (Robolectric): observers registered only with
  permission; provider `notifyChange` and Room inserts emit; permission grant
  and revocation on recheck re-register and emit; two collectors share one
  observer; cancellation unregisters everything.
- `ReactiveCalendarRefreshTest` (Robolectric, in-memory Room, fake change
  source): adding/deleting a personal event updates month indicators, the
  neighbour cache and the open day while keeping browsed month and selected
  date (Gregorian and Hebrew); paused → no reload until resume, then exactly
  one; bursts debounce to a single reload; a refresh causes no further
  reloads.

## Out of scope

Moving provider/Room reads off the main thread (separate fix).
