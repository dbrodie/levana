---
id: TASK-54
title: Holiday reminder says 'begins tomorrow' on later days of a multi-day holiday
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T12:45:00Z'
updated_date: '2026-11-14T21:00:00Z'
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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Decision: a look-ahead reminder (offset > 0) is posted only for the first day of an occurrence. A day continues an occurrence when the day before it has a Torah/Rabbinic holiday of the same family, where Chol HaMoed X belongs to family X (so Chanukah day 2+, Rosh Hashanah day 2, Pesach/Sukkot day 2, Chol HaMoed after Yom Tov, Chol HaMoed day 2+, and Pesach day 7 after Chol HaMoed are all continuations). Continuations get no look-ahead at all; 'Today is …' (offset 0) is unchanged. Distinct names (Hoshana Rabba, Shemini Atzeret, Simchat Torah, Shushan Purim) still get their own look-ahead. This applies whether or not today is already inside the holiday (e.g. 3 days before Chanukah no longer posts 'Chanukah begins in 2 days' for day 2). Language-independent: the check uses Holiday.name, so English and Hebrew both follow.
1. Extract the selection in DailyNotificationWorker.handleHolidays into a pure internal function (holidayReminders) taking today, daysBefore and a day->holidays lookup, returning (date, holiday, daysUntil).
2. Add the family/continuation check there.
3. Unit tests with the real CalendarRepository: Chanukah day 2 (2026-12-06) posts only 'Today is Chanukah'; day before Chanukah posts 'begins tomorrow' and not day 2 with daysBefore 3; Pesach chol hamoed / 7th day suppressed; Shemini Atzeret after Hoshana Rabba still announced.
4. Build/test/lint, emulator check with date override 2026-12-06, holidays 1 day before.
<!-- SECTION:PLAN:END -->
