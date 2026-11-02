---
id: TASK-61
title: 'Event editor: input made before the stored event loads is lost (picker closes)'
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-02T11:20:00Z'
updated_date: '2026-11-02T13:43:00Z'
labels: []
dependencies: []
priority: low
type: bug
ordinal: 101000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-60 (2026-11-02). Opening a stored event shows the editor at once with the default date (1 Tishrei of the current year) and an empty title. The title bar says Edit event, taken from the route. AddEditEventViewModel.loadEvent then replaces the whole AddEditEventState when Room answers. Anything done before that is lost: an open date picker closes (showDatePicker resets to false), and a typed title or a picked date is overwritten. The picker also opens on the default date rather than the stored one.

On a device the load usually takes milliseconds, so this is rare, but a slow disk or a fast tap can hit it. It was the root cause of the TASK-60 screenshot flake; the tests now wait for the load, which hides it in tests only.

ContactBirthdayViewModel already handles the same case: fields edited while loading win (TASK-51). Options: do not allow opening the picker until the event has loaded (show the fields as loading), or keep the UI-only state and fields the user changed while loading, like the birthday editor does.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Opening the date picker or editing a field before a stored event has loaded never loses the input or closes the picker, and the picker never opens on a date other than the stored one; a ViewModel test covers a slow load
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Design (owner decision 2026-11-02: explicit Loading / Editing):
1. State: AddEditEventState gets isLoading. The view model decides its starting phase at construction from its SavedStateHandle, which already holds the route (AddEditEventRoute via the nav back-stack entry; SavedStateHandle.toRoute):
   - a saved draft (TASK-52.3 process death) -> Editing with the draft; the stored event is never loaded over it;
   - otherwise a route eventId > 0 -> Loading, and the view model loads it once (init), then switches to Editing with the stored values;
   - otherwise (new event) -> Editing with the defaults.
   The LoadEvent intent and the screen's LaunchedEffect(eventId) go: the load is no longer screen-triggered, so recomposition, a fold/rotation or a re-sent intent cannot reload. The first frame is already Loading (no one-frame flash of the default form).
2. While loading, every input intent (title, notes, yahrzeit toggle, open/confirm/dismiss picker, prefill, Save) is ignored; the draft is not written while loading, so a process killed mid-load loads again rather than restoring an empty form.
3. Missing/deleted event: keep today's behaviour (the load finds nothing -> the form with the defaults, saved as a new event). Noted in the task.
4. Screen: while isLoading, a loading box replaces the whole form (no fields, no picker). The spinner appears only after ~300 ms so a normal millisecond load shows no flash; when shown it has a 'Loading event' label in a polite live region (EN + HE strings). If the spinner was shown, focus moves to the form's first element once it loads, so TalkBack lands on the form.
5. Tests (AddEditEventViewModelTest): existing tests open stored events through the route SavedStateHandle instead of LoadEvent. New tests with a gated (suspending) DAO: Loading at start, no input accepted and the picker can't open while loading, stored values after the load, edits after the load never overwritten (incl. another loading completion attempt), the restored draft beats the load (the DAO is never asked), new events never load, a missing event ends in Editing with defaults, killed mid-load loads again. Roborazzi: none expected to change (screenshots after the load); TASK-60's harness wait kept.
6. Figma: add a small 'Edit event – loading' frame next to the event editor on the Screens pages.
7. Verify: assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks from clean test-results; emulator: open and edit a stored event, rotation and process death keep the draft, and an uncommitted debug-only slowed load shows the loading state.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Owner decision 2026-11-02: use the standard explicit-loading architecture. The editor's state is Loading or Editing. While loading a stored event, the screen shows a brief loading indicator in place of the form (no input, no picker). The load runs once and switches to Editing with the stored values; after that, only user intents change the state, and nothing reloads over it. A restored draft (TASK-52.3 process-death restore) starts directly in Editing and is never overwritten. New events start in Editing with the defaults.
<!-- SECTION:NOTES:END -->
