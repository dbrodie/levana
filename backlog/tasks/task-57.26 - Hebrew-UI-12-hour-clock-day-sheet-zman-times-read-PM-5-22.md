---
id: TASK-57.26
title: 'Hebrew UI, 12-hour clock: day-sheet zman times read ''PM 5:22'''
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-01T18:03:00Z'
updated_date: '2026-11-02T14:16:00Z'
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
- [x] #1 In the Hebrew UI with a 12-hour clock, every displayed time reads "5:22 PM" (not "PM 5:22"): day sheet, Zmanim screen, Agenda, widgets, tile and notifications
- [x] #2 All time formatting goes through one shared helper, covered by unit tests; 12/24-hour behaviour per surface is unchanged
- [x] #3 Verified on the emulator in Hebrew and English UI, 12- and 24-hour, with Roborazzi baselines updated only where intended
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Shared helper: ui/ClockTime.kt `formatClockTime(time: LocalTime, use24Hour, hebrewUi, markerLocale: Locale? = null)` with the one pair of "h:mm a" / "HH:mm" formatters. In the Hebrew UI it wraps the result in a bidi isolate so "5:22 PM" keeps its order in RTL text/layout. Use FIRST STRONG isolate (U+2068…U+2069) rather than LRI: identical to LRI for "5:22 PM" / "17:22" (no strong char → LTR), but stays correct if the AM/PM marker is localised to Hebrew (notifications format 12h with the app locale, which can give "5:22 אחה״צ"; an LRI would flip that to read marker-first). MoladText's ltr() moves onto the helper.

Call sites and their current 12/24 rule (kept unchanged):
1. ui/MoladText.moladTimeText / agendaMevarchimText — caller's use24Hour; already isolated (LRI) in Hebrew.
2. ui/daydetail/DayDetailScreen — QuickZmanimCards, HalachicTimesSection (zman rows), ShabbatTimeCard (candles/Havdalah), SystemEventsSection (event start–end), molad line: device DateFormat.is24HourFormat.
3. ui/zmanim/ZmanimScreen — device setting.
4. ui/agenda/AgendaScreen.cardContent (candles, havdalah, Shabbat havdalah line, mevarchim; also TalkBack descriptions) — use24Hour override (walkthrough passes true) ?: device setting.
5. widget/WidgetCommon.timeFormatter → ZmanimWidget rows, TodayWidget next zman / candles / havdalah — device setting.
6. notifications/NotificationTexts.notificationTime — Hebrew: device setting; English: always 12h; 12h markers in the app locale.
7. ui/settings/SettingsTimeFormat.morningTimeFormatArgs — string-resource stepper value, not a LocalTime: English fixed 12h "AM", Hebrew 24h digits only (no bidi issue). Left as is; noted as an inconsistency for TASK-57.11.
Quick tile shows no times.

Steps: add helper + unit tests (en/he × 12/24, marker locale, isolate); route 1–6 through it; update MoladText/NotificationTexts tests for the isolate; build/test/lint/verifyRoborazzi, re-record only intended baselines; emulator check He 12h (day sheet, Zmanim, Agenda, widgets, notification), En 12h and He 24h spot checks.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Tested commit: e786d73 (code). Branch task-57.26-hebrew-time-bidi.

What changed
- New ui/ClockTime.kt `formatClockTime(time, use24Hour, hebrewUi, markerLocale = null)`: the one "h:mm a" / "HH:mm" pair; in the Hebrew UI wraps the time in a FIRST-STRONG isolate U+2068…U+2069 (built from Char constants; a literal isolate in a string trips lint BidiSpoofing). FSI instead of LRI: identical for "5:22 PM"/"17:22", and still right if a Hebrew AM/PM marker ever appears (notifications format 12h in the app locale; on the API 36 emulator `he` still yields "PM").
- Routed through it: MoladText (replaces its private LRI), DayDetailScreen (quick cards, zman rows, Shabbat candle/Havdalah cards, device-event start–end, molad), ZmanimScreen, AgendaScreen.cardContent (candles, havdalah, Shabbat havdalah line, device events, TalkBack descriptions, mevarchim), WidgetCommon.widgetTimeFormat (replaces timeFormatter; Zmanim + Today widgets), NotificationTexts.notificationTime. No other ofPattern("h:mm…")/("HH:mm") remain in app/src/main.
- 12/24-hour rule per caller unchanged (see plan). Tests: new ClockTimeTest (en/he × 12/24, marker locale); MoladText/NotificationTexts/PostMidnightHavdalah expectations now include the isolate.

Verification
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug`: BUILD SUCCESSFUL on e786d73, 1170 tests. No Roborazzi baseline changed: Robolectric renders Hebrew on a 24-hour clock, and the isolate is invisible. FoldContinuityTest failed once in an earlier run and passed on rerun (flaky, unrelated).
- Emulator (own session, API 36.1, Hebrew app language, device 12h), evidence in /Users/daniel/.paseo/evidence/task-57.26/:
  01-he-12h-daysheet-shabbat.png: zman rows, Havdalah, quick cards, molad all "5:22 PM" (was "PM 5:22", cf. task-57.6/09).
  02-he-12h-daysheet-friday.png: candle-lighting card "4:27 PM".
  03-he-12h-zmanim.png: Zmanim screen.
  04-he-12h-agenda.png: Agenda candles, "הבדלה 5:22 PM", mevarchim/molad.
  05-he-12h-today-widget.png and 06-he-12h-zmanim-widget.png: Glance widgets render the isolate correctly ("יום ו׳ 4:27 PM").
  07-he-24h-daysheet-shabbat.png: Hebrew 24h unchanged.
  08-he-24h-notification.png and 09-he-12h-notification.png: candle-lighting notification. Text is "הדלקת נרות בשעה ⁨4:22 PM⁩ · צאת השבת ⁨5:18 PM⁩", rendered in the right order. The second time wraps between "5:18" and "PM"; order stays correct.
  10-en-12h-daysheet.png: English 12h unchanged.
- TalkBack: Agenda content descriptions now contain the isolate characters (as the molad already did). They are invisible format characters (Cf) that TTS skips. Not checked with TalkBack speaking.
- Tile: shows no times.

Limitations / rejected
- Tried a no-break space in the 12h pattern to stop the notification wrap. Reverted: on the 200% font Fold cover screen the quick cards then broke inside the marker ("6:08 A / M"), which the baseline foldable_font200_calendar_fold8_ultra_cover caught.
- Notifications were posted using Developer settings → Date Override (Friday) and an app relaunch.

Inconsistencies for TASK-57.11 (left as is)
- Notifications in English are always 12h whatever the device setting. In Hebrew they follow the device. The AM/PM marker uses the app locale; screens use the default locale.
- Agenda allows a use24Hour override (the walkthrough passes true).
- Settings morning-time stepper (SettingsTimeFormat.morningTimeFormatArgs, string resource): English always "h:mm AM", Hebrew always 24h. It is not a LocalTime, so it doesn't go through the helper.
- Walkthrough illustration quick cards use hard-coded sample strings ("6:12", "16:23"), not the device setting.

New task: TASK-62 (Today widget Hebrew date line '· Jerusalem' bidi order).
<!-- SECTION:NOTES:END -->
