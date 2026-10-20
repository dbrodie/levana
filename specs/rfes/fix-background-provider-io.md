# Fix: Content Provider and File IO on the Main Thread

## Summary

`ContactBirthdayRepository` and `SystemCalendarRepository` expose blocking `ContentResolver`
calls (queries, `applyBatch`, lookups) as plain functions. Every ViewModel calls them from
`viewModelScope` (Main), so month loads, the day panel, the events list, birthday edit / save /
delete and calendar selection run provider IPC on the UI thread. Contact photos are decoded
inside `remember {}` during composition, the contact picker result is queried in the
activity-result callback, and `EventsScreen` reads/writes import/export files synchronously in
its activity-result callbacks. Cancelling a load (e.g. paging months) cannot stop a running
query.

## Fix

- **Repositories become main-safe.** All provider-touching methods of
  `ContactBirthdayRepository` (`getAll`, `getBirthdaysForDate`, `getBirthdayDaysForHebrewMonth`,
  `getBirthdayDatesForGregorianMonth`, `getBirthdayForContact`, `setBirthday`,
  `removeBirthday`) and `SystemCalendarRepository` (`getDeviceCalendars`, `getEventsForDate`,
  `getEventColorsForDateRange`) are `suspend` and run on an injectable IO dispatcher
  (default `Dispatchers.IO`). Permission checks stay synchronous (no IO).
- **Cancellation-aware queries.** A shared helper runs each query with a `CancellationSignal`
  that is cancelled when the calling coroutine is cancelled, so the caller is released
  immediately and the provider can abort; cursor loops check for cancellation per row.
  Provider aborts surface as `CancellationException`, never as an error. `applyBatch` has no
  signal; cancellation is checked between its lookup queries and the batch.
- **Contact picker and photos.** New `ContactBirthdayRepository.resolveContact(uri)` and
  `loadPhoto(uri)` (suspend, IO). The picker result goes to the ViewModel
  (`ContactBirthdayIntent.ContactPicked`); the three photo sites use one `rememberContactPhoto`
  composable built on `produceState`, so decoding happens off the main thread and is cancelled
  with the composable. Photo failures still fall back to the icon.
- **Import / export.** A new `EventFileRepository` reads/writes the chosen document on the IO
  dispatcher, streaming in chunks with cancellation checks. Import input is **bounded to 1 MiB**
  (new localized "file too large" message). The screen only forwards the URI
  (`EventsIntent.ExportEvents` / `ImportFromUri`); the work runs in `viewModelScope`, so it
  survives recomposition/rotation. Results become a typed `EventsMessage` in state, mapped to the
  existing localized strings by the screen (same texts as before).
- **Errors stay visible, cancellation is never swallowed.** Every `catch (Exception)` around the
  new suspend calls rethrows `CancellationException`. Existing behaviour is kept: month/day
  loads fall back on `SecurityException`; birthday save shows its error; export/import show the
  same localized snackbar/dialog. Newly surfaced: a failed birthday delete shows a localized
  snackbar instead of crashing; calendar selection treats a `SecurityException` as "no
  permission".
- **UI state stays on Main.** ViewModels write state after the suspend call returns on
  `viewModelScope` (Main).

## Out of scope

Calendar event-date, Omer count and holy-day logic (unchanged). No redesign of screens,
storage or DI beyond wiring the new repository.

## Tests (JVM / Robolectric)

- Repository queries for contacts and calendars execute on the injected IO thread, not the
  calling (main) thread — asserted from inside a fake `ContentProvider`.
- Responsiveness: while a fake provider blocks a query, the main looper keeps processing
  ViewModel intents; the result is applied once the provider is released.
- Cancellation: cancelling a caller blocked on a slow query returns promptly, cancels the
  provider's `CancellationSignal`, and surfaces `CancellationException`.
- Birthday save/delete and calendar selection run their provider calls off the main thread.
- Import/export: streams are opened and read/written on the IO thread; oversized input is
  rejected with the bounded-size message; a failing stream yields the localized error message.
- Existing calendar-date, Omer and holy-day suites keep passing.
