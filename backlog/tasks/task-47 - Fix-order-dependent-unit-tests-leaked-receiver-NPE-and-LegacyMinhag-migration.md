---
id: TASK-47
title: 'Fix order-dependent unit tests: leaked receiver NPE and LegacyMinhag migration'
status: To Do
assignee: []
created_date: '2026-10-30T00:47:00Z'
labels: []
dependencies: []
priority: low
type: bug
ordinal: 51000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-12.3 (2026-10-30). The JVM unit suite can fail depending on class order. Gradle runs previously failed classes first, so one failure can repeat on the next run. Neither failure involves the app shell.

1. VerifyReschedulingEndToEndTest (lines ~394/397) calls `NotificationAlarmReceiver().onReceive(app, …)` directly. Under Robolectric, `goAsync()` returns null, so `pendingResult.finish()` throws an NPE on Dispatchers.Default after the test. The next Compose test (createEmptyComposeRule / runTest) then fails with `UncaughtExceptionsBeforeTest`. Seen on AppShellScreenshotTest.editEvent[EN_LIGHT] and LocationsEmptyScreenshotTest.
2. LegacyMinhagPreferenceTest writes legacy `is_in_israel`/`is_in_israel_manual` keys into the JVM-wide preferences DataStore. It expects IsraelModeMigration to convert them. DataStore runs migrations only on its first read, so the test passes only when it is the first test in the JVM to read preferences. Otherwise `isInIsrael` is false and it fails with `AssertionError: SEPHARDI`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Neither test leaks state or depends on running first; the suite passes in any class order (e.g. with the screenshot tests forced first)
<!-- AC:END -->
