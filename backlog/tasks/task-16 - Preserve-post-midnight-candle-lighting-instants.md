---
id: TASK-16
title: Preserve post-midnight candle-lighting instants
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-21T11:19:35Z"
updated_date: "2026-10-23T07:29:29Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-yal
priority: high
type: bug
ordinal: 16000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — remaining findings: Preserve post-midnight candle-lighting dates (Open)

When Yom Tov candle lighting is at nightfall after civil midnight, carrying only LocalTime can reconstruct the trigger on the wrong date. Preserve the actual candle-lighting instant/date through hours-before reminder planning. The completed Omer nightfall rollover fix explicitly excludes this candle-lighting path.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/fix-omer-nightfall-rollover.md
- specs/rfes/fix-holiday-transitions.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Hours-before reminders are derived from the actual candle-lighting instant, including next-civil-day nightfall. Reminder identity retains the intended source evening. Regression checks cover post-midnight and ordinary same-day candle lighting, consecutive holy days, and time zones.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Commit f0b9266. Hours-before candle reminders now use ZmanimRepository.getCandleLightingInstant (same holy-day rules) instead of source date + LocalTime; identity stays the source evening; previous evening's candle reminder is planned while its candles are still to be lit (overdue kept, cancelled after lighting). Tests: PostMidnightCandleLightingTest (9; Tartu 2028 Shavuot 00:08:25, Tartu 2036 Shabbat->YT->YT, Israel vs Diaspora, DST Tallinn, device!=location zone, white nights, morning mode); mutation 5/9 fail. Emulator: St Petersburg 2031-05-28 alarm set 23:45:10.189 MSK, delivered 23:45:25 as candle_lighting/2031-05-28 (/tmp/levana-emu-h3rx7pgl/artifacts/02-yal-alarms-after-settings.txt, 03-yal-jh0-notification.txt). Candle wording unchanged (time only). Branch fix/notification-dates-travel-omer (worktree /Users/daniel/.paseo/worktrees/15o7vaq5/notification-dates-travel-omer). Spec b9d6047/635484c, verification record dca60e7 (specs/rfes/fix-notification-dates-travel-omer.md). Gates: testDebugUnitTest 608 tests/84 suites 0 failures/errors/skips; lintDebug pass; assembleDebug pass. Emulator emulator-5554 (session /tmp/levana-emu-h3rx7pgl, APK sha256 4283bec7...d3d2). Status: implemented/verified awaiting parent integration.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-yal",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-23T06:46:26Z",
  "closed_at": "2026-10-23T07:29:29Z",
  "close_reason": "Reviewed, verified and merged/pushed to origin/master at 94df4b4. Fresh parent-run debug unit suite: 608 tests, zero failures/errors/skips; lintDebug and assembleDebug passed. Emulator evidence inspected at /tmp/levana-emu-h3rx7pgl/artifacts, including real alarm delivery, timezone/system-clock broadcasts, process-restart delivery dedup and English/Hebrew card changes. Registry fixes all registered out-of-window alarm identities; legacy alarms beyond the one-time 14-day upgrade probe remain a documented limitation. Separate existing clock-jump/old-zone-delivery findings tracked in levana-vdq and levana-kq3.",
  "external_ref": "specs-import:followup-candle-rollover",
  "metadata": {
    "import_key": "followup-candle-rollover",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — remaining findings: Preserve post-midnight candle-lighting dates (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 1
}
```
