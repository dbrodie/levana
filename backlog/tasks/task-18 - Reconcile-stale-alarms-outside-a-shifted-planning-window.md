---
id: TASK-18
title: Reconcile stale alarms outside a shifted planning window
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-21T11:19:36Z"
updated_date: "2026-10-23T07:29:29Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-clp
priority: medium
type: bug
ordinal: 18000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — remaining findings: Reconcile alarms outside a shifted planning window (Open)

Cancel or update previously scheduled alarms that fall outside the new planning window after a date-changing time-zone move, including disabled reminders. Distinct from the completed device-zone rebuild and previous-evening Omer reconciliation.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/fix-device-timezone-reminders.md
- specs/rfes/fix-notification-lookahead.md
- specs/rfes/fix-omer-nightfall-rollover.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 After a date-changing zone move, previously scheduled alarms outside the new window are reconciled. Disabling a category removes its stale alarms too. Still-wanted overdue reminders are preserved according to the existing delivery rules.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Commit f0b9266. Durable ScheduledAlarmRegistry (DataStore scheduled_alarms; action|epochDay); scheduleAll cancels every registered unwanted identity on any date (incl. disabled categories), keeps plan alarms+overdue+due; register before set, unregister after cancel; one-time FLAG_NO_CREATE probe ±14 days for pre-registry alarms; window starts at min(device today, location today). Tests: ShiftedWindowReconciliationTest (7; westward/eastward, ±30-day clock jumps, disable after shift, overdue/location-ahead preserved, upgrade probe, restart persistence, boundedness); mutation 5/7 fail; VerifyLookaheadPathTest slide expectation updated intentionally. Emulator: -5y TIME_SET cancelled pending 2031-05-30 alarm; Tokyo->New York removed 1 Jun alarms outside new window; disabling Omer left 0 alarms (/tmp/levana-emu-h3rx7pgl/artifacts/04-*, 12-*, 13-*, 14-*, 18-*). Limitations: probe range; filed levana-vdq (forward jump race) and levana-kq3 (kept-overdue stale trigger). Branch fix/notification-dates-travel-omer (worktree /Users/daniel/.paseo/worktrees/15o7vaq5/notification-dates-travel-omer). Spec b9d6047/635484c, verification record dca60e7 (specs/rfes/fix-notification-dates-travel-omer.md). Gates: testDebugUnitTest 608 tests/84 suites 0 failures/errors/skips; lintDebug pass; assembleDebug pass. Emulator emulator-5554 (session /tmp/levana-emu-h3rx7pgl, APK sha256 4283bec7...d3d2). Status: implemented/verified awaiting parent integration.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-clp",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-23T06:46:26Z",
  "closed_at": "2026-10-23T07:29:29Z",
  "close_reason": "Reviewed, verified and merged/pushed to origin/master at 94df4b4. Fresh parent-run debug unit suite: 608 tests, zero failures/errors/skips; lintDebug and assembleDebug passed. Emulator evidence inspected at /tmp/levana-emu-h3rx7pgl/artifacts, including real alarm delivery, timezone/system-clock broadcasts, process-restart delivery dedup and English/Hebrew card changes. Registry fixes all registered out-of-window alarm identities; legacy alarms beyond the one-time 14-day upgrade probe remain a documented limitation. Separate existing clock-jump/old-zone-delivery findings tracked in levana-vdq and levana-kq3.",
  "external_ref": "specs-import:followup-shifted-window",
  "metadata": {
    "import_key": "followup-shifted-window",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — remaining findings: Reconcile alarms outside a shifted planning window (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 2
}
```
