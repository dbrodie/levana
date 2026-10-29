---
id: TASK-9.1
title: Widget foundation and Today home-screen widget
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:19:00Z'
updated_date: '2026-10-29T18:47:00Z'
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
- [x] #1 Resizable Today widget (2x1 to 4x2): 2x1 shows Hebrew date (primary) + Gregorian date + location; larger sizes add next zman and candle lighting + havdalah rows
- [x] #2 LevanaTheme and the widget both obtain colors from one extracted color-scheme function, and MainActivity and the widget both use one extracted active-holiday-theme function (incl. dev forced theme)
- [x] #3 Next zman comes from the user's selected zmanim and rolls to tomorrow's first after the last one passes
- [x] #4 Candle lighting and havdalah of the current/upcoming Shabbat-Yom Tov sequence are both shown and stay through the civil day of havdalah, consecutive days handled like reminders
- [x] #5 Widget refreshes on date change, time/time-zone change, locale change, preference/location change, and at the next zman instant / end of Shabbat window
- [ ] #6 Unit tests cover snapshot provider cases; emulator verification in English/Hebrew, light/dark, a forced holiday theme, and min/max sizes
- [x] #7 Split taps: date area opens month calendar on today; zman/candle rows open the Zmanim screen for today
- [x] #8 Uncomputable zman, candle lighting or havdalah shows —; dates always render
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

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Verification (tested commit 50af2ed)

**Build/test/lint**: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` → BUILD SUCCESSFUL; 669 tests, 0 failures. No lint issues in the new widget code; the remaining lint warnings are pre-existing (dependency versions, plurals, unused resources).

**New unit tests**
- `widget/TodayWidgetSnapshotsTest`, 11 tests:
  - next-zman ordering and rollover to tomorrow, empty selection, and high latitude with no sunset;
  - Shabbat window on weekday/Shabbat/Sunday rollover, Rosh Hashanah 5787 joined to Shabbat (Fri→Sun), and midweek Pesach 5786 in Israel (ends Thu) vs the Diaspora (joins Shabbat, ends Sat);
  - missing havdalah at high latitude; refresh time.
- `widget/WidgetThemeTest`, 3 tests:
  - the holiday scheme comes from `HolidayTheme`;
  - `activeHolidayTheme` follows the date, the setting and the forced theme;
  - unrelated preference changes are ignored.

**Emulator** (dedicated read-only instance of `Levana_Agent_Template_API_36_1`, emulator-5580, Asia/Jerusalem, 2026-10-29 ~20:35; evidence is from UI XML dumps):
- The widget picker lists "Levana · Today", 4×2, with its description. Added to the home screen.
- 4×2 shows: `18 Cheshvan, 5787`, `Thursday, October 29, 2026 · Jerusalem`, `Next: Sunrise / Fri 5:53 AM` (after nightfall, so it correctly rolls to Friday), `Candle Lighting Fri 4:33 PM`, `Havdalah Sat 5:27 PM`.
- Resizing to 2×1 gives the compact layout `18 Cheshvan, 5787` / `Thu 29 Oct · Jerusalem`. Resizing back to 4×2 restores the full layout.
- Taps: the date area opens the calendar; the next-zman row opens the Zmanim screen (Alot 4:39, Sunrise 5:52, ...).
- App language changed to Hebrew (device English): the widget redrew itself in Hebrew (`י״ח חשון תשפ״ז`, `הבא: הנץ החמה`, 24h times). Mirroring was confirmed: candle lighting is on the right and havdalah on the left. Back in English it is not mirrored.
- Candle offset changed in Settings while the widget session was live: 10 → 19 min gives 4:41 PM → 4:32 PM with no manual refresh.
- Device zone changed to Pacific/Kiritimati: TimeChangeReceiver redrew the widget to `19 Cheshvan`, Friday Oct 30, and next zman `5:53 AM` (today, so no weekday).
- `dumpsys alarm` shows the non-wakeup RTC refresh alarm at the next refresh time (midnight) with a 10-minute window.
- Dark mode was switched on during the Hebrew run. No crashes (0 FATAL EXCEPTION in logcat).

**Bugs found and fixed during verification**
- (a) Glance keeps a live session and only recomposes on `update()`. The snapshot was computed outside the composition and went stale. It is now computed in the composition, keyed on preferences plus `WidgetUpdater.refreshes`.
- (b) RTL mirroring: RemoteViews are laid out in the device's direction, and `Resources.getSystem()` reports the per-app locale in-process. The device locale now comes from `LocaleManager.systemLocales`.
- (c) The next-zman lookup assumed list order was chronological (MGA Shema comes after GRA). Night zmanim before noon are now treated explicitly as the next civil day.

**Limitations / follow-ups**
- Colors (light/dark, forced holiday theme) were not visually inspected: the android-debug skill forbids screenshots and UI XML has no colors. Parity with the app is guaranteed structurally, because both call the same `levanaColorScheme` and `activeHolidayTheme`, and this is unit-tested. A human eyeball check of a holiday theme on the widget is recommended.
- The zman-transition refresh could not be observed live: the emulator image has no root, so the clock can't be moved. Scheduling was verified with `dumpsys`. Without an exact-alarm permission, a passed zman may stay on display for up to 10 minutes; the device must be awake for the non-wakeup alarm.
- Location names come from the stored location (e.g. "Jerusalem" in the Hebrew UI), the same as the app.
- Each tap opens MainActivity, which shows the app's existing location-permission prompt on every launch until it is granted. This is pre-existing behavior.
- Glance 1.1.1 was chosen for compatibility with the current Compose BOM 2024.12; 1.2.0 exists.

AC #6 is left unchecked: the visual color check (light/dark and forced holiday theme on the widget) still needs a human screenshot or eyeball review. Everything else in #6 was done.
<!-- SECTION:NOTES:END -->
