---
id: TASK-45
title: >-
  Replace the app's date pickers with one shared dual-calendar (design 1b)
  Hebrew date picker
status: To Do
assignee: []
created_date: '2026-10-29T21:22:00Z'
labels: []
dependencies: []
priority: medium
type: feature
ordinal: 49000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner request 2026-10-29. Implement the 'Hebrew Date Picker — 1b dual calendar' design from Claude Design and make it the app's single date picker.

Design source: https://claude.ai/design/p/fe15c347-4eef-410e-9639-d561a1ff21b0?file=Hebrew+Date+Picker.dc.html (variant 1b), also shared as https://claude.ai/artifact/YBRjcnJqBQ1CUwjK9CkTj5. Agents cannot open these links (login required). A screenshot or HTML export of variant 1b must be attached to this task (stored outside the repo, path recorded here) before implementation starts. The visual and interaction spec below is to be filled in from it.

Scope: today the app has two separate date-entry UIs, and both are replaced by one shared picker component:
1. The stepper-based Hebrew day/month/year picker in the add/edit personal event screen and the contact birthday editor.
2. The calendar's Go-to-date dialog (separate Hebrew and Gregorian rows with dropdowns and text fields), which the developer date-override setting also reuses.
Remove the old implementations once nothing uses them. Do not keep two pickers.

Requirements:
- Dual calendar: Hebrew and Gregorian shown together and kept in sync; choosing on either side updates the other.
- Hebrew month handling covers leap years (Adar I/II vs Adar) and 29/30-day months, using the app's existing Hebrew date logic (KosherJava), with no new conversion code.
- Fast navigation to distant years, so years far away don't take many single-step taps (this covers the date-entry goal of TASK-25).
- Year bounds follow the single year-range policy from TASK-24. Opening an existing event or birthday whose date is valid but outside the quick range must not silently change it.
- Each call site keeps its current semantics: events and birthdays store a Hebrew date (with existing Adar/yahrzeit rules); Go-to-date navigates the calendar; the developer override sets a fixed device date.
- English and Hebrew UI with correct RTL, Material 3 theming including holiday themes and dark mode, TalkBack labels for every control, and keyboard operation.
- MVI: picker state is testable outside Compose.

Related: TASK-24 (year-range policy; resolve first or together), TASK-25 (add-event form and date-entry UI; its date-entry part is covered here, so reconcile its scope).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Variant 1b design is attached to the task and its layout/interactions are written into the description before implementation
- [ ] #2 One shared dual-calendar picker is used by the add/edit event editor, the contact birthday editor, the Go-to-date dialog and the developer date override; the old pickers are removed
- [ ] #3 Hebrew and Gregorian selections stay in sync; leap Adar, 29/30-day months and year bounds (TASK-24 policy) are handled; existing out-of-range dates are preserved
- [ ] #4 Unit tests cover picker state (sync, leap years, month lengths, bounds); existing event/birthday/go-to-date tests pass
- [ ] #5 Emulator verification with screenshots compared to the design: English/Hebrew, light/dark, a holiday theme, TalkBack labels, every call site
<!-- AC:END -->
