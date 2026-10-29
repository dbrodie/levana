---
id: TASK-9.2
title: Month calendar home-screen widget
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:19:00Z'
updated_date: '2026-10-29T20:23:00Z'
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
- [ ] #1 Month widget (about 4x4, resizable) shows the current Hebrew month with Gregorian secondary numbers, holiday markers and today highlighted, using the shared color scheme
- [ ] #2 Refreshes on date, locale, time-zone, and relevant preference changes
- [ ] #3 Day cell tap opens the calendar with that day selected; header opens month calendar on today
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
