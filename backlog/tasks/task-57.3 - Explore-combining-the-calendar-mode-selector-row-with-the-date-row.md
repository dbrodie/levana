---
id: TASK-57.3
title: Explore combining the calendar-mode selector row with the date row
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
type: spike
ordinal: 73000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The Hebrew/Gregorian selector row also shows the other calendar's date range, so it earns its space today. Explore whether the selector and the date line can share one row (Month and Agenda) without losing the range.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **N5** (Medium) — Month & Agenda headers. The **Hebrew / Gregorian** segmented control is permanently visible on both screens and consumes a 56 dp row above the grid. In Agenda it sits above a list that already prints both calendars per row and per month header. *Why:* It is a sticky preference, not a per-session toggle; the row costs a week of grid height. *Reviewer suggestion:* Move to the title dropdown or an overflow item; or make the title tap-toggle ("October 2026 · Tishrei–Cheshvan 5787" ⇄). On Agenda remove it entirely.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Options explored in Figma and a recommendation recorded
<!-- AC:END -->
