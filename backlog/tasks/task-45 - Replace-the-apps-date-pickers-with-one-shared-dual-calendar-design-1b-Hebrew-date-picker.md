---
id: TASK-45
title: >-
  Replace the app's date pickers with one shared dual-calendar (design 1b)
  Hebrew date picker
status: To Do
assignee: []
created_date: '2026-10-29T21:22:00Z'
updated_date: '2026-10-29T21:29:00Z'
labels: []
dependencies:
  - TASK-24
priority: medium
type: feature
ordinal: 49000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner request 2026-10-29. Implement variant **1b "Dual calendar"** from the Claude Design "Hebrew date picker" mockups and make it the app's **single** date picker.

**Design source**
- Claude Design project (needs login, agents can't open it): https://claude.ai/design/p/fe15c347-4eef-410e-9639-d561a1ff21b0?file=Hebrew+Date+Picker.dc.html
- Export supplied by the owner: `/Users/daniel/.paseo/uploads/upload_0e207f57-2a44-49ec-a29f-7146dcb2479e/Jewish Calendar App Design.html`. It is a self-unpacking bundle; open it in Chrome to use the live mockups. Variants 1a, 1c and 1d on the same page are NOT chosen.
- Rendered references (coordinator, 2026-10-29): `/tmp/levana-design-45/crop1b.png` (variant 1b at 2x) and `/tmp/levana-design-45/full.png` (all four variants). If /tmp was cleared, re-render the export with headless Chrome.

**Variant 1b spec** (read from the export's markup and logic)
- **Modal dialog** in M3 style, rounded, over a scrim.
- **Header row:** "Select date" label on one side; a two-segment toggle **Hebrew | Gregorian** on the other that switches which calendar *leads* the grid.
- **Month header:**
  - Large leading-calendar title with a dropdown arrow. Hebrew mode: Hebrew month and year in Hebrew letters (e.g. חשוון ה׳תשפ״ז). Gregorian mode: e.g. "October 2026".
  - Below it, the other calendar's matching span. Hebrew mode shows the Gregorian range (e.g. "Oct – Nov 2026"). Gregorian mode shows the Hebrew month(s) the Gregorian month spans (e.g. "Tishrei – Cheshvan 5787").
  - Previous/next chevrons step one month of the leading calendar.
- **Year view:** tapping the title toggles between the day grid and a scrollable year list (arrow flips up/down; chevrons hidden). The list scrolls to the selected year; the selected and current years are visually marked. Hebrew years show the Hebrew-letter year plus the number (e.g. ה׳תשפ״ז 5787); Gregorian years show the number. Picking a year returns to the day grid with the same month kept, adjusted if that month doesn't exist in the new year (e.g. Adar I/II vs Adar). The mockup's ranges were 5600–5900 / 1840–2140; real bounds come from the TASK-24 year-range policy.
- **Day grid:** 7-column week grid starting Sunday, with weekday initials. Each cell has two lines:
  - Hebrew mode: the Hebrew day in letters (large) over the Gregorian day (small). The Gregorian line adds the month name on the 1st of a Gregorian month and on the first cell (e.g. "12 Oct", "1 Nov").
  - Gregorian mode: the Gregorian number (large) over the Hebrew day in letters (small), with the Hebrew month name on Hebrew day 1.
- **Selection:** a filled dark circle (theme primary/inverse) with light text. **Today** has its own distinct marker when not selected.
- **Holiday dots:** a small dot on days with a holiday or observance, coloured by holiday category (white on the selected day).
- **Footer:**
  - A summary of the selected date in both calendars, the leading calendar first (e.g. "י״ח חשוון  Thu, 29 Oct 2026").
  - When the selected day has a holiday, a small tinted chip with the category icon and holiday name.
  - **Cancel** and **OK** text buttons.
- **Visual language:** follow the app's Material 3 theme and the existing calendar screen rather than the mockup's literal hex values and fonts. The mockup's neutral slate palette and Manrope font are placeholders. Holiday-dot colours, holiday names and icons reuse the app's existing holiday categories, colours and strings.

**Scope:** replace both of today's date-entry UIs with this one shared picker component.
1. The stepper-based Hebrew day/month/year picker in the add/edit personal event screen and the contact birthday editor.
2. The calendar's Go-to-date dialog (separate Hebrew and Gregorian rows with dropdowns and text fields), which the developer date override also reuses.

Remove the old implementations once nothing uses them. Do not keep two pickers.

**Requirements**
- Both calendars stay in sync: switching the leading calendar keeps the selected day and shows the month containing it.
- Hebrew month handling covers leap years (Adar I/II vs Adar) and 29/30-day months. Use the app's existing Hebrew date logic (KosherJava); add no new conversion code.
- Holidays come from the app's existing holiday calculation. Israel/Diaspora follows the resolved device setting (TASK-5 rule).
- "Today" comes from the device context (TASK-5), including the developer override.
- Year bounds follow the single year-range policy from TASK-24. Opening an existing event or birthday whose stored date is valid but outside the quick range must not silently change it.
- Each call site keeps its current meaning:
  - events and birthdays store a Hebrew date (existing Adar/yahrzeit rules apply);
  - Go-to-date navigates the calendar;
  - the developer override sets a fixed device date.
- The initial leading calendar is chosen sensibly per call site (e.g. Hebrew for events and birthdays; the calendar screen's current mode for Go-to-date).
- English and Hebrew UI with correct RTL; holiday themes and dark mode.
- TalkBack: each day cell announces both dates, and holiday and today/selected state. Every control is labelled and keyboard-operable.
- MVI: picker state and logic are testable outside Compose.

Related: TASK-24 (year-range policy; resolve first or together). TASK-25 was closed as superseded by this task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 One shared dual-calendar picker implementing variant 1b (Hebrew/Gregorian leading toggle, two-line day cells, year list, holiday dots, footer summary with holiday chip) is used by the add/edit event editor, the contact birthday editor, the Go-to-date dialog and the developer date override; the old pickers are removed
- [ ] #2 Hebrew and Gregorian stay in sync across mode switches; leap Adar, 29/30-day months and TASK-24 year bounds are handled; existing out-of-range stored dates are preserved
- [ ] #3 Today, holidays and Israel/Diaspora come from the device context and existing holiday logic; colours, icons and strings reuse the app's theme and holiday categories
- [ ] #4 Unit tests cover picker state (sync, mode switch, year pick with month fitting, leap years, month lengths, bounds); existing event/birthday/go-to-date tests pass or are deliberately updated
- [ ] #5 Emulator verification with screenshots compared to the 1b reference: every call site, English/Hebrew, light/dark, a holiday theme, a leap year (e.g. 5787), year view, and TalkBack labels
<!-- AC:END -->
