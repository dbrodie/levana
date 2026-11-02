---
id: TASK-57.30
title: Reminder switches show on while notifications are not allowed
status: To Do
assignee: []
created_date: '2026-11-02T14:27:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: medium
type: feature
ordinal: 100000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found in TASK-57.23 (emulator API 36.1, build of ec31da0). With reminders enabled and POST_NOTIFICATIONS then revoked (pm revoke or system Settings), Settings > Notifications still shows Candle Lighting, Holidays, Fasts, Personal Events and Sefirat HaOmer as on, with no warning. Alarms stay scheduled; moving the clock past the Fri 08:00 candle-lighting alarm consumed it (fired, or dropped by the time-change rebuild) with nothing posted and nothing telling the user (no crash: NotificationPoster checks the permission). Evidence: /Users/daniel/.paseo/evidence/task-57.23/49-settings-notif-revoked.png, /Users/daniel/.paseo/evidence/task-57.23/60-settings-notif-permdenied-toggle.png. With notifications granted the same alarm posts 'Candle Lighting at 4:22 PM · Shabbat ends 5:18 PM' (control run).

Needs a design: e.g. a banner above the reminder switches ('Notifications are off for Levana — Turn on') when the permission is missing or the app's notifications/channels are blocked, linking to the system notification settings. Related: TASK-57.29 (permanently denied dead switch).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 When notifications are not allowed, the Notifications section says so and offers a way to turn them on
- [ ] #2 Design recorded (Figma or task notes) before implementation
<!-- AC:END -->
