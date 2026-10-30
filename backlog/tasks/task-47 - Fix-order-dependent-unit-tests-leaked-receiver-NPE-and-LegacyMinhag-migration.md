---
id: TASK-47
title: 'Fix order-dependent unit tests: leaked receiver NPE and LegacyMinhag migration'
status: Done
assignee:
  - '@claude'
created_date: '2026-10-30T00:47:00Z'
updated_date: '2026-10-30T06:11:00Z'
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
- [x] #1 Neither test leaks state or depends on running first; the suite passes in any class order (e.g. with the screenshot tests forced first)
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

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Reproduction (base 77b3556, before fixes):
- Ordering tool: /tmp/task47/scripts/{gen_suite.py,run_order.sh,summarize.py} (outside the worktree). run_order.sh <order> generates a temporary JUnit Suite (app/src/test/.../order/OrderSuite.kt, deleted after each run, never committed) listing all 109 test classes in the chosen order (forward | reversed | shuffleN | screenshots-first), runs it in one JVM with --tests 'ninja.doskey.app.levana.order.OrderSuite', and summarizes the XML results. The class list is in /tmp/task47/classes.txt.
- reversed: LegacyMinhagPreferenceTest fails with AssertionError: SEPHARDI (821 tests, 1 failed).
- screenshots-first: LegacyMinhagPreferenceTest fails as above, and CalendarModeSelectorTest.choosingOtherOptionReportsItOnce fails with UncaughtExceptionsBeforeTest. The suppressed causes are 2x NPE 'PendingResult.finish() because this.$pendingResult is null' at NotificationAlarmReceiver.kt:29 (821 tests, 2 failed).
- Two-class suite [VerifyReschedulingEndToEndTest, LegacyMinhagPreferenceTest] also fails LegacyMinhag. Running VerifyReschedulingEndToEndTest twice in one JVM failed receiverOnReceiveReadsPersistedPrefs on the second pass ('morning omer posted' timeout): the JVM-wide NotificationDeliveryLog had already recorded that Omer notice as delivered. This is a third shared-state dependency.
- LegacyMinhag alone: passes with host TZ Asia/Jerusalem, but fails alone with TZ=America/New_York ./gradlew ... --tests '*LegacyMinhagPreferenceTest'.

Root causes:
1. VerifyReschedulingEndToEndTest called NotificationAlarmReceiver().onReceive directly, so goAsync() returned null. The coroutine on Dispatchers.Default outlived the test and NPE'd in finish(); kotlinx-coroutines-test then reported the NPE in the next runTest/Compose test.
2. LegacyMinhagPreferenceTest is not really migration-order dependent. Its store.edit() initialises DataStore before the legacy keys exist, so IsraelModeMigration never runs on them in any order. The test was written (e41d6d4) while is_in_israel was a live key; TASK-5.3 (144509c) replaced it with israel_mode. After that, the test passed only when israel_mode was unset (AUTO) and the host zone was Asia/Jerusalem. Tests such as VerifyReschedulingEndToEndTest leave israel_mode=DIASPORA behind, which fails it.

Fix (commit 681fb6a):
- VerifyReschedulingEndToEndTest.receiverOnReceiveReadsPersistedPrefs delivers through deliver(intent). It registers a NotificationAlarmReceiver, sends the broadcast, idles the main looper and waits for the ShadowBroadcastPendingResult future, the same pattern as VerifyTimeZoneCrossingTest. goAsync() is non-null, finish() runs inside the test, and the assertions are now synchronous instead of sleep/poll. The test clears the JVM-wide delivery log first.
- LegacyMinhagPreferenceTest writes israel_mode=ISRAEL, the live key, instead of is_in_israel/_manual. It clears the store in @Before and @After. IsraelModeTest still covers IsraelModeMigration.migrate itself.
- New shared test helper app/src/test/.../AppDataStores.kt: preferences(), deliveryLog(), clearPreferences(), clearDeliveryLog(). It reaches the private process-wide delegates by reflection in one place and fails loudly if a delegate is renamed. ScreenshotHarness and LegacyMinhag use it; their own reflection copies are removed.
- Production code unchanged, so no emulator check. NotificationAlarmReceiver (and Boot/DateChange/TimeChange receivers) are not hardened against a null goAsync(): the framework always supplies a PendingResult when it dispatches onReceive, so null only comes from direct calls outside the framework. A null-safe finish() would hide that misuse in tests. The alternative of making the DataStore delegates internal and dropping the reflection was left out to avoid a production change. It is a reasonable follow-up if wanted.

Audit of the same patterns:
- Direct onReceive: the only other call, VerifyTimeZoneCrossingTest.unrelatedActionIsIgnoredWithoutGoingAsync, returns before goAsync (it asserts wentAsync=false). Safe.
- Test-owned CoroutineScopes (VerifyReschedulingEndToEnd, DailyNotificationWorker, IntegrationVerifierSettingsFlow, EventDataChanges, ProviderIo, DeviceTodayUpdates, CalendarMonthSwipe, VerifyCandidateIo, VerifyCancelledCallerError, IntegrationVerifierSupersededLoad) are all cancelled in the test or teardown. None was seen to leak in any order run. Not changed.
- Shared DataStores: about 20 notification test classes use the production delivery log or alarm registry (NotificationDeliveryLog(app), NotificationAlarmScheduler(app)), and about 16 use the shared preferences store. None failed in the 5 whole-suite orders below, so they were left as is. The residual risk is a new test reusing a date or Omer day already recorded by another test. AppDataStores.clearDeliveryLog/clearPreferences is the remedy.

Verification (tested commit 681fb6a):
- Bad orders after the fix: forward, reversed, screenshots-first (screenshot and widget snapshot classes first), shuffle1, shuffle2 via run_order.sh: each 821 tests, 0 failed (/tmp/task47/matrix1.txt, summary-*.txt).
- TZ=America/New_York --tests LegacyMinhag + VerifyReschedulingEndToEnd: pass.
- Full suite 3x with rm -rf app/build/test-results and --rerun-tasks: 821/0 each (/tmp/task47/full-{1,2,3}.log).
- Final: assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks: BUILD SUCCESSFUL, 821/0 (/tmp/task47/final.log). git status clean afterwards: no screenshot changed. The repo tracks 118 PNGs under app/src/test/screenshots, not 116; all are unchanged.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-30T06:11:00Z

---

Coordinator integration 2026-10-30: test-only changes reviewed; merged as 4e75b06. Clean run (test-results deleted, --rerun-tasks): 821 tests, 0 failures; Roborazzi 116 unchanged.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Unit tests no longer depend on class order. VerifyReschedulingEndToEndTest sends alarms as real broadcasts and waits for goAsync, so no NPE leaks into later Compose tests. LegacyMinhagPreferenceTest uses the live israel_mode key from an empty store; it had depended on host TZ Asia/Jerusalem and an unset israel_mode, and the migration never actually ran in that test. The new AppDataStores test helper centralises access to and clearing of the app's process-wide DataStores, and ScreenshotHarness uses it. No production change. Verified in 5 whole-suite orders, 3 clean full runs and the final check including verifyRoborazziDebug (all 821 tests pass; screenshots unchanged).
<!-- SECTION:FINAL_SUMMARY:END -->
