---
id: TASK-60
title: Fix the flaky DatePickerScreenshotTest wait ('Select date' timeout)
status: Done
assignee:
  - '@claude'
created_date: '2026-11-02T10:57:00Z'
updated_date: '2026-11-02T12:24:00Z'
labels: []
dependencies: []
priority: medium
type: bug
ordinal: 100000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`DatePickerScreenshotTest` (e.g. leapYearAdarIIWithAHolidayChip, outOfRangeStoredDate) intermittently fails with "Timed out waiting for Text + EditableText contains 'Select date'". It happened again on 2026-11-02 at a normal host load (load average ~4.6, laptop awake), so it's a real flake, not only host sleep. It passes on rerun. The cause is `ScreenshotHarness.await` / `awaitOrFalse`, which polls with a fixed **10 s wall-clock** timeout (`AWAIT_TIMEOUT_MILLIS`, using System.currentTimeMillis and Thread.sleep), while opening the picker dialog depends on composition and dialog-window timing under Robolectric.

Find the root cause (why the dialog sometimes takes longer than 10 s to show its title: dialog window creation, idle detection, frame advancing in the harness) and fix it properly:
- prefer driving the Compose test clock or idling resources over real-time polling;
- otherwise use a centrally configured, generous timeout with clear diagnostics;
- check other harness wait sites (awaitGone, FoldContinuityTest, VerifyReactiveRefreshEndToEndTest's awaitUntil) for the same pattern.

Don't hide real failures with retries. Prove stability: run `DatePickerScreenshotTest` ≥ 20 times and the full suite 3 times from a clean `app/build/test-results`, all green, including once with an emulator running alongside.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Root cause identified and fixed without retries; the harness no longer depends on a short real-time timeout
- [x] #2 DatePickerScreenshotTest passes ≥ 20 consecutive runs and the full suite 3 times from clean, including under emulator load; Roborazzi baselines unchanged
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Reproduce: run DatePickerScreenshotTest repeatedly (isolated, --rerun-tasks) and inside the full suite; on a timeout dump diagnostics (semantics tree of all roots incl. dialog windows, window/dialog state, main looper queue and fake clock, picker view-model state, thread dump) to a dir outside the worktree; check order dependence.
2. Identify the root cause from that evidence (why the 'Select date' title is not composed within 10 s: dialog window, idling/frames, or an async load such as the picker's preferences read).
3. Fix the cause in the harness (or production code if it is a real bug): drive Compose/looper idling deterministically rather than wall-clock polling; make dialog roots discoverable; keep one central, generous timeout with a diagnostic failure message as a backstop only.
4. Audit the other wait sites (awaitGone, launch retry, FoldContinuityTest, VerifyReactiveRefreshEndToEndTest.awaitUntil) and fix those sharing the cause.
5. Prove stability: DatePickerScreenshotTest >= 20 consecutive forced runs; full suite 3x from clean app/build/test-results, one with an emulator running; Roborazzi baselines unchanged; final assemble/test/lint/verifyRoborazzi --rerun-tasks. Record root cause, fix, counts, evidence paths (/tmp/task60) in notes.

Revision after reproduction: the cause was an event-editor load race, not slowness. The fix makes the harness's waits include Room work (a counting query executor) and has the editor test wait for its loaded fields. Compose's Robolectric idling does not consult registered IdlingResources, so the harness checks the counter itself. The production race is filed as TASK-61.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Reproduction and root cause (base 8518864):
- Isolated runs (4 forced runs, then a 300-iteration one-JVM stress of the add-event picker path) and one full suite (1202/0) passed: the flake is rare at normal load. Earlier failures from other agents' sessions (task-51, task-57.8, majestic-sheep transcripts) were all in editEventOn (leapYearAdarIIWithAHolidayChip, outOfRangeStoredDate), never in the add-event or Go to date tests. One failing test took 10.177 s, the full wait: the title never appeared, it was not just slow.
- Mechanism (temporary probe test plus a 3 s delay on the test DB's query executor, neither committed; /tmp/task60/probe-evidence.txt): the editor opens showing the default date (1 Tishrei 5787) because the 'Edit event' title comes from the route, not from the loaded state. The test clicked the date field and the picker opened. When getById landed, AddEditEventViewModel.loadEvent replaced the whole state (showDatePicker=false) and the dialog closed (compose roots 2 -> 1). The picker's own content waits for a DataStore read (DatePickerViewModel.open), so if the editor load lands between the click and that read, the dialog closes before 'Select date' is ever composed, and the wait times out. Load depends on host load and Room/DataStore thread timing, not on test order.
- Compose's waitForIdle under Robolectric (RobolectricIdlingStrategy) checks only ComposeIdlingResource and Espresso onIdle; idling resources registered on the rule were not waited for (verified with the probe), so it is no help for Room work.

Fix (commit f891eb0, test-only):
- DatabaseIdling: the harness DB's query and transaction executor, counting queries in flight.
- ScreenshotHarness: await/awaitGone go through one awaitUntil(description, condition). It waits until the condition holds and Room is idle, then for Compose idle, re-checking Room once more. AWAIT_TIMEOUT_MILLIS is 60 s (was 10 s) and only bounds a wait that never ends. The failure message includes HarnessDiagnostics (all Compose roots with their full unmerged semantics trees, so dialogs show; window views; the main looper; the app's threads). Sample: /tmp/task60/diagnostics-sample.txt. launch() keeps its 3 s first-content attempt (pre-existing relaunch for an empty first frame, unchanged) without the Room condition, then waits for the launch's queries.
- DatePickerScreenshotTest.editEventOn waits for the stored title in the editable field before opening the picker.
- FoldContinuityTest.awaitShown uses awaitUntil (its own 10 s wall-clock loop is gone).
- VerifyReactiveRefreshEndToEndTest.awaitUntil is unchanged. It polls view-model state directly, with no UI action made before a load, so the race does not apply; it has not been seen to flake.
- Production race filed as TASK-61. Input before the stored event loads is lost on devices too, but rarely.

Verification (tested commit f891eb0):
- With the 3 s DB delay: the editor shows 14 Adar II 5787 before the click and the picker stays open (400 frames).
- DatePickerScreenshotTest 20/20 consecutive runs, each ./gradlew :app:testDebugUnitTest --tests '*DatePickerScreenshotTest' --rerun-tasks from deleted test-results, 16/16 tests each (/tmp/task60/stable-iso/summary.txt, xml-N/).
- Full suite 3x, each rm -rf app/build/test-results and --rerun-tasks: 1202 tests, 0 failed each. Run 1 ran with a headless Levana_Agent_Template_API_36_1 emulator running the app under a monkey loop, host load 7-10.9 (/tmp/task60/full-1-emulator*, full-2*, full-3*, full-summary.txt). The emulator was stopped afterwards.
- Final: assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks: BUILD SUCCESSFUL, 1202/0, lint 0 errors (83 warnings), Roborazzi verify passed; git status clean, no baseline changed (422 tracked PNGs) (/tmp/task60/final.log.keep).
- Limitation: the original timeout was not reproduced naturally in this session (it is rare). The mechanism was shown with an injected DB delay, and every recorded failure matches it.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-11-02T12:24:00Z

---

Coordinator integration 2026-11-02: test-only changes; merged as 08de8b6. Root cause: editor-load race (stored event load replaced editor state and closed the picker), not slowness. Clean run: 1202 tests, 0 failures; Roborazzi 420 unchanged. Agent evidence: 20/20 isolated, 3/3 full suite (one under emulator load). App-side race filed as TASK-61.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The DatePicker flake was a race, not slowness. The editor tests clicked the date field before the stored event loaded; the load replaced the editor state and closed the picker, so 'Select date' never appeared. The screenshot harness now runs Room on a counting executor and its waits hold until Room is idle. The editor test waits for its loaded fields. All waits share one 60 s bound with diagnostic failures. Test-only change; baselines unchanged. Verified with 20/20 isolated runs, 3 clean full suites (one under emulator load) and the final check. The production race (input before the load is lost) is TASK-61.
<!-- SECTION:FINAL_SUMMARY:END -->
