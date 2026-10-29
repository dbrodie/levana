---
id: TASK-12.1
title: 'Screenshot safety net: Roborazzi baselines for Settings and app shell'
status: To Do
assignee: []
created_date: '2026-10-29T18:50:00Z'
labels: []
dependencies:
  - TASK-5
  - TASK-9
parent_task_id: TASK-12
priority: low
type: chore
ordinal: 44000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Record from current master after TASK-5 and TASK-9 merge, before any split. Configure roborazzi { outputDir = file("src/test/screenshots") } so baselines are committed, with a fixed device qualifier. Cover light/dark × English/Hebrew-RTL for: Settings main (notification sub-rows expanded, developer group shown), each Settings dialog, Locations (empty, with saved cities, without permission), Halachic Times, Developer, About, the LevanaApp drawer (cities collapsed and expanded) and the top bar for each route. Use stub repositories and a fixed clock so images are deterministic. Update ARCHITECTURE_SPEC so its Roborazzi claim matches reality and document the record/verify commands.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 recordRoborazziDebug produces committed, deterministic baselines (two consecutive verifyRoborazziDebug runs pass)
- [ ] #2 Coverage list above is met in both themes and both languages
- [ ] #3 ARCHITECTURE_SPEC documents the screenshot workflow accurately
<!-- AC:END -->
