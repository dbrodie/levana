---
id: TASK-57.10
title: Zmanim list is added out of order
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-01T15:00:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: bug
ordinal: 80000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Sof Zman Shema (GRA) is added to the list before the earlier Sof Zman Shema (MGA), and Sof Zman Tefillah has a single unlabelled opinion. The zmanim follow a fixed order, so no runtime sort is needed: fix the order they are added in ZmanimRepository.getZmanim, add Sof Zman Tefillah (MGA) and label both opinions.

Fixed order: Alot → Misheyakir → Sunrise → Shema (MGA) → Shema (GRA) → Tefillah (MGA) → Tefillah (GRA) → Chatzot → Mincha Gedolah → Mincha Ketanah → Plag → Candle lighting → Sunset → Nightfall → Havdalah → Chatzot HaLaylah.

Known exception, accepted rather than sorted: Tefillah (MGA) = sunrise + day/3 − 24 min and Shema (GRA) = sunrise + day/4, so Tefillah (MGA) falls before Shema (GRA) only when the day is shorter than 4.8 hours (polar winter, roughly north of 60°). Note this in a code comment.

Guard against future misplacement with a unit test asserting chronological order for representative cities (e.g. Jerusalem, New York) in summer and winter.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **Z2** (High) — Zmanim (13:236). **Not chronological**: Sof Zman Shema (GRA) 8:38 is listed before Sof Zman Shema (MGA) 8:02. *Why:* A zmanim list is read top-to-bottom as the day unfolds. *Reviewer suggestion:* Sort by time; label "Sof Zman Shema (MGA) 8:02", "(GRA) 8:38", and likewise show Sof Zman Tefillah for both (currently only one, unlabelled).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Zmanim are added in fixed chronological order, with Sof Zman Tefillah shown and labelled for both MGA and GRA (English and Hebrew)
- [ ] #2 A unit test asserts chronological order for representative cities in summer and winter
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. ZmanimRepository.getZmanim: add in fixed order Alot, Misheyakir, Sunrise, Shema (MGA), Shema (GRA), Tefillah (MGA), Tefillah (GRA), Chatzot, ... (no runtime sort); add SOF_ZMAN_TEFILLAH_MGA (czc.sofZmanTfilaMGA) and rename SOF_ZMAN_TEFILLAH to SOF_ZMAN_TEFILLAH_GRA labelled 'Sof Zman Tefillah (GRA)' / 'סוף זמן תפילה (גר״א)'. Reorder ZMAN_NAMES to match. Code comment on the polar-winter exception (day < 4.8 h: Tefillah MGA before Shema GRA).
2. The English name is the persisted selectedZmanim key, so map the legacy key 'Sof Zman Tefillah' to 'Sof Zman Tefillah (GRA)' when reading preferences (PreferencesRepository) so existing selections survive; update the ZmanName doc comment.
3. Tests: chronological-order test for Jerusalem and New York on a summer and winter date (incl. a Friday for candle lighting / Shabbat for havdalah); legacy key mapping test; ZmanNamesTest keeps the order check.
4. Check consumers (Zmanim screen, day sheet, widgets, tile, notifications, Halachic Times settings, Roborazzi) and re-record snapshot baselines that change intentionally.
5. Verify: assembleDebug, testDebugUnitTest, lintDebug; runtime check on own emulator in English and Hebrew; screenshots outside the worktree.
<!-- SECTION:PLAN:END -->
