---
id: TASK-9.2
title: Month calendar home-screen widget
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:19:00Z'
updated_date: '2026-10-29T21:04:00Z'
labels: []
dependencies:
  - TASK-9.1
parent_task_id: TASK-9
priority: medium
type: feature
ordinal: 41000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Second subtask of TASK-9; see TASK-9 for product decisions and shared design. Uses the foundation from TASK-9.1. Month grid with Hebrew primary day numbers and Gregorian secondary, holiday markers, today highlighted, using the shared app/holiday colors. Header tap opens the calendar; day cell tap opens that day's details.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Month widget (about 4x4, resizable) shows the current Hebrew month with Gregorian secondary numbers, holiday markers and today highlighted, using the shared color scheme
- [x] #2 Refreshes on date, locale, time-zone, and relevant preference changes
- [x] #3 Day cell tap opens the calendar with that day selected; header opens month calendar on today
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Implementation plan (TASK-9.2)

1. **Shared grid model (no duplication)**: extract the calendar screen's inline week layout (Sunday-first leading blanks + chunk by 7) into a pure `monthGridWeeks(days: List<HebrewDay>): List<List<HebrewDay?>>` in `ui/calendar`. `GregorianMonthGrid`, `HebrewMonthGrid` and the widget all call it.
2. **Shared holiday-marker colors**: move the category→color mapping in `CalendarScreen.holidayCategoryColor` into a plain `holidayCategoryColor(category, scheme: ColorScheme)`. The screen passes `MaterialTheme.colorScheme`; the widget builds a day/night `ColorProvider` from `levanaColorScheme(false/true, holidayTheme)`.
3. **Snapshot** (`widget/MonthWidgetSnapshot.kt`, pure, unit-tested): `MonthWidgetSnapshots(calendarRepository).build(prefs, device: DeviceNow)`.
   - today = `device.today` (developer override included).
   - month = `HebrewYearMonth.from(today)`.
   - days = `CalendarRepository.getHebrewMonthDays(month, prefs.isInIsrael, prefs.showModernIsraeliHolidays)` (the resolved Israel/Diaspora preference, never `displayCity`).
   - weeks = `monthGridWeeks(days)`, `holidayTheme = activeHolidayTheme(prefs, today)`, `refreshAt = device.toSystemInstant(device.nextMidnight)`.
4. **Glance widget** (`widget/MonthWidget.kt`, `MonthWidgetReceiver`):
   - Header: Hebrew month title + Gregorian span, reusing `CalendarHeaderFormatter`.
   - Weekday row: Fri/Sat in primary, as in the app.
   - 7-column grid: Hebrew day primary, Gregorian day secondary, a holiday marker bar in the category color; today on primaryContainer, bold.
   - Mirrored like the Today widget when the app language direction differs from the device's.
   - `SizeMode.Responsive`: full (~4x4) and compact (down to 3x3; no Gregorian numbers or subtitle). `res/xml/month_widget_info.xml`: target 4x4, min resize 3x3.
5. **Taps**:
   - Header → calendar on today (the existing `EXTRA_DATE_EPOCH_DAY` deep link).
   - Day cell → the same deep link with that cell's date.
   - The calendar currently ignores the deep-link date (it always opens on today). Add an `initialDate` to `CalendarScreen` from `LevanaApp`: once the device date is loaded it sends `CalendarIntent.GoToDate`, once per launch (rememberSaveable guard). Notification and Today-widget deep links pass today or the notification day, so they now select that day too.
6. **Shared foundation (small, additive)**:
   - Move the Today widget's tap actions and mirror check into a shared `widget/WidgetCommon.kt`.
   - `WidgetUpdater` gets a per-receiver refresh intent and alarm (component-distinct PendingIntents, so the month's midnight alarm doesn't replace Today's next-zman alarm). `requestUpdate` broadcasts to every widget receiver.
   - A small `RefreshingWidgetReceiver` base holds the receive/disable logic both receivers share.
   - Manifest receiver + Koin single + en/he strings.
7. **Tests**: `MonthWidgetSnapshotsTest` and `monthGridWeeks` tests:
   - Month boundaries: first cell weekday, 29/30 days, 5–6 weeks, trailing padding.
   - A leap year: Adar I / Adar II 5787, Purim only in Adar II.
   - Today highlight / today in the month.
   - Israel vs Diaspora markers: 22 Nisan; 23 Tishrei.
   - The developer override's fixed date picks the month; the refresh time.
8. **Verify**:
   - build/test/lint.
   - android-debug on a dedicated emulator with the widget on a real home screen: en/he, light/dark, a forced holiday theme, resize limits, header and day-cell taps, a date change (dev override) and a time-zone change.
   - Evidence goes outside the worktree.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Verification (tested commit dd17d89)

**Build/test/lint**: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` → BUILD SUCCESSFUL; 736 tests, 0 failures/errors. No lint issues in the new code. The `CalendarScreen` ModifierParameter warning is pre-existing (optional callbacks come before `modifier`).

**New unit tests** (`widget/MonthWidgetSnapshotsTest`, 13):
- Month of today (18 Cheshvan 5787). Tishrei 5787 starts on Shabbat → 6 weeks, 6 leading and 6 trailing blanks.
- Every column is one weekday (Sunday first), and the grid holds exactly the month's days. A Sunday-start month (Nov 2026) has no leading blanks; an empty list gives no weeks.
- Leap 5787: Adar I has 30 days with Purim Katan; Adar II has 29 days with Purim. Common 5786: one Adar with Purim.
- Today appears once; today follows the device zone across midnight (NY vs Jerusalem).
- Israel vs Diaspora: 22 Nisan is Yom Tov only in the Diaspora; 23 Tishrei is Simchat Torah only in the Diaspora. Modern Israeli holidays follow the setting.
- The developer override's fixed date picks the month, today and the holiday theme (Purim), and the refresh is at the next *real* midnight. The theme is off when `dynamicHolidayTheme` is false.

**Emulator** (a dedicated read-only `Levana_Agent_Template_API_36_1`, emulator-5600, Pixel launcher; evidence is UI XML dumps):
- **Picker**: 'Levana · Month', 4 × 4, 'Hebrew month calendar with holidays'. Added to a real home screen.
- **4×4, English, light**: header 'חשון 5787' / 'Oct 12, 2026 – Nov 10, 2026'. Weekday row א׳…ש׳, Sunday first. 1 Cheshvan is in the Monday column (Oct 12). Each cell shows the Hebrew day plus the Gregorian day (א׳/12 … ל׳/Nov 10).
- **Day-cell tap** (ט״ז, Oct 27): opens the calendar. The selection was checked through Add event, which pre-filled 16 Cheshvan 5787. uiautomator doesn't expose Compose's `selected`.
- **Header tap**: opens the calendar, and Add event pre-fills 18 Cheshvan (today).
- **Hebrew app language** (`cmd locale set-app-locales … he`): the widget redrew as 'חשון תשפ״ז', '12 באוק׳ 2026 – 10 בנוב׳ 2026', Hebrew cell descriptions, and the row mirrored (Shabbat leftmost, Sunday rightmost). Back to English, it is unmirrored again.
- **Dark mode** (`cmd uimode night yes`): rerendered with no errors.
- **Forced holiday theme**: Purim was set in Developer settings while the widget was live, with no errors.
- **Date change** (developer override 22 Nisan 5786 = Apr 9 2026): the widget moved to 'ניסן 5786', 'Mar 19, 2026 – Apr 17, 2026'.
  - With the device in Asia/Jerusalem (Israel by auto): 16 Nisan is Chol HaMoed Pesach and 22 Nisan has no holiday.
  - The cell description reads 'Today, כ״ב ניסן…'.
  - Clearing the override → 'Today, י״ח חשון…, October 29'.
- **Time-zone change**:
  - To America/New_York (override on): Diaspora markers. 16 Nisan and 22 Nisan both 'Pesach'.
  - Later (override off) to Pacific/Kiritimati: today moved to 19 Cheshvan, Friday Oct 30.
- **Refresh alarm**: `dumpsys alarm` shows a non-wakeup RTC widget.REFRESH at 2026-10-31 00:00 (+14), the device's next midnight, with a 10-minute window.
  - With the Today widget also placed, there are two widget.REFRESH alarms with distinct PendingIntent records, one per receiver. The month alarm no longer replaces Today's.
  - The Today widget still renders after the shared-code refactor.
- **Resize**: minimum 3×2 cells (~277×210 dp, compact layout with no subtitle or Gregorian numbers). All 30 days lie within the widget bounds. The maximum reached was 4×4, because this home screen is 4 columns wide and icons occupy the rows below.
- Crash buffer empty; 0 FATAL EXCEPTION.

**Evidence**: /tmp/task9-2-month-widget/month-4x4-kiritimati-en.xml (final 4×4 dump), emulator.log, ui.sh (the dump helper). The other states are quoted above from live dumps.

**Limitations / notes for review**
- Colors were not inspected visually: the android-debug skill forbids screenshots and XML has no colors. This covers the holiday marker bars, the today primaryContainer background, light/dark, and the forced Purim theme.
  - Parity is structural: the widget uses `levanaColorScheme` + `activeHolidayTheme` (as in 9.1), and markers use the same `holidayCategoryColor` function as the calendar screen.
  - **AC #1 is left unchecked until a human eyeball/screenshot check.**
- Weekday headers are Hebrew in both languages (narrow 'א׳'). This matches the calendar screen's Hebrew grid, which always uses Hebrew day names.
- Header title uses Hebrew month letters in English too ('חשון 5787'), which is the app's `CalendarHeaderFormatter.hebrewMonthTitle`.
- Behavior change outside the widget: `EXTRA_DATE_EPOCH_DAY` deep links now select that day on the calendar (`CalendarScreen(initialDate)`, applied once per launch). This covers notifications too; before, they opened on today. The Today widget and quick tile pass today, so they are unchanged.
- Observation, not this task: with the emulator's GPS in Ukiah and the zone at Kiritimati, the Today widget showed 'Next: Sunrise 10:38 AM' with no weekday. That looks odd for a mismatched position/zone and may be worth a look under TASK-5 rules. The Today widget code is untouched apart from the shared helpers.

## Visual color check (screenshots allowed by the owner; tested commit fc119ee)

Screenshots of the widget on the Pixel home screen and of the app's calendar (Hebrew mode) in the same state are in /tmp/task9-2-month-widget/shots/:
- **English, light, no theme (Cheshvan, real today)**: 01-en-light-cheshvan-today.png (widget) and 02-en-light-cheshvan-app.png (app). 01-en-light-home.png is the widget's first-load spinner.
- **English, light, dynamic Sukkot theme** (override 18 Tishrei 5787): 03-en-light-tishrei-widget.png (before the fixes), 04-en-light-tishrei-app.png (app), 05-en-light-tishrei-widget-fixed.png (after).
- **English, dark, Sukkot**: 06-en-dark-tishrei-widget.png, 07-en-dark-tishrei-app.png.
- **English, dark, forced Purim**: 08-en-dark-purim-widget.png, 09-en-dark-purim-app.png.
- **Hebrew, light, forced Purim**: 10-he-light-purim-widget.png (bidi bug), 11-he-light-purim-app.png, 12-he-light-purim-widget-bidi-fixed.png.
- **Hebrew, dark, forced Purim**: 13-he-dark-purim-widget.png, 14-he-dark-purim-app.png.

**Result**: the widget's colors match the app in every state.
- Header title in primary.
- Fri/Shabbat weekday labels in primary.
- Today on primaryContainer, the same hue as the app's today/selected cell.
- Holiday marker bars carry the app's category colors:
  - Torah/rabbinic/minor holidays are the theme's primary family (green under Sukkot, pink under Purim, dark slate with no theme).
  - Tzom Gedaliah is error red in light and salmon in dark.
- The marked days are the same set as the app's holiday chips: Rosh Hashanah, the fast, Erev YK, YK, Erev Sukkot through Shemini Atzeret. In Israel, 23 Tishrei is unmarked.
- Light and dark surfaces follow the system, and the forced Purim theme overrides the dynamic Sukkot theme in both.

**Problems found and fixed** (commit fc119ee):
1. The app shades the Shabbat column (surfaceContainerLow) and the widget didn't. Shabbat cells now get the same color, as a day/night ColorProvider from the same schemes.
2. Today's number was onPrimaryContainer; the app uses primary. It is now primary.
3. Bidi: in Hebrew on an English device, the Gregorian range line rendered scrambled ('12 2026 'באוק 11 – 2026 'בספט'), because the launcher's TextView runs LTR. Hebrew text is now wrapped in an RTL isolate (U+2067…U+2069) and reads '12 בספט׳ 2026 – 11 באוק׳ 2026', as in the app.

Remaining, intentionally different:
- The widget uses short marker bars instead of the app's labeled chips (no room in widget cells).
- There is no today border: Glance has no borders; the container fill matches.
- Candle-lighting and parasha chips are not shown.

Owner decision 2026-10-29: Hebrew weekday letters and the Hebrew-letter month title stay as they are in English mode.

**Build/test/lint after the fixes**: BUILD SUCCESSFUL, 736 tests, 0 failures/errors, and no lint issues in the widget code. AC #1 is now checked.
<!-- SECTION:NOTES:END -->
