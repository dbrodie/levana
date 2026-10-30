---
id: TASK-45
title: >-
  Replace the app's date pickers with one shared dual-calendar (design 1b)
  Hebrew date picker
status: Done
assignee:
  - '@claude'
created_date: '2026-10-29T21:22:00Z'
updated_date: '2026-10-30T07:33:00Z'
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
- Rendered references (coordinator, 2026-10-29): `/tmp/levana-design-45/crop1b.png` (variant 1b at 2x), `/tmp/levana-design-45/full.png` (all four variants) and the interaction states in `/tmp/levana-design-45/states/` (listed below). They were driven with `/tmp/levana-design-45-scripts/drive.mjs` (Node + headless Chrome via DevTools protocol: `node drive.mjs <export.html> <outDir> '<steps json>'`). If /tmp was cleared, copy the export to a fresh dir and re-run it.

**Variant 1b spec** (read from the export's markup and logic)
- **Modal dialog** in M3 style, rounded, over a scrim.
- **Header row:** "Select date" label on one side; a two-segment toggle **Hebrew | Gregorian** on the other that switches which calendar *leads* the grid.
- **Month header:**
  - Large leading-calendar title with a dropdown arrow. Hebrew mode: Hebrew month and year in Hebrew letters (e.g. חשוון ה׳תשפ״ז). Gregorian mode: e.g. "October 2026".
  - Below it, the other calendar's matching span. Hebrew mode shows the Gregorian range (e.g. "Oct – Nov 2026"). Gregorian mode shows the Hebrew month(s) the Gregorian month spans (e.g. "Tishrei – Cheshvan 5787").
  - Previous/next chevrons step one month of the leading calendar.
- **Year view:** tapping the month title switches the grid area to a year list; see "Interaction behavior" below. Real bounds come from the TASK-24 year-range policy (the mockup used 5600–5900 / 1840–2140).
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

**Interaction behavior** (observed by driving the live mockup, 2026-10-29; screenshots in `/tmp/levana-design-45/states/`)
- **Hebrew-led** (`01-hebrew-initial`): title "חשוון ה׳תשפ״ז", subtitle "Oct – Nov 2026". Cells are Hebrew letters over the Gregorian day; the Gregorian month name appears on Hebrew day 1 and on the 1st of a Gregorian month ("12 Oct", "1 Nov"). The footer reads "י״ח חשוון  Thu, 29 Oct 2026".
- **Gregorian-led** (`02-gregorian-initial`): title "October 2026", subtitle "Tishrei – Cheshvan 5787" (Hebrew months transliterated in the subtitle). Cells are the Gregorian number over the Hebrew day in letters, with the Hebrew month name on Hebrew day 1 (e.g. "חשוון" on 12 Oct). The footer flips its order: "Thu, 29 Oct 2026  י״ח חשוון". The toggle highlights the active segment.
- **Month navigation** (`03-gregorian-next-month`): the chevrons step the leading calendar's month (October → November 2026; the subtitle becomes "Cheshvan – Kislev 5787"). In Hebrew-led mode they step Hebrew months, including Adar I → Adar II in a leap year (`10-hebrew-adar-i-5787-leap`, `11-…`).
- **Selecting** (`04-gregorian-pick-nov15`): tapping a day selects it (filled circle) and updates the footer immediately. Nothing is committed until OK; Cancel discards.
- **Switching the leading calendar re-anchors on the selection** (`05-hebrew-after-switch`, `13-gregorian-after-switch-purim`): after picking 15 Nov 2026 in Gregorian mode, switching to Hebrew shows Kislev 5787, the month containing the selection, not the previously viewed Hebrew month. Switching back to Gregorian after browsing to another Hebrew year likewise shows March 2027, where the selection is.
- **Year list** (`06-hebrew-year-list`, `08-gregorian-year-list`): tapping the title (arrow flips to ▲, chevrons hide, subtitle stays) replaces the grid with a 3-column scrollable grid of years. It is **single-select**, not multi-select. Hebrew mode shows the Hebrew-letter year over the number (ה׳תשפ״ז / 5787); Gregorian mode shows numbers only. The list opens scrolled to the viewed year, which is a filled pill; the current year has its own marker. The footer and the selection stay visible.
- **Picking a year changes only the viewed month, not the selection** (`07-hebrew-after-pick-5784`, `09-gregorian-after-pick-2030`, `12-hebrew-pick-5786-month-fit`): the grid returns showing the same month in the chosen year (Kislev 5787 → Kislev 5784, Nov 2026 → Nov 2030), with no selected cell visible if the selection lies elsewhere. The footer still shows the old selection until the user taps a day.
- **Month fitting across leap years** (`12-hebrew-pick-5786-month-fit`): viewing Adar II 5787 (leap) and picking 5786 (non-leap) shows "אדר ה׳תשפ״ו" (plain Adar). The reverse direction (non-leap → leap) uses the same month-fitting logic in the mockup code but was not rendered. The selection (14 Adar II 5787) is unchanged.
- **Holidays** (`11-hebrew-adar-ii-purim-selected`, `13-…`): days with a holiday or fast have a category-coloured dot under the date (white on the selected day; e.g. a dark dot for Ta'anit Esther, an accent dot for Purim/Shushan Purim, Sukkot and Chanukah). Selecting a holiday shows a tinted chip with its icon and name under the footer summary ("Purim").
- **Leap-year labels**: "אדר א׳ / אדר ב׳" in Hebrew; "Adar I – Adar II 5787" in the Gregorian subtitle.

**Open product question** (implementing agent: confirm with the owner before coding). In the mockup, picking a year only moves the view, and the selection stays put until a day is tapped. The alternative is to move the selection to the same day and month in the new year. The mockup's behavior is the default unless the owner decides otherwise.

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
- [x] #1 One shared dual-calendar picker implementing variant 1b (Hebrew/Gregorian leading toggle, two-line day cells, year list, holiday dots, footer summary with holiday chip) is used by the add/edit event editor, the contact birthday editor, the Go-to-date dialog and the developer date override; the old pickers are removed
- [x] #2 Hebrew and Gregorian stay in sync across mode switches; leap Adar, 29/30-day months and TASK-24 year bounds are handled; existing out-of-range stored dates are preserved
- [x] #3 Today, holidays and Israel/Diaspora come from the device context and existing holiday logic; colours, icons and strings reuse the app's theme and holiday categories
- [x] #4 Unit tests cover picker state (sync, mode switch, year pick with month fitting, leap years, month lengths, bounds); existing event/birthday/go-to-date tests pass or are deliberately updated
- [x] #5 Emulator verification with screenshots compared to the 1b reference: every call site, English/Hebrew, light/dark, a holiday theme, a leap year (e.g. 5787), year view, and TalkBack labels
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Shared picker: new package ui/datepicker (MVI), replaces GoToDateDialog and the editors' stepper HebrewDatePicker.

1. Pure logic (no Compose, no Android): DatePickerState + DatePickerIntent + DatePickerReducer.
   - State: selection (LocalDate), leading calendar (HEBREW/GREGORIAN), viewed month (Hebrew year+month or YearMonth), year list open, today, allowed Hebrew years.
   - Intents: SetLeading, PreviousMonth, NextMonth, SelectDay, ToggleYearList, PickYear.
   - Switching the leading calendar re-anchors the view on the selection's month and closes the year list.
   - Chevrons step the leading calendar's months (HebrewYearMonth.next/previous for Adar I/II).
   - PickYear moves the selection to the same day and month in that year (owner decision 2): Hebrew via HebrewDateMatcher.monthForYear (Adar II -> Adar) and clampDay (30 -> 29); Gregorian 29 Feb -> 28 Feb. The view follows the selection and the year list closes.
   - Bounds: selectable days are those whose Hebrew year is in SupportedHebrewYears (5600-6000), plus the initial date's Hebrew year when it lies outside. That way an out-of-range stored date opens unchanged, can be browsed within its own year, and the year list moves it into range. Month steps stop at the bounds. Year lists: 5600-6000 (Hebrew) and SupportedHebrewYears.gregorianYears 1840-2239 (Gregorian).
2. DatePickerViewModel (Koin): Open(initialDate, leading) reads today, Israel/Diaspora (resolveIsInIsrael with the device zone) and the modern-holidays preference from DeviceContext/preferences once. It runs intents through the reducer and derives the month cells from CalendarRepository.getMonthDays/getHebrewMonthDays (existing holiday logic).
3. UI: HebrewDatePickerDialog (variant 1b), styled with the app's M3 colour scheme (holiday themes, dark mode).
   - Segmented Hebrew|Gregorian toggle; month title with dropdown arrow and the other calendar's span (reusing CalendarHeaderFormatter.hebrewMonthRange); chevrons.
   - 7-column Sunday-first grid with two-line cells, today ring, filled primary selection and category-coloured holiday dots (holidayCategoryColor).
   - 3-column year grid opened on the viewed year, with a current-year marker.
   - Footer with both dates (leading first), a holiday chip (localized name + category icon), Cancel/OK, and an optional extra action ("Clear" for the developer override).
   - Strings in English and Hebrew, correct RTL. TalkBack: each cell describes both dates, the holiday and "today", with selected state; all controls are labelled buttons, so they are keyboard focusable.
4. Call sites:
   - Event and birthday editors: the inline stepper becomes a date field card (Hebrew date + Gregorian equivalent) that opens the picker in Hebrew mode. VM intents OpenDatePicker / DatePicked(LocalDate) / CloseDatePicker, with the picker shown from VM state.
   - DatePicked equal to the shown date is a no-op, so an out-of-range stored date (TASK-24 preservedDate) stays untouched until a different date is picked. Otherwise it sets the date (a known year is kept, other years are coerced) and clears preservedDate.
   - SetHebrewDay/Month/Year are replaced by SetHebrewDate(day, month, year); the birthday editor's edited-while-loading merge stays field-wise.
   - Go-to-date: the calendar's current mode leads; OK navigates (CalendarIntent.GoToDate).
   - Developer override: Gregorian leads; OK sets the override, Clear removes it.
5. Remove GoToDateDialog.kt, HebrewDatePicker/NumberPicker/MonthPicker/buildMonthList (moving EditorMonthNamesTest coverage to the picker's month names), and strings left unused.
6. Tests:
   - Reducer unit tests: sync/mode switch, month stepping incl. Adar I/II, 29/30-day months, year pick with month/day fitting both directions, Feb 29, bounds and the out-of-range initial date.
   - VM tests: today/Israel from DeviceContext, holiday on Purim, Shushan Purim in Israel.
   - Editor VM tests migrated to the new intents, plus picker no-op/preservation cases.
   - Roborazzi: run verify first and list the diffs; re-record and inspect them; add picker baselines (Hebrew-led, Gregorian-led, year list, Purim 5787 Adar II chip, out-of-range stored date) x en/he x light/dark.
7. Verify: assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks. Emulator screenshots of every call site, both modes, year list, leap Adar + chip, en/he, light/dark, and TalkBack labels via the UI dump. Save them under /tmp/levana-task45-evidence, then record the evidence in the notes.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Owner decisions 2026-10-30:
1. Year range: 5600–6000 Hebrew (from TASK-24's single policy). The Gregorian year list covers the matching span.
2. Picking a year in the year list MOVES THE SELECTION to the same day and month in that year (adjusting Adar I/II/Adar and day 30 → 29 when needed), and the grid shows it. This replaces the mockup's view-only behavior and resolves the open question.
3. Initial leading calendar per screen: Hebrew for the event and birthday editors; the calendar screen's current mode for Go-to-date; Gregorian for the developer date override.
4. The owner wants screenshots of the finished picker: every call site, Hebrew and Gregorian modes, the year list, a leap-year Adar and a holiday chip, in English and Hebrew, light and dark.

Implementation (commits 2b5fa58, 0429929; plan 1213df3):
- New package ui/datepicker:
  - DatePickerState/Intent, plus DatePickerReducer (pure rules: sync, mode switch, month stepping, year pick with fitting, bounds).
  - DatePickerViewModel (Koin): reads today/Israel/modern-holidays once per session from DeviceContext and preferences; month cells and the selected day come from CalendarRepository.
  - DatePickerFormatter for the text; HebrewDatePickerDialog (variant 1b UI); HebrewDateField (the editors' date card).
- Call sites:
  - Event and birthday editors: a date field opens the picker, Hebrew-led. VM intents OpenDatePicker / DismissDatePicker / SetHebrewDate(day, month, year), the last with a LocalDate constructor. They replace SetHebrewDay/Month/Year.
  - Go to date: the calendar's mode leads; OK navigates.
  - Developer override: Gregorian-led, with a Clear action.
- Removed: GoToDateDialog.kt; the stepper HebrewDatePicker/NumberPicker/MonthPicker/buildMonthList; EditorMonthNamesTest (its coverage moved to DatePickerFormatterTest); 11 strings left unused (calendar_gregorian_date, calendar_invalid_date, calendar_day/month/year, calendar_cancel, event_date_picker_day/month/year, event_increase_value, event_decrease_value).

Behaviour decisions within the spec:
- Bounds: a day is pickable when its Hebrew year is in SupportedHebrewYears (5600-6000), or is the Hebrew year of the initial date when that lies outside the range. Chevrons stop at those bounds. Year lists: 5600-6000 (Hebrew) and SupportedHebrewYears.gregorianYears 1840-2239 (Gregorian); the list moves an out-of-range date into the range.
- A stored out-of-range date (TASK-24):
  - It opens unchanged, and confirming the shown date is a no-op in the editor VM, so preservedDate survives and Save writes the stored values.
  - Picking another day in that year keeps the year (as TASK-24's day change did); any other year is coerced into the range.
- Year pick (owner decision 2): Hebrew uses HebrewDateMatcher.monthForYear (Adar II -> Adar; Adar stays Adar, i.e. Adar I in a leap year) and clampDay (30 -> 29). Gregorian 29 Feb -> 28 Feb.
- Birthday edited-while-loading merge stays field-wise: the fields of a picked date that differ from the shown date count as edited, which keeps VerifyBirthday* behaviour.
- Departures from the mockup:
  - The Hebrew/Gregorian toggle reuses the calendar's CalendarModeSelector (M3 segmented buttons with a check mark).
  - Hebrew month spellings come from KosherJava ("חשון").
  - Hebrew years are written "ה׳תשפ״ז" (KosherJava's space removed; "ו׳ אלפים" for 6000).
  - The footer shows the Hebrew year too and wraps to two lines.
  - The app has no holiday icons, so the chip icons are chosen per category: Torah AutoAwesome, Rabbinic Celebration, Fast NoFood, Minor Event, Modern Israeli Flag. Colours come from holidayCategoryColor.
- TalkBack: each day cell's description reads "<full Gregorian date>, <Hebrew date>, <holiday>, Today", with selected state. The chevrons, title (click label "Choose year"/"Choose day"), date field ("Change date") and buttons are labelled; everything is a clickable/focusable element.

Verification at 0429929:
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks`: BUILD SUCCESSFUL. 922 unit tests, 0 failures/errors. Lint 0 errors, no new warnings in touched files; the old GoToDateDialog ModifierParameter warning is gone. Roborazzi 156 total, 156 unchanged.
- New tests:
  - DatePickerReducerTest (21): sync/re-anchor, Adar I/II stepping, ordinary-year Adar, Elul->Tishrei, Gregorian year wrap, select, year pick in both directions with Adar and day-30 fitting, 29 Feb, year lists, bounds at 5600/6000 in both calendars, out-of-range initial date in both calendars.
  - DatePickerFormatterTest (9): titles, spans, footer, cell labels, TalkBack description, month names vs HebrewMonth/KosherJava.
  - DatePickerViewModelTest (6): today incl. developer override, holidays and chip, month lengths 30/29/28, Israel vs Diaspora (16 Nissan), selection outside the shown month, session reset.
  - Editor VM tests: the picker no-op keeps out-of-range dates (both editors), a picked day in 5500 is saved in 5500, a picked Purim is saved as 14/13/5787, dismiss changes nothing.
  - Existing editor tests were migrated mechanically through test helpers pickDay/pickMonth/pickYear (ui/EditorDatePicks.kt).
- Roborazzi: the first verify --rerun-tasks before re-recording changed exactly 12 images: app_add_event, app_edit_event and app_contact_birthday, each in _en_light/_en_dark/_he_light/_he_dark. The compare images show only the date card changed (stepper -> date field), with the content below moved up. The app_contact_birthday test no longer steps the year; it waits for 1 Tishrei of TODAY's year.
- Added 24 baselines (DatePickerScreenshotTest), each in en/he × light/dark:
  - picker_hebrew_led (7 Kislev 5787, today, Chanukah dots)
  - picker_year_list (Hebrew)
  - picker_gregorian_led
  - picker_purim_5787 (Adar II, Purim chip)
  - picker_out_of_range (stored 12 Nissan 5500)
  - picker_go_to_date
- Emulator (Levana_Agent_Template_API_36_1, headless, debug APK of 0429929): every call site in English light and Hebrew dark, with extra shots in Hebrew light and English dark. Covered:
  - both modes and both year lists;
  - year pick moving the selection (2030; Adar II 5787 -> Adar 5786 -> Adar I 5787);
  - Purim chip; Go to date navigating;
  - developer override set/Clear, Purim holiday theme and today ring under the override;
  - birthday saved as 9/7/5787;
  - stored 12/1/5500 birthday: opened, OK, Update; the contacts row is still 12/1/5500.
  TalkBack labels were confirmed from the UI dumps (accessibility tree), not by navigating with TalkBack on. The evidence index is /tmp/levana-task45-evidence/README.md (40 screenshots + XML dumps, final-verify.log).

Found during verification (not caused by this task):
- The app crashes on back-to-back locale + night-mode changes (ClassCastException DrawerValue -> Bundle in rememberNavController restore). Reproduced on master c063b34; filed as TASK-50.
- Observation, unverified: in Nissan 5500 (1740) the existing holiday logic marks a day around 28 Nissan, probably a modern Israeli day (Yom HaShoah) computed for a pre-1948 year. The picker shows what CalendarRepository returns; the calendar screen would show the same.
- Go to date opens on the calendar's selected day. That was the old dialog's behaviour too, and the selection does not follow a developer-override change.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-30T07:33:00Z

---

Coordinator integration 2026-10-30: reviewed screenshots (Purim 5787 chip, Hebrew year list, he-dark Gregorian-led, add-event date card) against design 1b; merged as ee4440d. Clean run: 922 tests, 0 failures; Roborazzi 156 unchanged (12 editor baselines intentionally re-recorded, 24 picker baselines added). Evidence: /tmp/levana-task45-evidence/README.md. Follow-ups: TASK-50 (config-change crash, pre-existing); possible anachronistic modern holiday in 5500; Go-to-date initial date ignores the dev override.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Replaced the event/birthday stepper picker and the calendar's Go-to-date dialog (also used by the developer override) with one shared MVI dual-calendar picker, design 1b.
- Behaviour: Hebrew/Gregorian leading toggle, two-line day cells, year lists, holiday dots, and a footer with both dates plus a holiday chip. Year picks move the selection, fitting Adar and day 30.
- Bounds and data: TASK-24 bounds with non-destructive handling of stored out-of-range dates. Today and Israel/Diaspora come from DeviceContext; holidays from CalendarRepository.
- Leading calendar: Hebrew in the editors, the calendar's mode for Go-to-date, Gregorian for the override.
- Removed the old pickers and their strings.
- Verification: 922 unit tests pass and lint has 0 errors. Roborazzi: 12 baselines deliberately re-recorded and 24 picker baselines added; 156 unchanged afterwards. Emulator screenshots at /tmp/levana-task45-evidence.
- Follow-up: TASK-50 (pre-existing config-change crash).
<!-- SECTION:FINAL_SUMMARY:END -->
