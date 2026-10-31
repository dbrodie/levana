---
id: TASK-52.3
title: 'Fold/unfold continuity: same screen, preserved state, correct relayout'
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T10:31:00Z'
updated_date: '2026-10-30T12:26:00Z'
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
- [ ] #1 Fold/unfold, rotation and split-screen keep the same screen, back stack and in-progress state on every screen listed; the layout matches the new size
- [ ] #2 No duplicate dialogs, prompts or walkthrough on config change; tests cover state restoration for key screens; emulator evidence (screen recording or screenshots before and after) recorded
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
<!-- SECTION:PLAN:END -->
