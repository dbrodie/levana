---
id: TASK-54
title: Holiday reminder says 'begins tomorrow' on later days of a multi-day holiday
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T12:45:00Z'
updated_date: '2026-11-14T21:13:00Z'
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
- [x] #1 A reminder for a later day of a holiday that is already in progress is not described as beginning; only the first day gets 'begins tomorrow / in N days'
- [x] #2 Unit test covers a multi-day holiday on its second day
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Decision: a look-ahead reminder (offset > 0) is posted only for the first day of an occurrence. A day continues an occurrence when the day before it has a Torah/Rabbinic holiday of the same family, where Chol HaMoed X belongs to family X (so Chanukah day 2+, Rosh Hashanah day 2, Pesach/Sukkot day 2, Chol HaMoed after Yom Tov, Chol HaMoed day 2+, and Pesach day 7 after Chol HaMoed are all continuations). Continuations get no look-ahead at all; 'Today is …' (offset 0) is unchanged. Distinct names (Hoshana Rabba, Shemini Atzeret, Simchat Torah, Shushan Purim) still get their own look-ahead. This applies whether or not today is already inside the holiday (e.g. 3 days before Chanukah no longer posts 'Chanukah begins in 2 days' for day 2). Language-independent: the check uses Holiday.name, so English and Hebrew both follow.
1. Extract the selection in DailyNotificationWorker.handleHolidays into a pure internal function (holidayReminders) taking today, daysBefore and a day->holidays lookup, returning (date, holiday, daysUntil).
2. Add the family/continuation check there.
3. Unit tests with the real CalendarRepository: Chanukah day 2 (2026-12-06) posts only 'Today is Chanukah'; day before Chanukah posts 'begins tomorrow' and not day 2 with daysBefore 3; Pesach chol hamoed / 7th day suppressed; Shemini Atzeret after Hoshana Rabba still announced.
4. Build/test/lint, emulator check with date override 2026-12-06, holidays 1 day before.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented in DailyNotificationWorker: handleHolidays now uses the pure internal holidayReminders(today, daysBefore, holidaysOn). Offset 0 posts every Torah/Rabbinic holiday as before ('Today is …'). For offset > 0 a holiday is skipped when the day before it has a Torah/Rabbinic holiday of the same occurrence (name with any 'Chol HaMoed ' prefix removed). So Chanukah day 2+, Rosh Hashanah/Pesach/Sukkot day 2, Chol HaMoed after Yom Tov, and Pesach 7th day after Chol HaMoed get no look-ahead. Hoshana Rabba, Shemini Atzeret, Simchat Torah, Shushan Purim and Pesach day 1 after Erev Pesach are still announced. The check uses Holiday.name, so English and Hebrew text both follow. Strings unchanged.
This also fixes the look-ahead from before a holiday: with 3 days before, 3 Dec no longer posts 'Chanukah begins in 3 days' for day 2.

Tested commit: c434120 (fix + tests).
- ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug: passed.
- New HolidayRemindersTest uses the real CalendarRepository (Diaspora). Cases: Chanukah day 2 (6 Dec 2026, 1 and 3 days before) is only today; 4 Dec gets 'Chanukah +1' and 3 Dec gets 'Chanukah +2'; Pesach 5787 (Erev, day 2, Chol HaMoed with day 7 ahead); Sukkot (Hoshana Rabba / Shemini Atzeret / Simchat Torah still announced); Rosh Hashanah 5788 day 2.
- Emulator (Levana_Agent_Template_API_36_1, read-only session, Jerusalem/auto-Israel defaults): Developer settings date override 2026-12-06, Holidays on, 1 day before. The shade shows only 'Today is Chanukah' (tag holiday/2026-12-06/Chanukah), with no holiday/2026-12-07 entry. Override 2026-12-04, then Holidays toggled off/on: 'Chanukah begins tomorrow' (tag holiday/2026-12-05/Chanukah) posted. The 12-06 entry in that dump is left over from the first run.
Evidence: /Users/daniel/.paseo/evidence/task-54/ (02-shade-2026-12-06.png, 02-dumpsys-2026-12-06.txt, 03-shade-2026-12-04.png, 03-dumpsys-2026-12-04.txt).
Limitations: the emulator check was done in English only; Hebrew follows the same name-based selection, and its strings are covered by the existing NotificationTextsTest. Occurrence grouping depends on the HolidayMapper English names ('Chol HaMoed X' ↔ 'X').
<!-- SECTION:NOTES:END -->
