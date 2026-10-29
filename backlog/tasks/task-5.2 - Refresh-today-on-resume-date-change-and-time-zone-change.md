---
id: TASK-5.2
title: Refresh today on resume, date change and time-zone change
status: To Do
assignee: []
created_date: '2026-10-29T18:47:00Z'
labels: []
dependencies: []
parent_task_id: TASK-5
priority: medium
type: bug
ordinal: 41000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-5 case table row 13. The calendar today highlight goes stale past midnight (state.today is set only in loadMonth). The icon is not refreshed on TIMEZONE_CHANGED/TIME_SET. Make the calendar, icon, tile and holiday theme observe the device context and update on resume, date change and time/zone change.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 With the app alive across midnight or a zone change, the calendar highlight, icon, tile and theme all show the new device date after resume, without restart
- [ ] #2 Unit tests with a fake clock cover the midnight advance; emulator evidence recorded
<!-- AC:END -->
