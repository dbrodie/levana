---
id: TASK-57.22
title: Redesign the app icon
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: low
type: feature
ordinal: 92000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Redesign the launcher icon (and the dynamic day variants) with the review in mind: numeral legibility, brand meaning (Levana = moon), crescent visibility.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **W5** (Low) — Launcher icon (54:455). The rendered icon shows a navy disc with a white calendar glyph and a small "כ"; the spec text mentions a gold crescent that isn't visible. Hebrew numeral is small (38 %) and reads poorly at 48 dp. *Why:* The name means "moon"; the brand mark doesn't say moon. The daily letter is the icon's whole point. *Reviewer suggestion:* Make the numeral ≥ 50 % of the safe zone, bold; bring the crescent into the mark; test at 48 dp on light and dark wallpapers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 New icon designed in Figma and the day-variant generator updated
<!-- AC:END -->
