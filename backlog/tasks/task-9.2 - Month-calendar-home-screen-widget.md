---
id: TASK-9.2
title: Month calendar home-screen widget
status: To Do
assignee: []
created_date: '2026-10-29T18:19:00Z'
updated_date: '2026-10-29T18:23:00Z'
labels: []
dependencies:
  - TASK-9.1
parent_task_id: TASK-9
priority: medium
type: feature
ordinal: 41000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Second subtask of TASK-9; see TASK-9 for product decisions and shared design. Uses the foundation from TASK-9.1. Month grid with Hebrew primary day numbers and Gregorian secondary, holiday markers, today highlighted, using the shared app/holiday colors. Header tap opens the calendar; day cell tap opens that day's details.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Month widget (about 4x4, resizable) shows the current Hebrew month with Gregorian secondary numbers, holiday markers and today highlighted, using the shared color scheme
- [ ] #2 Refreshes on date, locale, time-zone, and relevant preference changes
- [ ] #3 Day cell tap opens the calendar with that day selected; header opens month calendar on today
<!-- AC:END -->
