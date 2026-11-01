---
id: TASK-57.25
title: Hebrew years written the same way everywhere (date picker shows ה׳תשפ״ז)
status: To Do
assignee: []
created_date: '2026-11-01T15:23:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: medium
type: bug
ordinal: 96000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The date picker writes Hebrew years in the long form with the thousands letter ("ה׳תשפ״ז"), while the rest of the app writes the short form ("תשפ״ז"): Hebrew-UI month titles, Agenda headers, day details, the quick tile, the Events screen and the walkthrough.

Why: DatePickerFormatter sets KosherJava's isUseLongHebrewYears = true (and strips the space KosherJava puts after ה׳). It was copied from the design 1b mockups when the picker was built in TASK-45 ("חשוון ה׳תשפ״ז", year grid "ה׳תשפ״ז / 5787"). Nothing else in the app uses the long form; it was not a deliberate product decision.

Decision (owner, 2026-11-01): use the short form "תשפ״ז" everywhere, including the picker's title, year grid and footer.

Consistency: the app has about nine separately configured KosherJava HebrewDateFormatter instances (CalendarRepository, CalendarHeaderFormatter, DatePickerFormatter, EventsScreen, HebrewMonth, OmerCountText, WalkthroughIllustrations, HebrewDateTileService, ...). Put Hebrew year/number formatting behind one shared helper (or a shared configured formatter) so the year form cannot drift again, and route every Hebrew-letter year through it. Keep the change to formatting; don't change month spelling or other text.

Related: TASK-57.8 (concurrently changes CalendarHeaderFormatter.hebrewMonthTitle for the English UI).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Hebrew-letter years use the short form (e.g. תשפ״ז) everywhere in the app, including the date picker title, year grid and footer
- [ ] #2 All Hebrew-letter year formatting goes through one shared helper, covered by unit tests
- [ ] #3 Affected Roborazzi baselines updated intentionally and verified on the emulator
<!-- AC:END -->
