---
id: TASK-54
title: Holiday reminder says 'begins tomorrow' on later days of a multi-day holiday
status: To Do
assignee: []
created_date: '2026-10-30T12:45:00Z'
labels: []
dependencies: []
priority: low
type: bug
ordinal: 61000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-52.5 (emulator, 2026-10-30). DailyNotificationWorker.handleHolidays posts one reminder per holiday occurrence for each date in 0..holidayNotifyDaysBefore, and holidayNotificationText uses daysUntil only. On a day that is already Chanukah, tomorrow's occurrence (day 2+) is posted as 'Chanukah begins tomorrow' next to 'Today is Chanukah'. Repro: Developer settings date override 2026-12-06 (Chanukah day 2), Holidays notifications on with 1 day before; the shade shows 'Chanukah begins tomorrow' (tag holiday/2026-12-07/Chanukah) and 'Today is Chanukah'. Same applies to Sukkot/Pesach chol hamoed and other consecutive-day holidays. Evidence: /tmp/levana-task52.5-evidence/fold8/32-notifications-cover.png.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A reminder for a later day of a holiday that is already in progress is not described as beginning; only the first day gets 'begins tomorrow / in N days'
- [ ] #2 Unit test covers a multi-day holiday on its second day
<!-- AC:END -->
