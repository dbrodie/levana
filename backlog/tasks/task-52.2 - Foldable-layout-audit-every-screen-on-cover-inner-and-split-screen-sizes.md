---
id: TASK-52.2
title: 'Foldable layout audit: every screen on cover, inner and split-screen sizes'
status: To Do
assignee: []
created_date: '2026-10-30T10:31:00Z'
labels: []
dependencies:
  - TASK-52.1
parent_task_id: TASK-52
priority: medium
type: task
ordinal: 57000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Audit every screen and dialog on the Fold 8 and Fold 8 Ultra cover display, inner display (portrait and landscape) and split-screen or freeform widths, in English/Hebrew (RTL) and light/dark:
- calendar (Month, both modes) and the day panel;
- Agenda and search;
- Zmanim;
- My Events, the event and birthday editors;
- the date picker;
- Settings and all sub-screens and dialogs;
- the drawer;
- the walkthrough;
- the time-zone mismatch dialog and the permission prompts.

Look for: stretched or awkwardly wide content, clipped or overlapping text, the calendar grid and day cells scaling badly, dialogs or pickers at full tablet width (cap the width), bottom sheets, touch targets, the hinge and display cutouts, and 200% text on the cover screen. Fix the issues that need no design decision. File or defer anything needing a new adaptive layout to the adaptive-layout subtask. Add Roborazzi baselines for key screens at the foldable sizes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Every screen is audited on all foldable sizes with screenshots and recorded findings; non-design issues are fixed; design-level items are moved to the adaptive-layout subtask
- [ ] #2 Roborazzi foldable baselines for the key screens; existing baselines unchanged except for justified fixes
<!-- AC:END -->
