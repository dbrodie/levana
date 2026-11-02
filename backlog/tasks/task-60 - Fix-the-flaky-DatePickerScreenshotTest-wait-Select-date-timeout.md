---
id: TASK-60
title: Fix the flaky DatePickerScreenshotTest wait ('Select date' timeout)
status: To Do
assignee: []
created_date: '2026-11-02T10:57:00Z'
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
