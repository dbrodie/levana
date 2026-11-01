---
id: TASK-57.9
title: Hebrew-primary month shows Hebrew weekday names in the English UI
status: To Do
assignee: []
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-01T15:02:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: bug
ordinal: 79000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
In Hebrew-primary mode the weekday header switches to Hebrew names even in the English UI. Weekday names should follow the UI language; the calendar mode only decides the grid layout.

Cause: CalendarScreen.kt has two DayHeaderRow call sites. GregorianMonthGrid passes the app locale (LocalConfiguration.current.locales[0]); HebrewMonthGrid passes a hardcoded HEBREW_LOCALE. Fix: HebrewMonthGrid uses the app locale too, and remove HEBREW_LOCALE if it becomes unused.

Out of scope: the Hebrew header names themselves ("יום א׳" from CLDR's SHORT style vs "ראשון" or NARROW "א׳") — keep TextStyle.SHORT.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **C5** (Medium) — Weekday header, Hebrew mode, English UI (21:307). Weekday labels switch to Hebrew ("יום א׳ … שבת") when the user is in English UI. *Why:* Mode (which calendar is primary) should not change UI language. *Reviewer suggestion:* Keep weekday labels in the UI language; optionally "Sun · א׳".
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Weekday labels follow the UI language in both calendar modes (English and Hebrew UI × Gregorian and Hebrew mode), covered by Roborazzi snapshots or a UI test
<!-- AC:END -->
