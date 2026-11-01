---
id: TASK-57.8
title: Hebrew-mode month title mixes scripts ("5787 חשון")
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-01T11:34:00Z'
updated_date: '2026-11-01T15:36:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
parent_task_id: TASK-57
priority: medium
type: bug
ordinal: 78000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
In the English UI, the Hebrew-mode month title mixes scripts: "5787 חשון" (Hebrew month name, Arabic year, and reads awkwardly in an LTR bar). The Hebrew UI is already right ("חשון תשפ״ז").

Agreed formats (owner, 2026-11-01):
- English UI: transliterated month, then the year in digits — "Cheshvan 5787" (and "Adar I 5787", "Adar II 5787"), using the same transliterated month names the header's month-range line already uses.
- Hebrew UI: unchanged — "חשון תשפ״ז".

Where: CalendarHeaderFormatter.hebrewMonthTitle. It is also used by the Agenda month headers (AgendaFormatter) and the Month home-screen widget (MonthWidget), so those change too — this also resolves the mixed-script widget header noted in TASK-57.21 W3. Update the KDoc example and tests/snapshots for all three.

Out of scope: the date picker. Its Hebrew-led title is written in Hebrew letters in both UI languages by design (1b) and uses the long year form "ה׳תשפ״ז"; leave it unchanged.

Findings (from specs/reviews/2026-11-01-figma-design-review.md):
- **C4** (Medium) — Month header, Hebrew mode (21:307). Title "5787 חשון" mixes Arabic numerals with a Hebrew month name and reads awkwardly in an LTR bar; the date picker shows the same year as "ה׳תשפ״ז". *Why:* Inconsistent year representation. *Reviewer suggestion:* EN UI: "Cheshvan 5787" (transliterated month). HE UI: "חשון תשפ״ז". Keep numerals consistent with the picker.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Hebrew-mode month title reads "Cheshvan 5787" in the English UI and "חשון תשפ״ז" in the Hebrew UI, including leap-year Adar I/II
- [x] #2 Calendar header, Agenda month headers and the Month widget all use the new format, covered by unit tests and snapshots
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. CalendarHeaderFormatter.hebrewMonthTitle: delegate to hebrewMonthRange(year, month, year, month, locale) so the English UI uses HebrewMonth.englishName (Cheshvan, Adar I/II) + digit year, and the Hebrew UI keeps KosherJava month name + gematria year. Update KDoc example.
2. Unit tests: replace the old title test with English/Hebrew cases for Cheshvan 5787 and leap-year Adar I/II 5787; check AgendaFormatter/MonthWidget tests for expectations of the old format.
3. Re-record Roborazzi baselines; confirm only calendar header (Hebrew mode), Agenda Hebrew-month headers and Month widget snapshots change.
4. Build/test/lint + verifyRoborazziDebug; runtime check on own emulator (Month Hebrew mode + Agenda, EN and HE UI), screenshots to /Users/daniel/.paseo/evidence/task-57.8/.
5. Record evidence in task notes and commit.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Tested commit: c92598e.

Change: CalendarHeaderFormatter.hebrewMonthTitle now delegates to the single-month case of hebrewMonthRange, so the English UI uses HebrewMonth.englishName + digit year ("Cheshvan 5787", "Adar I 5787", "Adar II 5787", "Adar 5786") and the Hebrew UI keeps KosherJava name + gematria ("חשון תשפ״ז", "אדר ב׳ תשפ״ז"). Agenda month headers and the Month widget pick this up through the shared formatter. Date picker untouched.

Tests: new CalendarHeaderFormatterTest cases (EN/HE, Cheshvan, leap-year Adar I/II, ordinary Adar); AgendaFormatterTest and AgendaScreenshotTest updated to "Cheshvan/Kislev 5787". Re-recorded Roborazzi: only app_calendar_hebrew_mode_en_* and app_agenda{,_drawer,_month_header,_no_calendar,_search}_en_* changed; no Hebrew-UI or picker baseline changed.

Results: assembleDebug, testDebugUnitTest (1151 tests), lintDebug and verifyRoborazziDebug all pass. One earlier run had a DatePickerScreenshotTest timeout ('Select date') while an emulator was booting in parallel; it passed in isolation and on the full rerun — unrelated contention, not this change.

Runtime (own emulator Levana_Agent_Template_API_36_1, debug build of c92598e), screenshots in /Users/daniel/.paseo/evidence/task-57.8/:
- month_hebrew_mode_en.png — "Cheshvan 5787"
- month_adar_i_en.png / month_adar_ii_en.png — "Adar I 5787" / "Adar II 5787"
- agenda_hebrew_mode_en.png — headers "Cheshvan 5787", "Kislev 5787"
- month_hebrew_mode_he.png / agenda_hebrew_mode_he.png — "חשון תשפ״ז", "כסלו תשפ״ז" (unchanged)

Limitation: the Month home-screen widget was not checked on device (placing a widget needs launcher interaction) and it has no Roborazzi baseline; it calls the same CalendarHeaderFormatter.hebrewMonthTitle, which the unit tests cover.
<!-- SECTION:NOTES:END -->
