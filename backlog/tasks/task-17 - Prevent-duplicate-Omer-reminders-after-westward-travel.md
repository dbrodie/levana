---
id: TASK-17
title: Prevent duplicate Omer reminders after westward travel
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
  - levana-1bl
priority: medium
type: bug
ordinal: 17000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — remaining findings: Avoid repeated Omer notices after westward travel (Open)

Track delivered Omer reminders so a westward device time-zone change does not re-alert for a count already delivered. The time-zone receiver fix excludes delivery deduplication; the existing candle-lighting delivery log does not resolve Omer travel behavior. Keep the intentional evening and optional morning reminders distinct.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/fix-device-timezone-reminders.md
- specs/rfes/fix-notification-lookahead.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A westward time-zone/date change does not repost the same already-delivered Omer reminder. Subsequent counts and intentionally separate morning/evening reminders still deliver. Deduplication persists across rebuilds and process restarts.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Commit f0b9266. NotificationDeliveryLog.deliverOmerOnce: key (evening|morning, Hebrew year of counted day, count), checked/recorded under a process-wide mutex, recorded only when posted (permission, notifications enabled, channel not blocked); persisted in notification_delivery; prunes years older than previous. Receiver uses deliverAlarm; candle log now only when posted. Tests: OmerTravelDeliveryTest (6; repost suppressed, morning/evening distinct, next count and next year deliver, not recorded when not posted, restart persistence, 32-way concurrency, Tokyo->Honolulu westward replan); mutation 5/6 fail. Emulator: Tokyo evening+morning count 33 delivered; cleared, am kill, zone->New York (new process), city->New York; replanned 24 May evening and 25 May morning fired (alarm stats 2+2) and posted nothing; count 34 posted (/tmp/levana-emu-h3rx7pgl/artifacts/11-*, 13-*..17-*, 16-alarm-stats.txt). Branch fix/notification-dates-travel-omer (worktree /Users/daniel/.paseo/worktrees/15o7vaq5/notification-dates-travel-omer). Spec b9d6047/635484c, verification record dca60e7 (specs/rfes/fix-notification-dates-travel-omer.md). Gates: testDebugUnitTest 608 tests/84 suites 0 failures/errors/skips; lintDebug pass; assembleDebug pass. Emulator emulator-5554 (session /tmp/levana-emu-h3rx7pgl, APK sha256 4283bec7...d3d2). Status: implemented/verified awaiting parent integration.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-1bl",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-23T06:46:26Z",
  "closed_at": "2026-10-23T07:29:29Z",
  "close_reason": "Reviewed, verified and merged/pushed to origin/master at 94df4b4. Fresh parent-run debug unit suite: 608 tests, zero failures/errors/skips; lintDebug and assembleDebug passed. Emulator evidence inspected at /tmp/levana-emu-h3rx7pgl/artifacts, including real alarm delivery, timezone/system-clock broadcasts, process-restart delivery dedup and English/Hebrew card changes. Registry fixes all registered out-of-window alarm identities; legacy alarms beyond the one-time 14-day upgrade probe remain a documented limitation. Separate existing clock-jump/old-zone-delivery findings tracked in levana-vdq and levana-kq3.",
  "external_ref": "specs-import:followup-omer-travel",
  "metadata": {
    "import_key": "followup-omer-travel",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — remaining findings: Avoid repeated Omer notices after westward travel (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 2
}
```
