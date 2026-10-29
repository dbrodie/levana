---
id: TASK-9.3
title: Zmanim list home-screen widget
status: To Do
assignee: []
created_date: '2026-10-29T18:19:00Z'
updated_date: '2026-10-29T20:08:00Z'
labels: []
dependencies:
  - TASK-9.1
parent_task_id: TASK-9
priority: medium
type: feature
ordinal: 42000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Third subtask of TASK-9; see TASK-9 for product decisions and shared design. Uses the foundation from TASK-9.1. Scrollable list of today's selected zmanim (same selection as day details), past times dimmed, next zman highlighted. Times and header location follow the DEVICE location via DeviceContext (owner decision 2026-10-29, consistent with the Today widget), not the selected city. Per-widget city choice is a separate future task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Zmanim list widget shows today's selected zmanim with past dimmed and next highlighted, using the shared color scheme
- [ ] #2 Advances the highlighted zman as times pass and refreshes on date, time-zone, location and preference changes
- [ ] #3 Row tap opens the Zmanim screen for today; uncomputable zman shows —
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
TASK-5 owner decision (2026-10-29): widgets show the device position (not the selected city); the 'active location in header' here should read the device position, as the Today widget now does.
<!-- SECTION:NOTES:END -->
