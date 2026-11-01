---
id: TASK-57.25
title: Hebrew years written the same way everywhere (date picker shows ה׳תשפ״ז)
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-01T15:23:00Z'
updated_date: '2026-11-01T15:38:00Z'
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
- [x] #1 Hebrew-letter years use the short form (e.g. תשפ״ז) everywhere in the app, including the date picker title, year grid and footer
- [x] #2 All Hebrew-letter year formatting goes through one shared helper, covered by unit tests
- [x] #3 Affected Roborazzi baselines updated intentionally and verified on the emulator
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Shared helper: new object domain/model/HebrewDateText.kt owning the app's only two KosherJava HebrewDateFormatter instances (Hebrew letters, short years, geresh/gershayim; and transliterated). It exposes functions only (no formatter), so nobody can reconfigure the year form: hebrewNumber, hebrewYear, hebrewMonth, hebrewDate(JewishDate), parsha(jc, hebrew), hebrewOmer, transliteratedDate, transliteratedMonth, date(jd, hebrew).

Audit of formatter instances and changes:
1. DatePickerFormatter: isUseLongHebrewYears=true + space strip -> removed; hebrewYear/hebrewMonthName/hebrewDate call HebrewDateText. Title, year grid and footer become short form (תשפ״ז). hebrewYear(6000) stays "ו׳ אלפים" (KosherJava's whole-thousand form in both modes).
2. CalendarHeaderFormatter: only the private formatter and yearLabel/hebrewMonthName bodies change to call HebrewDateText; English-UI behaviour unchanged (minimal edit for TASK-57.8 integration).
3. CalendarRepository: hebrew + translit formatters -> HebrewDateText (hebrewFormatted, transliterated, day number, parsha). Feeds widgets, day details, agenda, notifications.
4. EventsScreen formatHebrewDate: -> HebrewDateText.
5. HebrewMonth.hebrewName: -> HebrewDateText.hebrewMonth.
6. OmerCountText: -> HebrewDateText.hebrewOmer.
7. WalkthroughIllustrations: hebrewNumbers + per-call formatters -> HebrewDateText.
8. HebrewDateTileService: per-call formatter -> HebrewDateText.date / hebrewNumber.
9. Widgets / notifications: no own formatters; they use HebrewDay.hebrewFormatted/transliterated from CalendarRepository (no change).

Tests: HebrewDateTextTest (normal year 5787 -> תשפ״ז, 5600 -> ת״ר, 6000 -> ו׳ אלפים, number/date); DatePickerFormatterTest updated for short title/year grid/footer. Roborazzi: re-record only picker screenshots that show years. Runtime check on own emulator.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Tested commit: d0b84dd (code); this notes update is committed on top.

Changes: new domain/model/HebrewDateText.kt owns the app's only two KosherJava HebrewDateFormatter instances (Hebrew letters, short years; transliterated), exposing functions only. All 8 former instances (CalendarRepository x2, CalendarHeaderFormatter, DatePickerFormatter, EventsScreen, HebrewMonth, OmerCountText, WalkthroughIllustrations x3 incl. per-call, HebrewDateTileService) now call it; `grep HebrewDateFormatter app/src/main` only hits HebrewDateText.kt (plus a comment in HebrewDateMatcher). Widgets and notifications have no formatter of their own (they use HebrewDay strings from CalendarRepository). DatePickerFormatter drops isUseLongHebrewYears and the space-stripping; picker title, year grid and footer now read "תשפ״ז". 6000 stays "ו׳ אלפים" (KosherJava's whole-thousand form). CalendarHeaderFormatter edit is limited to the formatter field and the two private helpers; English-UI output unchanged. Corrected a doc comment in WalkthroughIllustrations that said חשוון (output is חשון).

Tests: HebrewDateTextTest (5787, 5775, 5600, 6000, numbers, whole dates Hebrew/transliterated, months, Omer); DatePickerFormatterTest expectations updated to the short form.

Verification: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug` BUILD SUCCESSFUL. Roborazzi: only picker baselines changed (picker_hebrew_led/gregorian_led/go_to_date/out_of_range/purim_5787/year_list en+he light+dark, foldable_picker*, foldable_font200_picker*); diffs inspected, they show only the year text (and footer Hebrew date).

Runtime (own headless emulator emulator-5556, Levana_Agent_Template_API_36_1, debug build of d0b84dd): English UI Hebrew mode -> picker title "חשון תשפ״ז", footer "כ״א חשון תשפ״ז", year grid "תש״פ… תשפ״ז"; Hebrew UI -> month title "חשון תשפ״ז", picker title/footer/year grid same short form.
Evidence: /Users/daniel/.paseo/evidence/task-57.25/ (en_hebrew_mode_month.png, en_picker_hebrew_led.png, en_picker_year_grid.png, he_hebrew_mode_month.png, he_picker_hebrew_led.png, he_picker_year_grid.png, baseline-old/ for previous baselines).

Limitations: quick tile not checked on device (same helper, short form unchanged from before). No new tasks filed: no user-visible month-spelling inconsistency found (all output is KosherJava's חשון). Integration note: TASK-57.8 also edits CalendarHeaderFormatter; on merge keep its title logic and call HebrewDateText.hebrewYear/hebrewMonth instead of a local HebrewDateFormatter.
<!-- SECTION:NOTES:END -->
