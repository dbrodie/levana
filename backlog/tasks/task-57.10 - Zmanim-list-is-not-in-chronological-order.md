---
id: TASK-57.10
title: Zmanim list is added out of order
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-01T14:56:00Z'
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
