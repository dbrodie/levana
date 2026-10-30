---
id: TASK-53
title: Make screenshot and end-to-end test waits robust under host load
status: To Do
assignee: []
created_date: '2026-10-30T11:28:00Z'
labels: []
dependencies: []
priority: medium
type: chore
ordinal: 61000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while integrating TASK-51 (2026-10-30). With the host busy (an agent's emulators running, load average ~7), full runs failed 1–3 tests each time, always on wall-clock timeouts, never on image differences:
- DatePickerScreenshotTest: 'Timed out waiting for Select date' (outOfRangeStoredDate, leapYearAdarIIWithAHolidayChip; EN/HE, light/dark);
- VerifyReactiveRefreshEndToEndTest: 'timed out: instance shown'.

The same classes pass in isolation. ScreenshotHarness.await uses a fixed 10 s real-time timeout (AWAIT_TIMEOUT_MILLIS), and the end-to-end test has its own awaitUntil. Since coordinators run full suites while agents run emulators, these timeouts make integration unreliable.

Fix the root cause:
- Prefer the Compose test clock or idling over wall-clock polling where possible.
- Otherwise use a generous, centrally configured timeout that scales under CI or load. Give clear failure messages.
- Look for other wall-clock waits in tests.

Do not hide real failures with retries.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Waits in the screenshot harness and the end-to-end tests no longer depend on a short real-time timeout; the full suite passes three times in a row while an emulator runs alongside (load comparable to ~7)
- [ ] #2 The approach is documented in ARCHITECTURE_SPEC's testing section
<!-- AC:END -->
