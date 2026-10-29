---
id: TASK-21
title: Localize the day-panel Omer card
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-21T11:19:38Z"
updated_date: "2026-10-23T07:29:29Z"
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-3kn
priority: medium
type: bug
ordinal: 21000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Source: specs/rfes/localize-notifications-zman-names.md — Interpretations #4

The RFE explicitly says the day panel Omer card (DayInfo.omerFormatted) is still English-only and excludes it from notification localization. This is a separate screen-localization gap; do not duplicate the broader notification/zman-name RFE whose completion status is ambiguous.

Review basis: document-only curation from specs/TASKS.md and specs/rfes/. No code verification is claimed.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 With Hebrew app language, the day-panel Omer card uses Hebrew text and correct counts. English output remains correct. The card follows app-language changes independently of notification delivery.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Commit d6eb7f6. Removed DayInfo.omerFormatted; ui/OmerCountText.kt (omerDayLabel, omerWeeksText, omerCardText) with resources omer_card_day(_with_weeks); plurals renamed omer_weeks/omer_and_days/omer_weeks_and_days and shared with omerNotificationText (output unchanged, NotificationTextsTest passes). OmerSection(omerDay) reads LocalContext. Tests: OmerCardTextTest (4; EN/HE literals for 1,6,7,8,9,13,14,15,21,33,48,49; all 1-49 structural; language switch). Emulator: EN day 33/28 and HE RTL day 33/28 with live set-app-locales switch, same process, selected date kept (/tmp/levana-emu-h3rx7pgl/artifacts/20-*..23-* png/xml). Branch fix/notification-dates-travel-omer (worktree /Users/daniel/.paseo/worktrees/15o7vaq5/notification-dates-travel-omer). Spec b9d6047/635484c, verification record dca60e7 (specs/rfes/fix-notification-dates-travel-omer.md). Gates: testDebugUnitTest 608 tests/84 suites 0 failures/errors/skips; lintDebug pass; assembleDebug pass. Emulator emulator-5554 (session /tmp/levana-emu-h3rx7pgl, APK sha256 4283bec7...d3d2). Status: implemented/verified awaiting parent integration.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-3kn",
  "spec_id": "specs/rfes/localize-notifications-zman-names.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-23T06:46:26Z",
  "closed_at": "2026-10-23T07:29:29Z",
  "close_reason": "Reviewed, verified and merged/pushed to origin/master at 94df4b4. Fresh parent-run debug unit suite: 608 tests, zero failures/errors/skips; lintDebug and assembleDebug passed. Emulator evidence inspected at /tmp/levana-emu-h3rx7pgl/artifacts, including real alarm delivery, timezone/system-clock broadcasts, process-restart delivery dedup and English/Hebrew card changes. Registry fixes all registered out-of-window alarm identities; legacy alarms beyond the one-time 14-day upgrade probe remain a documented limitation. Separate existing clock-jump/old-zone-delivery findings tracked in levana-vdq and levana-kq3.",
  "external_ref": "specs-import:rfe-omer-card-language",
  "metadata": {
    "import_key": "rfe-omer-card-language",
    "review_basis": "documents-only",
    "import_source": "specs/rfes/localize-notifications-zman-names.md — Interpretations #4"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 2
}
```
