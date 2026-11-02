---
id: TASK-60
title: Fix the flaky DatePickerScreenshotTest wait ('Select date' timeout)
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-02T10:57:00Z'
updated_date: '2026-11-02T11:00:00Z'
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
- [ ] #1 Root cause identified and fixed without retries; the harness no longer depends on a short real-time timeout
- [ ] #2 DatePickerScreenshotTest passes ≥ 20 consecutive runs and the full suite 3 times from clean, including under emulator load; Roborazzi baselines unchanged
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Reproduce: run DatePickerScreenshotTest repeatedly (isolated, --rerun-tasks) and inside the full suite; on a timeout dump diagnostics (semantics tree of all roots incl. dialog windows, window/dialog state, main looper queue and fake clock, picker view-model state, thread dump) to a dir outside the worktree; check order dependence.
2. Identify the root cause from that evidence (why the 'Select date' title is not composed within 10 s: dialog window, idling/frames, or an async load such as the picker's preferences read).
3. Fix the cause in the harness (or production code if it is a real bug): drive Compose/looper idling deterministically rather than wall-clock polling; make dialog roots discoverable; keep one central, generous timeout with a diagnostic failure message as a backstop only.
4. Audit the other wait sites (awaitGone, launch retry, FoldContinuityTest, VerifyReactiveRefreshEndToEndTest.awaitUntil) and fix those sharing the cause.
5. Prove stability: DatePickerScreenshotTest >= 20 consecutive forced runs; full suite 3x from clean app/build/test-results, one with an emulator running; Roborazzi baselines unchanged; final assemble/test/lint/verifyRoborazzi --rerun-tasks. Record root cause, fix, counts, evidence paths (/tmp/task60) in notes.
<!-- SECTION:PLAN:END -->
