---
id: TASK-61
title: 'Event editor: input made before the stored event loads is lost (picker closes)'
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-02T11:20:00Z'
updated_date: '2026-11-02T13:56:00Z'
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
- [x] #1 Opening the date picker or editing a field before a stored event has loaded never loses the input or closes the picker, and the picker never opens on a date other than the stored one; a ViewModel test covers a slow load
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

## Implementation (commit ee6d442)
- AddEditEventState.isLoading. AddEditEventViewModel decides its start in init from its SavedStateHandle: a saved draft of this route -> Editing with the draft (the stored event is never loaded); else the route's eventId (SavedStateHandle.toRoute<AddEditEventRoute>(), filled by navigation through Koin's extras) > 0 -> Loading, loaded once, then Editing with the stored values; else Editing with the defaults. The LoadEvent intent and the screen's LaunchedEffect(eventId) are gone, so recomposition, folds/rotation or a re-sent intent cannot reload. The first frame of an edit is already Loading.
- While loading, onIntent drops every intent (title, notes, yahrzeit, picker open/confirm/dismiss, prefill, Save). The draft is not written while loading, so a process killed mid-load loads the event again; a draft is only used if it belongs to the route's event.
- Missing/deleted event: unchanged from before — the load finds nothing, the editor shows the defaults (title bar still 'Edit event', from the route) and Save adds it as a new event. Covered by a test.
- Screen: while loading, EventLoading replaces the whole form (no fields, no picker). The indicator (CircularProgressIndicator + 'Loading event…' / 'האירוע נטען…', polite live region, merged) appears only after 300 ms, so a normal load shows no flash and TalkBack announces only a visible wait. If it was shown, focus moves to the form's first line once loaded (FocusRequester on the hint text).
- Birthday editor untouched (no shared code).

## Tests
- AddEditEventViewModelTest: existing tests open stored events through the route SavedStateHandle; new gated-DAO tests: aStoredEventOpensLoadingAndLoadsOnce, noInputIsAcceptedWhileLoading (incl. picker can't open, Save ignored, picker then opens on the stored date), editsAfterTheLoadAreNeverOverwritten, aRestoredDraftBeatsTheLoad (DAO never asked), aProcessKilledWhileLoadingLoadsAgain, aNewEventHasNoLoadingState, aMissingEventOpensTheEmptyForm. 40/40.
- TASK-60 harness wait kept unchanged; DatePickerScreenshotTest passes in the full run.

## Verification (tested commit ee6d442)
- rm -rf app/build/test-results; assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks: BUILD SUCCESSFUL; 1225 tests, 0 failures/errors; lint 0 errors (83 warnings, as before); Roborazzi verify passed, no baseline changed (git status clean). Log: /tmp/task61/full.log
- Emulator (Levana_Agent_Template_API_36_1, headless): created an event, opened it from My events -> Events: loads normally with stored values ('Update'); edited notes, rotation to landscape and back kept the draft; opened the date picker, HOME + am kill + relaunch: the picker and the edited notes were restored, Update saved.
- Slowed load (uncommitted 3 s delay in loadEvent, debug build only, reverted; APK /tmp/task61/emu/app-debug-slowload.apk): the editor showed the 'Loading event…' indicator in place of the form; a tap where the date field would be did nothing; after the load the stored values showed, no picker opened, and focus was on the form's first line ('Choose the Hebrew date for this event.').
- Evidence: /tmp/task61/emu/artifacts (03-edit-loaded, 04-rotated, 05-after-process-death, 06-draft-after-process-death, 07-slow-loading, 08-slow-loaded).
- Figma: added 'Events / Edit event — loading' frames to section 05 on Screens — Light (98:6873), Dark (98:15728), and the Hebrew key-screens section (98:15776, 'עריכת אירוע' / 'האירוע נטען…'); the sections were widened to fit.
- Limitations: TalkBack itself was not run on the emulator; the focus move was checked through the UI hierarchy's focused node.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The event editor now has an explicit Loading / Editing state. A stored event loads once in the view model (from the route in its SavedStateHandle); until it loads, the screen shows a delayed loading indicator instead of the form and the view model ignores all input, so nothing typed or picked can be lost and the picker opens on the stored date. A restored draft starts in Editing and is never overwritten; new events never load. Missing events keep today's behaviour (empty form, saved as new). New ViewModel tests use a slow, gated load. Full check green (1225 tests, lint, Roborazzi unchanged); emulator checked normal edit, rotation, process death and a temporarily slowed load. Figma has the new loading frame (Light, Dark, Hebrew).
<!-- SECTION:FINAL_SUMMARY:END -->
