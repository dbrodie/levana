---
id: TASK-52.3
title: 'Fold/unfold continuity: same screen, preserved state, correct relayout'
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T10:31:00Z'
updated_date: '2026-10-31T18:52:00Z'
labels: []
dependencies:
  - TASK-52.1
parent_task_id: TASK-52
priority: high
type: task
ordinal: 58000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Folding or unfolding is a configuration change (size, and often density and orientation); it can also move the app between displays. Verify and fix it so the user stays exactly where they were and the layout adapts.

Check across fold→unfold and unfold→fold, rotation on the inner display, and entering and leaving split screen:
- **Navigation:** the same route and back stack; Month vs Agenda; deep-linked screens.
- **Screen state:** calendar month and selected day, Agenda scroll position and loaded window, search query and results, the Zmanim date, the drawer's open state.
- **Dialogs and in-progress input:** open dialogs and the date picker (including the year list and the current selection), unsaved editor input (event title or notes, birthday contact and date), the walkthrough page and its permission step, in-flight permission prompts, the TASK-46 mismatch dialog (it must not reappear or duplicate).
- **Nothing reloads or flashes needlessly:** no repeated network or geocoder calls, no alarm rebuilds triggered by the config change, no re-entry into the walkthrough.
- **Process death** while unfolded, then restore.

Add instrumentation or Robolectric tests that simulate configuration changes (e.g. StateRestorationTester or ActivityScenario recreate with new qualifiers) for the key screens.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Fold/unfold, rotation and split-screen keep the same screen, back stack and in-progress state on every screen listed; the layout matches the new size
- [x] #2 No duplicate dialogs, prompts or walkthrough on config change; tests cover state restoration for key screens; emulator evidence (screen recording or screenshots before and after) recorded
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Audit (code read 2026-10-30, master 8a86ad0 + TASK-50 fix). ViewModels survive a configuration change; what breaks is composition-only state, effects that re-run in the recreated composition, and anything held only in a ViewModel (process death).
Found:
- Shell: LevanaApp and MainActivity read the preferences with collectAsState(initial = null), so every recreation shows a blank frame and the system theme until DataStore answers (a light flash for a forced dark theme).
- Startup effects: every recreation gets a catch-up ON_RESUME, which runs a high-accuracy location fix + reverse geocode + DataStore write (and with it, every screen's preference reload).
- Editors: LaunchedEffect(eventId)/LaunchedEffect(contactLookupKey) reload the stored item over unsaved input, and the prefill reapplies over a picked date; a new birthday relaunches the contact picker after a fold.
- Month: the day sheet's LaunchedEffect(selectedDate) collapses an expanded sheet and scrolls it to the top; DayDetail LoadDay reloads with a spinner.
- Zmanim: LoadDate reruns with a spinner.
- Dialogs/menus kept in plain remember close: the 8 Settings dialogs, the developer date dialog and menu, My events' overflow menu and delete confirmations, the drawer's city list, the candle-lighting slider's unsaved value.
- Process death: the nav back stack is restored, but ViewModel-only state is lost: the Month's selected day/month and Go-to-date dialog, the Agenda's position, search and date picker, the editors' unsaved input and their date pickers.
Already fine (verify with tests): the date picker session (rememberSaveable session, VM state incl. year list), Agenda list state (rememberLazyListState) and search (VM), walkthrough page and permission step (activity-scoped VM, saved pendingPermission), the TASK-46 notice (saved flag, recorded key: no duplicate), the location ask (saved flag), notification alarms (not rebuilt on a config change; the icon refresh is idempotent).

Plan:
1. Shell: an activity-scoped ShellViewModel holds the preferences (stateIn, Eagerly) so a recreated activity has them on its first frame; MainActivity and LevanaApp read it. It also records that the location was refreshed, so the catch-up ON_RESUME of a recreated composition does not refresh again (a real return to the app still does).
2. Editors: LoadEvent/LoadBirthday/PreFillDate apply once per ViewModel (later identical intents are ignored); the birthday contact picker auto-launch is guarded by a saved flag.
3. Month: the day sheet collapses only when the selected day changes (saved last-shown day). DayDetail and Zmanim ignore a load of the day already shown.
4. Dialog and menu flags: rememberSaveable.
5. Process death: SavedStateHandle in CalendarViewModel (selected day, Go-to-date dialog), AgendaViewModel (the day at the top, search query, date picker), AddEditEventViewModel and ContactBirthdayViewModel (unsaved fields and the date picker), so restoring after process death returns to the same state.
6. Tests: Robolectric MainActivity recreated with new qualifiers (fold 411/475dp cover <-> inner, rotation, split half) for the key screens: route + back stack, Month selected day/month + sheet, Agenda position/search, Zmanim date, drawer, Settings dialog, date picker incl. year list, editor input, walkthrough page, TASK-46 dialog once; process-death simulation (saved state into a fresh activity + new view models); VM unit tests for idempotent loads and SavedStateHandle restore.
7. Emulator: Fold8 and Fold8 Ultra: fold->unfold, unfold->fold, inner rotation, split screen enter/leave; screenshots before/after per scenario + README in /tmp/levana-task52.3-evidence; logcat for geocoder/location calls.
8. Verify the full command from clean test-results; Roborazzi baselines unchanged; notes and commit. Layout changes stay out (TASK-52.2).

Revision after review/tests: the 'same day' reload skip lives in CalendarScreen (only send LoadDay when the panel shows another day), not in DayDetailViewModel, so LoadDay keeps meaning an explicit reload. Process death is simulated in tests by recreating through ActivityScenario and clearing the view models once the old instance is destroyed (an ActivityController-built activity polluted later Compose tests in the same JVM). The birthday editor keeps its once-only loads and picker guard; its draft is not kept across process death (the event editor's is).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation
- Shell: ui/navigation/ShellViewModel.kt (activity-scoped). preferences and today as StateFlows (WhileSubscribed 5s), so a recreated MainActivity/LevanaApp draws its first frame in the right theme and shell, not an empty frame in the system theme. shouldRefreshLocation()/onStop(): the resume location refresh runs on opening and on a real return to the app (once per 60s at most), never for a configuration change (each new composition gets a catch-up ON_RESUME).
- Editors: AddEditEventViewModel applies LoadEvent and PreFillDate once (the screen re-sends them on recreation, which reloaded the stored event over unsaved input, and re-applied the prefill over a picked date). ContactBirthdayScreen opens the contact picker once (saved flag); its VM already ignored a repeated LoadBirthday.
- Month: CalendarDaySheet collapses/scrolls only when the selected day changes (saved last-shown day), so an expanded panel stays expanded; CalendarScreen sends LoadDay only when the panel shows another day (no spinner flash). ZmanimViewModel ignores a repeated identical load.
- Dialogs/menus to rememberSaveable: the 8 Settings dialogs, developer date dialog and menu, My events' overflow menu and delete confirmations, the drawer's city list, the candle-lighting slider's unsaved value.
- Process death (SavedStateHandle via Koin's koinViewModel extras): CalendarViewModel (selected day, Go to date dialog; restores the selected day's month without a scroll target, so the pager's own restored page decides), AgendaViewModel (top day via new AgendaIntent.TopDayShown, search query, open date picker), AddEditEventViewModel (draft as JSON incl. its TASK-24 preserved date and the once-only flags).
- Already correct, now covered by tests/emulator: date picker session and year list (saved session + VM), Agenda list position (rememberLazyListState) and search (VM), walkthrough page and permission step (activity-scoped VM, saved pendingPermission), TASK-46 notice (saved flag + recorded key), the location ask (saved flag). Notification alarms are not rebuilt by a configuration change (only the worker and time-change receivers rebuild them); the launcher-icon refresh on start is idempotent.
- Layout: no layout code changed (TASK-52.2 owns it).

## Tests
- FoldContinuityTest (Robolectric, real MainActivity over the production Koin graph), parameterised Fold8 / Fold8 Ultra: each screen through fold -> unfold -> rotate -> unfold -> split -> unfold (recreate with FoldableSize qualifiers, view models kept): Month month/day/expanded panel; Settings Theme dialog + back stack; drawer; Agenda position and search; Zmanim date + back keeps the day; date picker year list + selection; event editor unsaved input (+ process death); process death keeps Settings over Month and the selected day. FoldContinuityZoneNoticeTest: the TASK-46 notice stays one dialog across sizes and stays dismissed.
- ScreenshotHarness: recreate(qualifiers) and restoreAfterProcessDeath() (recreate through the scenario, clear the view models when the old instance is destroyed, saved state parcelled).
- VM tests: ShellViewModelTest (location refresh gate), AddEditEventViewModelTest (repeated load/prefill keep input; draft survives process death incl. TASK-24 preserved date), CalendarTodayRefreshTest and AgendaViewModelTest (restore from SavedStateHandle), DisplayCityZmanimTest (no reload of the shown date). Existing Koin-graph tests pass the SavedStateHandle koinViewModel supplies.
- Not covered by Robolectric: the walkthrough across a resize. Composing it directly on a later page never goes idle under Robolectric (also without any recreation); on the emulator nothing redraws while idle on that page (gfxinfo frame count unchanged), and the page holds through fold/rotate/split on both AVDs.

## Verification (tested commit 34832a1, rebased on origin/master 4fbf5c4)
rm -rf app/build/test-results && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks: BUILD SUCCESSFUL; 1144 tests, 0 failures/errors/skipped; Roborazzi verify passed, no baseline changed; lint 0 errors, 82 warnings.
An earlier run (pre-fix harness) had 40 'Compose did not get idle' failures in unrelated Compose tests: test pollution from the ActivityController used for process death, bisected and fixed (see plan revision), not host load.

## Emulator (Fold8 and Fold8 Ultra AVDs, one at a time)
Scenarios (walkthrough, month, drawer, settings dialog, date picker, agenda, zmanim, editor, process death) through fold, unfold, inner rotation and split screen enter/leave: Fold8 49/49 checks, Ultra 48/49 (the one is a driver check: the split half hides the page title; the screenshot shows the right page). Before build (TASK-50 only): Settings dialog and Month panel fail all 14 checks (dialog closes, panel collapses). Location requests (each with a reverse geocode): before 6 per 3 rotations, 2 per split enter/exit, 6 per 3 fold cycles; after 0/0/0 on both devices. TASK-46 notice: one dialog through fold/unfold/rotate, gone for good after Not now. No crashes.
Evidence: /tmp/levana-task52.3-evidence/ (README.md, fold8/, fold8ultra/, fold8-before-task50-only/, task50/, scripts/, APKs, gradle-verify.log).

## Limitations / not done
- Emulators only, no real Fold8/Fold8 Ultra; process death via am kill of a backgrounded app.
- The birthday editor's unsaved input is kept across configuration changes but not across process death (its TASK-51 state is intricate; restoring part of it could save a wrong date). The date picker reopens at its starting date after process death (its session is a fresh view model).
- In the Ultra's 472dp-tall split half the walkthrough page title is below the fold: layout (TASK-52.2).
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-31T18:52:00Z

---

Coordinator integration 2026-10-31: merged as e4aced5. Clean run (load ~4): 1144 tests, 0 failures; Roborazzi 320 unchanged. FoldContinuityTest covers fold/unfold/rotation/split/process death at Fold8 and Ultra sizes; emulator 49/49 (Fold8) and 48/49 (Ultra, script check only). Known gaps: walkthrough resize not testable under Robolectric (verified on emulator); birthday editor input not restored after process death; Ultra split-half walkthrough title below the fold (layout, for TASK-52.4).

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Folding, unfolding, rotating and split screen now keep the user exactly where they were, on Fold8 and Fold8 Ultra.
- Includes TASK-50 (crash after back-to-back recreations), committed separately.
- Shell: an activity-scoped ShellViewModel keeps preferences/today across recreations (no blank or wrong-theme first frame) and stops the resume location refresh from running on configuration changes (before: a location fix + geocoder call + preference write on every fold/rotation/split).
- Editors no longer reload over unsaved input or reopen the contact picker; the expanded day panel stays expanded; day panel and Zmanim don't reload with a spinner; Settings/Events/drawer dialogs and menus stay open.
- Process death restores the back stack, the Month's selected day and Go to date dialog, the Agenda's position/search/date picker and the event editor's draft.
- Tests: FoldContinuityTest/FoldContinuityZoneNoticeTest (Robolectric recreation at the Fold8/Ultra sizes + process death) and VM tests. Full build/tests/lint/Roborazzi green (1144 tests, baselines unchanged).
- Emulator: all scenarios pass on both AVDs; before/after shows the dialog/panel losses and 6/2/6 location requests going to 0/0/0. Evidence /tmp/levana-task52.3-evidence/.
- Not done: birthday-editor draft across process death; real-device check.
<!-- SECTION:FINAL_SUMMARY:END -->
