---
id: TASK-57.9
title: Hebrew-primary month shows Hebrew weekday names in the English UI
status: Done
assignee:
  - '@claude'
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-01T15:19:00Z'
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
- [x] #1 Weekday labels follow the UI language in both calendar modes (English and Hebrew UI × Gregorian and Hebrew mode), covered by Roborazzi snapshots or a UI test
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. CalendarScreen.kt: HebrewMonthGrid passes LocalConfiguration.current.locales[0] to DayHeaderRow (same as GregorianMonthGrid); remove the now-unused HEBREW_LOCALE. Keep TextStyle.SHORT.
2. Tests: add an AppShellScreenshotTest case that saves calendarHebrewMode=true before launch, asserts the weekday header shows the UI-language SHORT names, and captures app_calendar_hebrew_mode in all four variants (en/he × light/dark). Gregorian mode is already covered by app_calendar_*; add the same text assertion there so all four UI × mode combinations are asserted.
3. Survey other weekday headers: HebrewDatePickerDialog already uses the app locale (NARROW); MonthWidget hardcodes Hebrew NARROW names in both languages -> file a separate task rather than widen this one.
4. Verify: assembleDebug, testDebugUnitTest, lintDebug; runtime check on a dedicated emulator in English and Hebrew UI with Hebrew mode; screenshots outside the worktree.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Tested commit: 775805c (code, tests, baselines).

Change: CalendarScreen.kt HebrewMonthGrid now passes LocalConfiguration.current.locales[0] to DayHeaderRow, like GregorianMonthGrid; HEBREW_LOCALE removed. TextStyle.SHORT kept.

Tests: AppShellScreenshotTest gains calendarHebrewMode (saves calendarHebrewMode=true, launches, asserts the SHORT Sunday/Saturday names in the UI locale, captures app_calendar_hebrew_mode_{en,he}_{light,dark}); calendarAndDrawer now asserts the same names in Gregorian mode. So all four UI × mode combinations are asserted, not only imaged. On the unfixed CalendarScreen.kt the new test fails in the two English variants (2 of 4 failed); with the fix all pass. Only the four new baselines were recorded; no existing baseline changed.

Results: assembleDebug, testDebugUnitTest and lintDebug pass (exit 0); verifyRoborazziDebug passes.

Runtime (Levana_Agent_Template_API_36_1, emulator-5556, own headless session, debug APK from this commit's source): Hebrew mode with English UI shows Sun…Sat; with app locale set to he (cmd locale set-app-locales) shows יום א׳…שבת. UI dump confirms the header texts.

Evidence: /tmp/levana-task57.9-evidence/ (01_en_ui_hebrew_mode.png, 02_he_ui_hebrew_mode.png, t579_build.log, t579_verify.log).

Survey of other weekday headers: HebrewDatePickerDialog already uses the app locale (NARROW). MonthWidget hardcodes Hebrew NARROW names in both languages; filed TASK-57.24 rather than widening this task (different file, and English NARROW names are ambiguous, so it needs a style decision).

Limitations: Hebrew header style (SHORT 'יום א׳') unchanged, out of scope. TASK-57.24's ID was allocated in this worktree; check for a clash if a concurrent branch also created 57.24.
<!-- SECTION:NOTES:END -->
