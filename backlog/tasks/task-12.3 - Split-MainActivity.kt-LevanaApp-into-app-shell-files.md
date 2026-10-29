---
id: TASK-12.3
title: Split MainActivity.kt / LevanaApp into app-shell files
status: To Do
assignee: []
created_date: '2026-10-29T18:50:00Z'
labels: []
dependencies:
  - TASK-12.1
parent_task_id: TASK-12
priority: low
type: chore
ordinal: 46000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Target: ui/navigation/LevanaApp.kt, LevanaNavHost.kt, AppDrawer.kt (LevanaDrawerContent), TopBarTitles.kt (pure topBarTitleFor(destination)), startup effects (permission, resume, deep link) extracted as a composable; MainActivity keeps only the Activity and theme resolution (resolveHolidayTheme as a pure function). Behavior unchanged.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 MainActivity.kt is roughly 80 lines or fewer; move-only diff
- [ ] #2 New tests: topBarTitleFor per route (incl. Add vs Edit event); Compose tests that drawer items navigate to Calendar, My Events, Settings and Manage cities, that a city tap selects it and closes the drawer, and that the back arrow shows on non-drawer routes
- [ ] #3 verifyRoborazziDebug passes with zero diffs; emulator check of drawer, deep link, back navigation and My Events top-bar actions recorded
<!-- AC:END -->
