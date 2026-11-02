---
id: TASK-57.26
title: 'Hebrew UI, 12-hour clock: day-sheet zman times read ''PM 5:22'''
status: To Do
assignee: []
created_date: '2026-11-01T18:03:00Z'
updated_date: '2026-11-02T13:42:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: low
ordinal: 97000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-57.6 runtime check (evidence: /Users/daniel/.paseo/evidence/task-57.6/09-he-daysheet-molad-12h.png). With the app in Hebrew and the device on a 12-hour clock, the day sheet's Zmanim card rows and the Havdalah card show times reversed by the bidi algorithm ('PM 5:22', 'AM 6:00'), while the quick zman cards show '5:22 PM'.

Cause: a Latin "5:22 PM" inside right-to-left text/layout is reordered by the Unicode bidi algorithm unless it is isolated. MoladText (TASK-57.6) already wraps its time in an LTR isolate (U+2066 … U+2069); other places don't.

Scope (agreed 2026-11-02):
- Fix every Hebrew-UI 12-hour time display: day sheet (zman rows, Havdalah, Shabbat cards, quick cards), Zmanim screen, Agenda, widgets, tile, notifications — anywhere a formatted time is shown.
- Consistency: time formatting is duplicated in ~7 places (DayDetailScreen, ZmanimScreen, AgendaScreen, MoladText, WidgetCommon, NotificationTexts, …), each with its own "h:mm a"/"HH:mm" formatters. Put it behind one shared helper that formats a time and, in the Hebrew UI, wraps it in an LTR isolate; route every caller through it, so the bidi fix can't be missed again.
- Keep each caller's current 12/24-hour decision unchanged (e.g. NotificationTexts' rule). Making every surface follow the device setting is TASK-57.11; note any inconsistencies you find there rather than changing them here.
- Check that the isolate characters don't leak into places that measure or compare text (tests, TalkBack content descriptions, widgets) in a visible way.

Related: TASK-57.11 (acceptance criterion #2 "Hebrew UI renders times correctly inside RTL text" is covered by this task).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 In the Hebrew UI with a 12-hour clock, every displayed time reads "5:22 PM" (not "PM 5:22"): day sheet, Zmanim screen, Agenda, widgets, tile and notifications
- [ ] #2 All time formatting goes through one shared helper, covered by unit tests; 12/24-hour behaviour per surface is unchanged
- [ ] #3 Verified on the emulator in Hebrew and English UI, 12- and 24-hour, with Roborazzi baselines updated only where intended
<!-- AC:END -->
