---
id: TASK-38
title: >-
  Port independent verifier tests for Omer nightfall, time-zone and rescheduling
  fixes
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T10:50:00Z'
updated_date: '2026-10-29T10:58:00Z'
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
- [ ] #1 Ported tests compile and pass on current master
- [ ] #2 No ported test uses reflection or non-public APIs
- [ ] #3 Full build, unit test and lint command from AGENTS.md passes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Port VerifyOmerNightfallRolloverTest (44309ce), VerifyTimeZoneCrossingTest and VerifyTimeZoneSingletonTest (f82161e), VerifyReschedulingEndToEndTest and VerifyProductionObserverTest (9bb8534) into app/src/test/java/ninja/doskey/app/levana/notifications/.
2. Drop any test that relies on non-public APIs (androidx.work.impl.*) or reflection, along with helpers, imports and files left unused. Public Robolectric shadows stay. Do not port the markdown reports.
3. Change the westward time-zone test that documents the duplicate Omer morning notice to assert the behavior fixed in 94df4b4, and rename it.
4. Adapt only what current master APIs require, without weakening assertions. A test that fails because of a real production bug is left out and filed as its own task.
5. Verify with the AGENTS.md build, unit test and lint command. Test-only change, so no emulator run.
<!-- SECTION:PLAN:END -->
