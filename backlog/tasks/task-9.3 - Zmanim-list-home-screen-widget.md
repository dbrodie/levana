---
id: TASK-9.3
title: Zmanim list home-screen widget
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:19:00Z'
updated_date: '2026-10-29T20:42:00Z'
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
<!-- SECTION:NOTES:END -->
