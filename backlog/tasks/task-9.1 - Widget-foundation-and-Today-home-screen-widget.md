---
id: TASK-9.1
title: Widget foundation and Today home-screen widget
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:19:00Z'
updated_date: '2026-10-29T18:24:00Z'
labels: []
dependencies: []
parent_task_id: TASK-9
priority: medium
type: feature
ordinal: 40000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
First subtask of TASK-9; see TASK-9 for product decisions and shared technical design. Build the shared widget foundation (Glance setup, shared color-scheme/holiday-theme extraction, WidgetSnapshotProvider, refresh plumbing) and the resizable Today widget.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Resizable Today widget (2x1 to 4x2): 2x1 shows Hebrew date (primary) + Gregorian date + location; larger sizes add next zman and candle lighting + havdalah rows
- [ ] #2 LevanaTheme and the widget both obtain colors from one extracted color-scheme function, and MainActivity and the widget both use one extracted active-holiday-theme function (incl. dev forced theme)
- [ ] #3 Next zman comes from the user's selected zmanim and rolls to tomorrow's first after the last one passes
- [ ] #4 Candle lighting and havdalah of the current/upcoming Shabbat-Yom Tov sequence are both shown and stay through the civil day of havdalah, consecutive days handled like reminders
- [ ] #5 Widget refreshes on date change, time/time-zone change, locale change, preference/location change, and at the next zman instant / end of Shabbat window
- [ ] #6 Unit tests cover snapshot provider cases; emulator verification in English/Hebrew, light/dark, a forced holiday theme, and min/max sizes
- [ ] #7 Split taps: date area opens month calendar on today; zman/candle rows open the Zmanim screen for today
- [ ] #8 Uncomputable zman, candle lighting or havdalah shows —; dates always render
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Implementation plan (TASK-9.1)

1. **Dependencies**: add `androidx.glance:glance-appwidget` and `glance-material3` (1.1.1; compatible with Kotlin 2.1 / Compose BOM 2024.12) to `libs.versions.toml` and `app/build.gradle.kts`.
2. **Shared theme extraction (no duplication)**
   - `ui/theme/Theme.kt`: move the color-scheme `when` out of `LevanaTheme` into `fun levanaColorScheme(darkTheme: Boolean, holidayTheme: HolidayTheme?, dynamicColorScheme: ColorScheme? = null): ColorScheme`. `LevanaTheme` calls it and keeps its behavior.
   - `ui/theme/HolidayTheme.kt`: add `fun activeHolidayTheme(prefs: UserPreferences, date: LocalDate): HolidayTheme?` (dev forced theme → `dynamicHolidayTheme` → `HolidayThemeResolver.resolve(date)`). `MainActivity` uses it instead of its inline logic.
   - The widget theme is `GlanceTheme(colors = ColorProviders(light = levanaColorScheme(false, h), dark = levanaColorScheme(true, h)))`.
3. **Snapshot model + provider** (`widget/` package, pure Kotlin, unit-tested)
   - `TodayWidgetSnapshot(today, hebrewDate, gregorianDate, locationName, nextZman: NextZman?, sabbath: SabbathTimes?, holidayTheme, nextRefreshAt: Instant?)`.
   - `WidgetSnapshotProvider(zmanimRepository, preferencesRepository, todayProvider, clock)`:
     - **Next zman**: the first selected zman (`getZmanim` filtered by `selectedZmanim`, same filter as `DayDetailViewModel`) whose instant is after now, from today's then tomorrow's list. Returns null (shown as "—") if none.
     - **Sabbath sequence**: scan from `today - 6` for the first evening `d` where `hasCandles(d)` and `getObservanceEnd(d).date >= today`. Candle lighting is `getCandleLightingInstant(d)` and havdalah is the `ObservanceEnd` date/time. The window holds through the havdalah civil day; Sunday rolls to the next sequence.
     - `nextRefreshAt`: min(next zman instant, start of the day after the havdalah date, next midnight).
4. **Glance widget** (`widget/TodayWidget.kt`, `TodayWidgetReceiver`)
   - `SizeMode.Responsive` with compact (2x1: Hebrew date + Gregorian · location) and full (4x2: + next zman row + candle/havdalah row) layouts. Hebrew is always primary. Layout follows app language/RTL. Time format follows `DateFormat.is24HourFormat`. Zman names use `ZmanTime.displayName(hebrewUi)`.
   - `res/xml/today_widget_info.xml` (resizable, `targetCellWidth/Height`, preview layout, description string); register the receiver in the manifest. Strings go in en + he.
5. **Taps**
   - Date area: `MainActivity` with `EXTRA_DATE_EPOCH_DAY` = today (existing deep link → calendar).
   - Rows: new `EXTRA_OPEN_ZMANIM` extra, handled in `LevanaApp` next to the existing deep link; it navigates to `ZmanimRoute(today)`.
6. **Refresh**: a `WidgetUpdater.updateAll(context)` helper.
   - Called from the existing `DateChangeReceiver`, `TimeChangeReceiver` and `BootReceiver`, plus `LOCALE_CHANGED`.
   - Called from a collector in `LevanaApplication` on distinct widget-relevant preference changes (location, Israel, candle offset, selected zmanim, holiday theme, dev date/theme).
   - After each update, schedule a non-wakeup `AlarmManager.set(RTC, nextRefreshAt)` that broadcasts to the widget receiver. Non-wakeup alarms fire when the device next wakes, so no exact-alarm permission is needed.
7. **Tests**: `WidgetSnapshotProviderTest` covering:
   - Next-zman rollover to tomorrow and the empty selection.
   - Sun/Fri/Sat window and the Sunday rollover.
   - A Yom Tov adjacent to Shabbat (consecutive), a 2-day Diaspora Yom Tov, and a midweek Yom Tov.
   - A polar location where times are null.
   - `levanaColorScheme` parity: the holiday primary equals the `HolidayTheme` primary.
8. **Verify**: run the build/test/lint command, then use the `android-debug` skill on the emulator. Check the widget picker, both sizes, English and Hebrew, light and dark, a forced holiday theme, the taps, and a date change. Record evidence paths in the notes.
<!-- SECTION:PLAN:END -->
