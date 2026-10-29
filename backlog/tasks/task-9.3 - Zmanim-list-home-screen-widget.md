---
id: TASK-9.3
title: Zmanim list home-screen widget
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:19:00Z'
updated_date: '2026-10-29T20:22:00Z'
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
<!-- SECTION:NOTES:END -->
