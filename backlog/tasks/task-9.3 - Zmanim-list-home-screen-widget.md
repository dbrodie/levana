---
id: TASK-9.3
title: Zmanim list home-screen widget
status: Done
assignee:
  - '@claude'
created_date: '2026-10-29T18:19:00Z'
updated_date: '2026-10-29T21:36:00Z'
labels: []
dependencies:
  - TASK-9.1
parent_task_id: TASK-9
priority: medium
type: feature
ordinal: 42000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Third subtask of TASK-9; see TASK-9 for product decisions and shared design. Uses the foundation from TASK-9.1. Scrollable list of today's selected zmanim (same selection as day details), past times dimmed, next zman highlighted. Times and header location follow the DEVICE location via DeviceContext (owner decision 2026-10-29, consistent with the Today widget), not the selected city. Per-widget city choice is a separate future task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Zmanim list widget shows today's selected zmanim with past dimmed and next highlighted, using the shared color scheme
- [ ] #2 Advances the highlighted zman as times pass and refreshes on date, time-zone, location and preference changes
- [ ] #3 Row tap opens the Zmanim screen for today; uncomputable zman shows —
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Implementation plan (TASK-9.3)

1. **Shared selected-zmanim logic** (`widget/WidgetZmanim.kt`, extracted from `TodayWidgetSnapshots.nextZman`, no duplication)
   - `selectedZmanimOn(prefs, location, date)`: the day's zmanim from `ZmanimRepository.getZmanim` filtered by `selectedZmanim` (the day-details filter, same order), each placed at its civil moment in the location's zone. A night zman before noon (halachic midnight after civil midnight) belongs to the next civil day. An uncomputable time stays in the list with a null moment.
   - `nextSelectedZman(...)`: the first of those after now, from today's list and then tomorrow's. `TodayWidgetSnapshots.nextZman` delegates to it.
   - `widgetRefreshAt(device, next)`: the next zman, or the device's next midnight, on the real clock. Today and Zmanim widgets share it.
2. **Snapshot** (`widget/ZmanimWidgetSnapshot.kt`, pure, Koin single)
   - `ZmanimWidgetSnapshots.build(prefs, device: DeviceNow)` gives today (the device's, override included), the Hebrew day, the device position's name (never `displayCity`), rows, holiday theme and refreshAt.
   - Each row is `(name, hebrewName, at?, state)` with state PAST / NEXT / UPCOMING. NEXT is the row equal to the shared next zman when it is in today's list. PAST means at <= now. A null time is shown as — and is not dimmed.
3. **Widget** (`widget/ZmanimWidget.kt`): Glance, the same session pattern as TodayWidget (snapshot keyed on prefs + `WidgetUpdater.refreshes`, schedules its refresh, `GlanceTheme` from `levanaColorScheme` + the snapshot's holiday theme).
   - Header: the Hebrew date · location. Tapping it opens the calendar on today (the existing deep link).
   - Body: a `LazyColumn` of rows (name | time). Past rows are dimmed (`onSurfaceVariant`), the next row is highlighted (`primaryContainer` background, bold). Every row opens the Zmanim screen for today (`EXTRA_OPEN_ZMANIM`).
   - An empty selection shows a short hint. Mirroring and time format follow TodayWidget; small helpers (`openCalendar`/`openZmanim`/`timeFormatter`/mirror) become internal and shared rather than copied.
4. **Refresh plumbing (small, additive)**
   - `WidgetUpdater.requestUpdate` broadcasts to every widget receiver.
   - `scheduleRefresh`/`cancelRefresh` take the receiver class, so each widget kind has its own alarm (no clobbering).
   - A shared base receiver handles `ACTION_REFRESH`/`LOCALE_CHANGED`; `ZmanimWidgetReceiver` uses it.
   - Manifest: add the receiver. Add `res/xml/zmanim_widget_info.xml` (resizable, about 2x2 min, 3x3 target). Add en + iw strings.
5. **Tests** (`ZmanimWidgetSnapshotsTest`): past/next/upcoming classification, a night zman after civil midnight (Chatzot Laila) is next/upcoming late in the evening, an empty selection, polar (uncomputable → null, not past), the override date (shifted today and refresh on the real clock), and the device position vs the selected city. TodayWidgetSnapshotsTest stays green.
6. **Verify**: run build/test/lint, then use android-debug on a dedicated emulator. Check the picker, place the widget, English/Hebrew, light/dark, a forced holiday theme, resize + scroll, row tap, highlight advance / refresh alarm in dumpsys, a time-zone change, and that a sidebar city change leaves the widget unchanged.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
TASK-5 owner decision (2026-10-29): widgets show the device position (not the selected city); the 'active location in header' here should read the device position, as the Today widget now does.

## Implementation (commits 56da488, e56d204)
- `widget/WidgetZmanim.kt` is shared and extracted from the 9.1 code:
  - `selectedZmanimOn`: the day-details filter in the same order, with a night zman before noon dated to the next civil day; uncomputable times stay listed with a null time.
  - `nextSelectedZman`: today, then tomorrow.
  - `widgetRefreshAt`: the next zman or the device midnight, on the real clock.
  - `TodayWidgetSnapshots` now delegates to these, with no behavior change; its 15 tests still pass.
- `ZmanimWidgetSnapshots` (pure, Koin) gives today, the Hebrew day, the device position name, rows (PAST/NEXT/UPCOMING), the holiday theme and refreshAt. NEXT is the same next zman the Today widget shows, highlighted when it falls in today's list. Null times are never dimmed.
- `ZmanimWidget` (Glance):
  - Header: the Hebrew day/month · location. Tapping it opens the calendar on today.
  - Rows are a `LazyColumn`. Past rows use onSurfaceVariant; the next row uses primaryContainer, bold. Each row opens the Zmanim screen for today (`EXTRA_OPEN_ZMANIM`). Mirroring and the time format are shared with TodayWidget (helpers made internal, plus a new `mirrored()`).
  - An empty selection shows "No zmanim selected".
  - Each row has a content description ending in "next"/"passed" (Hebrew "הבא"/"עבר"), because the dimming and highlight are color only.
- Refresh plumbing:
  - `LevanaWidgetReceiver` is a base class for Today and Zmanim.
  - `requestUpdate` broadcasts to every receiver.
  - `scheduleRefresh`/`cancelRefresh` take the receiver class, so each widget kind has its own alarm.
  - Manifest receiver, `zmanim_widget_info.xml` (min 2x2, target 3x3, resizable), en/iw strings.
  - **Merge note for 9.2:** `WidgetUpdater.scheduleRefresh` now needs a receiver argument and `receivers` lists the widget receivers. The month widget should subclass `LevanaWidgetReceiver` and add itself to that list.

## Verification (tested commit e56d204)
**Build/test/lint:** `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` → BUILD SUCCESSFUL, 731 tests, 0 failures/errors. No lint issues in widget code or the new strings; the remaining warnings are pre-existing.

**New unit tests** (`ZmanimWidgetSnapshotsTest`, 8 tests):
- past/next/upcoming in day-details order;
- the highlight equals the Today widget's next zman, refresh happens when it passes, and the highlight then moves on;
- Chatzot HaLaylah after civil midnight (Jerusalem, July, 23:30) is NEXT, dated the next day, with refresh at midnight first;
- all passed means no highlight and refresh at midnight;
- an empty selection gives no rows;
- polar (Tromsø, 21 Jun): Sunrise/Sunset null and not dimmed, Chatzot past;
- developer override: the fixed date's list, with refresh on the real clock;
- selecting New York in the sidebar gives a snapshot identical to the device-position one.

**Emulator** (dedicated read-only instance of `Levana_Agent_Template_API_36_1`, emulator-5592, Asia/Jerusalem, 2026-10-29 22:25–23:35, GPS fix set to Jerusalem; evidence is UI XML dumps via uiautomator, as the android-debug skill forbids screenshots):
- The widget picker lists "Levana · Zmanim", 3 × 3, with its description. It was placed on the Pixel launcher home screen.
- Initial: `18 Cheshvan · Current Location`, Sunrise/Sunset/Nightfall. After a GPS fix to Jerusalem, the widget redrew itself with Jerusalem times (5:52 / 4:52 / 5:29 PM).
- Selection change (Settings → Halachic Times, +Alot, Chatzot, Candle Lighting, Chatzot HaLaylah): the widget redrew itself. Candle lighting is correctly absent on Thursday. Descriptions at 22:33: Alot…Nightfall `passed`, `Chatzot HaLaylah, 11:22 PM, next`.
- Taps: a row opened the Zmanim screen; the header opened the calendar. After scrolling, a lazy row still opened the Zmanim screen.
- Sidebar city switched to New York (sidebar shows ACTIVE CITY New York): the widget was unchanged (Current Location, Jerusalem times, same states). The Zmanim screen opened from the widget shows New York ("New York time"), which is the app's selected-city lens per TASK-5.
- Hebrew app language (`cmd locale set-app-locales … iw`): `י״ח חשון`, Hebrew names, 24h times, `…, עבר` / `חצות הלילה, 23:22, הבא`. Mirrored: time on the left, name on the right, date on the right of the header. English is not mirrored.
- Dark mode and forced Purim theme (Developer settings): redraws with no errors, and 0 FATAL EXCEPTION in logcat.
- Resize 3x3 → 2x2: the date stays whole and the location truncates. The 6th row moved off-screen and scrolling the list brought it into view.
- Time zone → Pacific/Kiritimati (`service call alarm 3`): the widget redrew itself immediately to `19 Cheshvan` (Friday). Candle Lighting 4:33 PM appeared and `Alot HaShachar, 4:40 AM, next`. The refresh alarm moved to 16:40 +14 (= 04:40 Jerusalem). Back to Jerusalem: 18 Cheshvan, alarm 23:22:49.
- Alarms (`dumpsys alarm`): `widget.REFRESH` RTC at 23:22:49 with a 10 min window, separate from the app's DATE_ROLLOVER. With the Today widget also placed there are two REFRESH alarms (one per receiver), both at 23:22:49, and the two widgets agree on the next zman (`Next: Chatzot HaLaylah`).

**Limitations**
- Colors (light/dark, Purim theme, highlight background, dimming) were not visually inspected: no screenshots per the android-debug skill, and UI XML has no colors. States are confirmed via the row descriptions. Colors come structurally from `levanaColorScheme`/`activeHolidayTheme`, as in 9.1. A human eyeball check is recommended.
- Times are in the position's zone. After a zone change the GPS fix keeps its original zone (Kiritimati showed Jerusalem-zone times for Friday). This is pre-existing DeviceContext behavior and is the same in the Today widget.
- The location name is "Current Location" (stored name, no reverse-geocoded name on this emulator), the same as the app.

## Visual check with screenshots (owner allowed screenshots; tested commit 69e5f5f)
Screenshots are in /tmp/task93-widget-shots/, outside the worktree: `NN-*.png` is the full screen and `NN-*-crop.png` is the widget only. The widget is 3x3 at 22:52–23:01 IST, with Chatzot HaLaylah 23:22 as next.
- 01-en-light-purim, 02-en-dark-purim: forced Purim. Date in the Purim primary; next row on a Purim primaryContainer pill (pink in light, plum in dark), bold.
- 03-en-dark-default, 04-en-light-default: the default scheme (blue-grey primary and pill).
- 05-he-light-default, 06-he-dark-default, 07-he-dark-purim, 08-he-light-purim: Hebrew, mirrored. Times on the left, names on the right, date at the right of the header. 24h times.
- **Fix found by the visual check (69e5f5f):** the first screenshot showed past rows (onSurfaceVariant) barely distinguishable from upcoming ones, especially in dark mode. Past rows now use the scheme's onSurface at 50% alpha, built from the same `levanaColorScheme` light/dark schemes. They are clearly dimmed in all 8 shots. Build/test/lint passed again: 731 tests, 0 failures, no widget lint issues.
- The earlier 2x2 screenshot (taken before the fix, since deleted) showed truncation working: "18 Cheshvan · Curr…", "Chatzot H… 11:22 PM".

**Emulator restart:** the emulator was stopped by the 30-minute background-command limit. It is a read-only instance, so all state was lost, and the setup was repeated: install, grants, Jerusalem fix, the same 6 zmanim, widget placed. The Today widget was not re-added.

**Discovery:** after reverse geocoding, the current-location name became "القدس" (Arabic), in the sidebar and the widget header alike. This is not widget code. Filed as TASK-44 (low; possibly the emulator's geocoder).

**Glance note:** each redraw resets the list's scroll to the top (Glance LazyColumn has no scroll-position API). At small sizes the next row can be below the fold after a refresh. It is acceptable at the default 3x3, where all six default+extra rows fit.

## Highlight advance observed live (pre-rebase build 69e5f5f, 2026-10-29)
- The 23:22:49 refresh alarm fired. At 23:23:11 the widget showed `חצות הלילה, 23:22, עבר` (all rows passed), and the next REFRESH alarm moved to 2026-10-30 00:00. Polling dumps are in the task output; screenshot 09-he-light-after-chatzot-halaylah.
- **Bug found and fixed (see the fix commit after the rebase):** screenshot 09 shows the row correctly dimmed and not bold, but still on the highlight pill. Glance/RemoteViews reuses lazy-list rows and doesn't clear a background a row no longer sets; a fresh render (10) was clean. Every row now sets its background (transparent unless next), with the same corner radius.
- Re-tested in a live session with a zone round-trip:
  - Kiritimati: Friday, `עלות השחר, 04:40, הבא` with a pill (11);
  - back to Jerusalem: no row is next and the pill is gone (12).

## Rebase onto origin/master fcf8da5 (after TASK-9.2)
- Took 9.2's `WidgetUpdater.kt` (`RefreshingWidgetReceiver`, one alarm per receiver) and added `ZmanimWidgetReceiver` to `receivers`. My `LevanaWidgetReceiver` was dropped; `ZmanimWidgetReceiver` (in ZmanimWidget.kt, like MonthWidgetReceiver) extends `RefreshingWidgetReceiver`.
- ZmanimWidget uses 9.2's `WidgetCommon.kt` helpers (`widgetLocale`/`isHebrewUi`/`widgetMirrored`/`openCalendar`). The helpers shared by Today and Zmanim (`openZmanim`, `NO_TIME`, `textAlign`, `timeFormatter`) moved from TodayWidget's private functions into `WidgetCommon.kt`, so neither keeps a duplicate.
- Manifest, Koin, strings: both sides kept. TASK-44 does not collide (master has up to TASK-43).
- The earlier "merge note for 9.2" is obsolete.

## Verification after rebase (tested commit: the highlight fix on top of fcf8da5)
- Build/test/lint: BUILD SUCCESSFUL, 744 tests, 0 failures/errors, no lint issues in widget code.
- Emulator (emulator-5592, rebased build installed): Zmanim (page 1), Today and Month (page 2) all placed. `dumpsys appwidget` shows ids 2/3/4 bound to Zmanim/Today/MonthWidgetReceiver.
- `dumpsys alarm` shows 3 separate `widget.REFRESH` RTC alarms (10 min window), all at 2026-10-30 00:00 since no zman is left today. `dumpsys activity intents` maps them to `cmp=.widget.MonthWidgetReceiver`, `.TodayWidgetReceiver` and `.ZmanimWidgetReceiver`, one alarm per widget kind.
- Screenshot 13-he-today-and-month-page: Today and Month in Hebrew next to each other. The Arabic location name (TASK-44) runs into the year in Today's date line; noted on TASK-44.
- Evidence: /tmp/task93-widget-shots/01–13.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-29T21:36:00Z

---

Coordinator integration 2026-10-29: reviewed diff (no direct clock/zone/override/displayCity reads) and screenshots; merged onto master as 5c81af4. Combined :app:assembleDebug :app:testDebugUnitTest :app:lintDebug passed, 744 tests, 0 failures/errors. Agent verified all three widgets coexist with separate refresh alarms.

---
<!-- COMMENTS:END -->
