---
id: TASK-38
title: >-
  Port independent verifier tests for Omer nightfall, time-zone and rescheduling
  fixes
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T10:50:00Z'
updated_date: '2026-10-29T10:59:00Z'
labels:
  - test
dependencies: []
priority: medium
ordinal: 37000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Port the JVM verifier suites left on local branches into master's test tree (app/src/test/java/ninja/doskey/app/levana/notifications/):
- VerifyOmerNightfallRolloverTest.kt (commit 44309ce, branch verify-omer-nightfall-rollover)
- VerifyTimeZoneCrossingTest.kt, VerifyTimeZoneSingletonTest.kt (commit f82161e, branch verify-device-timezone-reminders)
- VerifyReschedulingEndToEndTest.kt, VerifyProductionObserverTest.kt (commit 9bb8534, branch verify-notification-rescheduling)

User decision: drop any test that uses a non-public API (e.g. androidx.work.impl.WorkManagerImpl) or reflection, plus helpers or files left unused. Do not port the markdown verification reports. The westward documentsRepeat test asserts the repeat fixed in 94df4b4 (former Beads levana-1bl); assert the fixed behavior instead.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Ported tests compile and pass on current master
- [x] #2 No ported test uses reflection or non-public APIs
- [x] #3 Full build, unit test and lint command from AGENTS.md passes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Port VerifyOmerNightfallRolloverTest (44309ce), VerifyTimeZoneCrossingTest and VerifyTimeZoneSingletonTest (f82161e), VerifyReschedulingEndToEndTest and VerifyProductionObserverTest (9bb8534) into app/src/test/java/ninja/doskey/app/levana/notifications/.
2. Drop any test that relies on non-public APIs (androidx.work.impl.*) or reflection, along with helpers, imports and files left unused. Public Robolectric shadows stay. Do not port the markdown reports.
3. Change the westward time-zone test that documents the duplicate Omer morning notice to assert the behavior fixed in 94df4b4, and rename it.
4. Adapt only what current master APIs require, without weakening assertions. A test that fails because of a real production bug is left out and filed as its own task.
5. Verify with the AGENTS.md build, unit test and lint command. Test-only change, so no emulator run.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Tested commit: 8797360 (branch port-verifier-tests, rebased onto origin/master a757c9e). Plan is in this task (no RFE file, per updated AGENTS.md).

Verification: set -a && . ./.env && set +a && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug -> BUILD SUCCESSFUL. Unit tests 608 before -> 651 after, 0 failures, 0 skipped. A second full unit run with --rerun-tasks also passed on the pre-rebase commit e28cf6c (identical test files). No emulator (test-only change).

Ported (43 tests):
- VerifyOmerNightfallRolloverTest (44309ce): 25 tests, unchanged.
- VerifyTimeZoneCrossingTest (f82161e): 13 tests. Three tests recorded pre-fix behavior and now assert master's fixed behavior:
  - westward_..._replansSameDay_documentsRepeat -> westward_beforeNyMorning_afterMoscowDelivery_replansSameDayWithoutReposting: Fri is still replanned at 07:00 NY, but delivering it through deliverAlarm with a NotificationDeliveryLog posts nothing; the next day still posts (fix 94df4b4, levana-1bl; mirrors OmerTravelDeliveryTest). Uses a TestDataStore delivery log.
  - westward_dateCrossesBackward_lastOldWindowDayFixedOnNextRebuild -> ..._cancelsLastOldWindowDayAndNextRebuildReplansIt: Sat+7 alarm at 07:00 Moscow is now cancelled by registry reconciliation (levana-clp, see ShiftedWindowReconciliationTest) instead of left pending; next rebuild still plans it at 07:00 NY.
  - manualClockJumpForward_timeSetMovesTheWindow -> ..._AndCancelsSkippedDays: Fri's past out-of-window alarm is now cancelled, not left to fire late (same levana-clp behavior).
- VerifyTimeZoneSingletonTest (f82161e): 1 of 3 tests kept (singletonCreatedInNewYorkPlansMorningAlarmsInMoscowAfterZoneChange).
- VerifyReschedulingEndToEndTest (9bb8534): 4 tests. Adaptations: removed saveMinhag/Minhag (deleted from master in e41d6d4); alarm lookups and per-date counts scoped to the test date since master plans an 8-day window (the source assumed one alarm per category); 'only candle lighting' checks compare the action set across all dates; the unrelated-prefs sentinel compares against the planned count and checks the Omer sentinel is cancelled after the Israel change; setUp resets isInIsrael and tearDown restores shared DataStore prefs so state does not leak into other suites.

Removed:
- VerifyProductionObserverTest (whole file, single test): uses androidx.work.impl.WorkManagerImpl.setDelegate.
- VerifyTimeZoneSingletonTest.timeZoneChangedBroadcastReconcilesWithoutUiOrRestart and timeSetBroadcastReconciles plus helper assertBroadcastMovesMorningAlarms: instantiate receivers through Class.forName(...).getDeclaredConstructor() reflection. Broadcast handling is still covered by VerifyTimeZoneCrossingTest via TimeChangeReceiver directly. Unused imports and the stopKoin teardown removed with them.
- Markdown verification reports not ported.

No production bugs found; no new tasks filed.
<!-- SECTION:NOTES:END -->
