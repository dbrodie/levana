---
id: TASK-15
title: Preserve post-midnight havdalah end dates
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
  - levana-jh0
priority: medium
type: bug
ordinal: 15000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/TASKS.md — remaining findings: Preserve post-midnight havdalah end dates (Open)

Keep the actual civil end date for post-midnight havdalah through observance-end calculation and notification wording. This is distinct from the completed holy-day transition rules and from candle-lighting trigger planning.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.

Related specifications:
- specs/rfes/fix-omer-nightfall-rollover.md
- specs/rfes/fix-holiday-transitions.md
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An observance ending after midnight retains its actual end date/instant. Notification wording shows the correct civil day. Ordinary havdalah and consecutive Shabbat/Yom Tov end calculations remain correct.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Commit f0b9266. ObservanceEnd.date = civil date of havdalah (location zone), new lastDay = last holy day; new alarm extra end_civil_date_epoch_day, legacy end_date_epoch_day still read. Walk over consecutive days/Israel unchanged. Tests: PostMidnightHavdalahTest (6; St Petersburg 2026-07-17 -> Sun 19 Jul 00:13, Riga Shavuot+Shabbat 2027 -> Sun 13 Jun 00:12 YT, ordinary NY, Rosh Hashanah 2026, EN/HE wording, legacy extras); mutation 3/6 fail. Emulator: delivered 'Candle Lighting at 9:43 PM · Shabbat ends Sunday 12:13 AM' (/tmp/levana-emu-h3rx7pgl/artifacts/05-jh0-notification.txt). Display zmanim unchanged (levana-aw1). Branch fix/notification-dates-travel-omer (worktree /Users/daniel/.paseo/worktrees/15o7vaq5/notification-dates-travel-omer). Spec b9d6047/635484c, verification record dca60e7 (specs/rfes/fix-notification-dates-travel-omer.md). Gates: testDebugUnitTest 608 tests/84 suites 0 failures/errors/skips; lintDebug pass; assembleDebug pass. Emulator emulator-5554 (session /tmp/levana-emu-h3rx7pgl, APK sha256 4283bec7...d3d2). Status: implemented/verified awaiting parent integration.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-jh0",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-23T06:46:26Z",
  "closed_at": "2026-10-23T07:29:29Z",
  "close_reason": "Reviewed, verified and merged/pushed to origin/master at 94df4b4. Fresh parent-run debug unit suite: 608 tests, zero failures/errors/skips; lintDebug and assembleDebug passed. Emulator evidence inspected at /tmp/levana-emu-h3rx7pgl/artifacts, including real alarm delivery, timezone/system-clock broadcasts, process-restart delivery dedup and English/Hebrew card changes. Registry fixes all registered out-of-window alarm identities; legacy alarms beyond the one-time 14-day upgrade probe remain a documented limitation. Separate existing clock-jump/old-zone-delivery findings tracked in levana-vdq and levana-kq3.",
  "external_ref": "specs-import:followup-havdalah-rollover",
  "metadata": {
    "import_key": "followup-havdalah-rollover",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — remaining findings: Preserve post-midnight havdalah end dates (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 2
}
```
