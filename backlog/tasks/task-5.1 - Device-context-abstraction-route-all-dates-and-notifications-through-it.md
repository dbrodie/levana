---
id: TASK-5.1
title: Device context abstraction; route all dates and notifications through it
status: To Do
assignee: []
created_date: '2026-10-29T18:47:00Z'
labels: []
dependencies: []
parent_task_id: TASK-5
priority: high
type: chore
ordinal: 40000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Per TASK-5's rule and case table rows 1-5, 7, 8, 11, 12. Introduce the single injectable device context (clock + dev override as an 'as if' clock, device zone, device position = current/GPS or zone-fallback location), replacing TodayProvider and every inline override/now read. Make the alarm planner, daily worker, fast notices, tile, icon, theme and calendar use it. Remove the location-today planning window and use the device position (not the selected city) for candle-lighting/Omer triggers and notification text; candle 'morning' mode uses the device zone. Update existing time-zone/alarm tests that encode the old selected-city behavior, explaining each change in the notes. Update PROJECT_SPEC.md and ARCHITECTURE_SPEC.md with the rule.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All date/now/zone reads outside the abstraction are gone (grep evidence recorded)
- [ ] #2 Notification triggers and text use the device position and zone; the selected city has no effect (tests with New York device vs Jerusalem city)
- [ ] #3 Dev override behaves as an as-if device clock everywhere, including receivers and alarm triggers
- [ ] #4 PROJECT_SPEC.md and ARCHITECTURE_SPEC.md updated
<!-- AC:END -->
