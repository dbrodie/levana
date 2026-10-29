---
id: TASK-5.2
title: Refresh today on resume, date change and time-zone change
status: To Do
assignee:
  - '@claude'
created_date: '2026-10-29T18:47:00Z'
updated_date: '2026-10-29T18:54:00Z'
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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
- DeviceContext gains `today: Flow<LocalDate>` (distinct) that re-evaluates on: preference changes (dev shift/position), a system time signal flow (Android callbackFlow over ACTION_DATE_CHANGED / ACTION_TIME_CHANGED / ACTION_TIMEZONE_CHANGED, injected so tests can drive it), and a timer to the next device midnight.
- CalendarViewModel collects it and updates state.today (highlight) in place; Resume intent also re-reads now. Selection/browsed month are left alone (only the highlight moves).
- MainActivity theme collects the today flow; icon updated on resume and when the today flow changes.
- TimeChangeReceiver also refreshes the launcher icon (TIMEZONE_CHANGED/TIME_SET). Tile reads DeviceContext on every onStartListening (already per-show) and requests a listening state refresh via TileService.requestListeningState on date change.
- Tests: fake clock + signal flow cover the midnight advance in CalendarViewModel and DeviceContext.today; zone change moves the date.
<!-- SECTION:PLAN:END -->
