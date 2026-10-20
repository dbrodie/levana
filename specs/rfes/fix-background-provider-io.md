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
  that is cancelled when the calling coroutine is cancelled, so a provider that honours it
  aborts at once; cursor loops check for cancellation per row. Provider aborts surface as
  `CancellationException`, never as an error. Work without a signal (`applyBatch`, stream
  open) is skipped if cancellation came first, otherwise it finishes on the IO thread before
  the cancelled caller completes — the main thread is never blocked either way.
- **Contact picker and photos.** New `ContactBirthdayRepository.resolveContact(uri)` and a
  top-level `loadContactPhoto(resolver, uri)` (suspend, IO). The picker result goes to the ViewModel
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
  `viewModelScope` (Main). The events list's birthday load, now asynchronous, cancels any
  earlier load so a stale result cannot overwrite a newer one (e.g. after a removal).
  Birthday editor lifecycle: a new contact pick or selection cancels any pending pick
  lookup **and** any pending stored-birthday load; a repeated load request for the same
  record (rotation) is ignored, before or after completion, so edits survive; a load of a
  different record replaces the pending one; Save supersedes a pending load. A token check
  drops any superseded load result. Day/month/year fields edited while the stored birthday
  loads keep the user's values; unedited fields take the stored ones.

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

## Results (2026-10-20, JVM/Robolectric only)

`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest --rerun`
→ BUILD SUCCESSFUL, 38 suites, 250 tests, 0 failures, 0 errors, 0 skipped
(20 new: ContactBirthdayRepositoryIoTest 4, ContactBirthdayViewModelIoTest 4,
SystemCalendarRepositoryIoTest 3, EventsFileIoTest 9). `:app:lintDebug` reports nothing new in
the touched code.

Mutation checks: running provider work on the caller's thread fails 13 of the 19 IO tests;
not cancelling the `CancellationSignal` fails both cancellation tests; dropping the stale
birthday-load cancellation fails the removal-race test.

Not covered: no device/emulator run; `rememberContactPhoto` has no direct test (it relies on
the tested helper); Calendar/DayDetail view models are covered through their repositories.
Import parsing (≤ 1 MiB) still runs on Main.

### Follow-up: birthday editor races (2026-10-20)

Independent verification (test-only commit `b9e5925`) found that a slow earlier contact pick
could replace a later one, and a late stored-birthday load could discard a date edit made while
loading. Fixed in `ContactBirthdayViewModel`.

- Candidate suite: `./gradlew :app:testDebugUnitTest --rerun` → 38 suites, 254 tests, 0 failures,
  0 errors, 0 skipped (4 new focused regression tests in ContactBirthdayViewModelIoTest).
- With the verifier's `verifyio` tests applied (not committed here): 41 suites, 277 tests,
  0 failures, 0 errors, 0 skipped, including `aSlowFirstPickDoesNotOverrideALaterPick` and
  `aSlowBirthdayLoadDoesNotDiscardAnEditMadeWhileLoading`.
- Against the previous view model, 6 of these tests fail: both verifier reproductions, the
  3 new race tests and the updated edit-during-load test.

### Follow-up 2: editor load/selection lifecycle (2026-10-20)

Re-verification (test-only commit `e6e23c0`) found that a pick or selection made during a
pending stored load was overwritten by it, and that a repeated same-record load (rotation)
discarded an in-flight edit. Fixed as described above.

- Candidate suite: `./gradlew :app:testDebugUnitTest --rerun` → 38 suites, 262 tests,
  0 failures, 0 errors, 0 skipped (8 more focused tests in ContactBirthdayViewModelIoTest).
- With the verifier's `verifyio` tests from `b9e5925` + `e6e23c0` applied (not committed): 42
  suites, 291 tests, 0 failures, 0 errors, 0 skipped.
- Against the previous view model, 10 of the 22 tests in ContactBirthdayViewModelIoTest +
  VerifyBirthdayEditorRaceTest fail.

### Follow-up 3: calendar selection stale loads (2026-10-20)

`CalendarSelectionViewModel.load()` now awaits the provider query, and the permission prompt
stays on screen while it runs (`isLoading` is not set by a refresh). So a repeated
`PermissionGranted`/`Load` could start a second load while an earlier one was still querying,
and the earlier one could publish last: an older calendar list, or `hasPermission = true` after
a newer load had found no permission. Each load now cancels the previous one.

Audit of the other newly asynchronous loads: Calendar and DayDetail already cancel earlier
loads; the Events birthday list and the birthday editor are covered by earlier follow-ups.

- Two gated regression tests in SystemCalendarRepositoryIoTest; both fail without the guard
  (stale "Family" list replaces "Work"; stale load restores permission) and pass with it.
- Candidate suite: 38 suites, 264 tests, 0 failures/errors/skipped. With the verifier's
  `verifyio` tests (`b9e5925` + `e6e23c0`, not committed): 42 suites, 293 tests, 0 failures.
