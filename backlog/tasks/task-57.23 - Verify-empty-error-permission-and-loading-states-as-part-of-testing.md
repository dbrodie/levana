---
id: TASK-57.23
title: Verify empty, error, permission and loading states as part of testing
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
type: task
ordinal: 93000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The review lists error/empty/denied/loading states that are not drawn in Figma. This is a coverage gap: verify in the app (tests and emulator) that each state exists and is not broken; add Figma frames only where useful.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **ST1** (High) — No error states exist anywhere: location unavailable, zmanim unavailable at high latitudes (spec can't compute some zmanim in polar summers), import failure, crash-report dialog (Developer has "Test crash" but the dialog isn't designed), no network for update check. *Reviewer suggestion:* Design an error pattern (inline banner + snackbar) and the crash-report dialog.
- **ST2** (Medium) — Missing empty states: Agenda empty, Events tab empty, Search no-results, Cities search no-match, Saved cities empty. *Reviewer suggestion:* Add, reusing the existing birthdays empty-state pattern.
- **ST3** (Medium) — Missing "permanently denied" permission variants (location, contacts, calendar, notifications). *Reviewer suggestion:* Add "Open settings" variants.
- **ST4** (Low) — No loading/skeleton states (first launch computing, calendar provider query). *Reviewer suggestion:* Add minimal shimmer or progress on Agenda/System calendars.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Each listed state is verified in the app (or a bug is filed)
<!-- AC:END -->
