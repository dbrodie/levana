---
id: TASK-57.9
title: Hebrew-primary month shows Hebrew weekday names in the English UI
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-01T15:03:00Z'
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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. CalendarScreen.kt: HebrewMonthGrid passes LocalConfiguration.current.locales[0] to DayHeaderRow (same as GregorianMonthGrid); remove the now-unused HEBREW_LOCALE. Keep TextStyle.SHORT.
2. Tests: add an AppShellScreenshotTest case that saves calendarHebrewMode=true before launch, asserts the weekday header shows the UI-language SHORT names, and captures app_calendar_hebrew_mode in all four variants (en/he × light/dark). Gregorian mode is already covered by app_calendar_*; add the same text assertion there so all four UI × mode combinations are asserted.
3. Survey other weekday headers: HebrewDatePickerDialog already uses the app locale (NARROW); MonthWidget hardcodes Hebrew NARROW names in both languages -> file a separate task rather than widen this one.
4. Verify: assembleDebug, testDebugUnitTest, lintDebug; runtime check on a dedicated emulator in English and Hebrew UI with Hebrew mode; screenshots outside the worktree.
<!-- SECTION:PLAN:END -->
