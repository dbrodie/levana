---
id: TASK-57.8
title: Hebrew-mode month title mixes scripts ("5787 חשון")
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: bug
ordinal: 78000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Fix the Hebrew-mode title format (and the picker's year format) so numerals and script are consistent per UI language.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **C4** (Medium) — Month header, Hebrew mode (21:307). Title "5787 חשון" mixes Arabic numerals with a Hebrew month name and reads awkwardly in an LTR bar; the date picker shows the same year as "ה׳תשפ״ז". *Why:* Inconsistent year representation. *Reviewer suggestion:* EN UI: "Cheshvan 5787" (transliterated month). HE UI: "חשון תשפ״ז". Keep numerals consistent with the picker.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Title formats are consistent in English and Hebrew UI
<!-- AC:END -->
