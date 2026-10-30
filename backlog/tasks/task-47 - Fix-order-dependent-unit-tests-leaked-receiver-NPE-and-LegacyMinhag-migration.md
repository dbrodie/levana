---
id: TASK-47
title: 'Fix order-dependent unit tests: leaked receiver NPE and LegacyMinhag migration'
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T00:47:00Z'
updated_date: '2026-10-30T05:39:00Z'
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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Reproduce each failure by forcing the bad order with a temporary JUnit Suite class (run via --tests '*Suite'), plus TZ override for the host-zone dependency; record the commands.
2. Receiver leak: drive NotificationAlarmReceiver through a real Robolectric broadcast (registered receiver + sendBroadcast) and wait on its ShadowBroadcastPendingResult future, like VerifyTimeZoneCrossingTest does for TimeChangeReceiver, so goAsync() is non-null and the coroutine finishes inside the test. Production receivers are not hardened: the system always supplies a PendingResult when it calls onReceive, so a null there only happens with direct test calls.
3. LegacyMinhagPreferenceTest: findings show the migration never sees the legacy keys (store.edit initialises DataStore first). The test passes only when israel_mode is unset and the host zone is Asia/Jerusalem (AUTO). Write the live israel_mode key instead of the pre-TASK-5.3 is_in_israel keys; IsraelModeTest already covers the migration itself.
4. Add one shared test helper (data/TestPreferenceStore.kt) that reaches the JVM-wide preferences DataStore and clears it; use it in ScreenshotHarness and LegacyMinhagPreferenceTest instead of their own reflection.
5. Audit the other tests for the same patterns (direct onReceive with goAsync, reliance on shared DataStore state or migrations, escaping coroutine exceptions); fix or list them.
6. Prove order-independence: rerun the bad orders, run the full suite 3x with clean results and --rerun-tasks, run a suite with screenshot classes first, then the final check with verifyRoborazziDebug (116 unchanged).
<!-- SECTION:PLAN:END -->
