---
id: TASK-5.4
title: City selection only from the sidebar; zone label on zmanim
status: To Do
assignee: []
created_date: '2026-10-29T18:47:00Z'
labels: []
dependencies: []
parent_task_id: TASK-5
priority: medium
type: feature
ordinal: 43000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-5 case table row 9 and the owner's Settings decisions. Adding a city in Settings → Locations no longer makes it active; choosing happens only in the sidebar/drawer. Keep the Settings wording as is, but remove the secondary text under the main Settings Location row that shows the current city (SettingsScreen.kt:347). The Zmanim screen and day-panel times show the selected city's zmanim in the city's own clock, with a zone label (e.g. 'Jerusalem time') only when the city's zone differs from the device zone.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Adding a city does not change the selected city; removing the selected city still falls back to Current location
- [ ] #2 The Settings Location row no longer shows the current city as secondary text
- [ ] #3 Zone label appears only when zones differ, in English and Hebrew; tests and emulator screenshots recorded
<!-- AC:END -->
