---
id: TASK-9
title: Add a home-screen calendar widget
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-21T11:19:31Z'
updated_date: '2026-10-29T20:08:00Z'
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-crt
priority: medium
type: feature
ordinal: 9000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add home-screen widgets that show Levana's calendar information outside the app. The Quick Settings tile and the dynamic launcher icon are separate surfaces and do not satisfy this task.

## Product decisions (product session, 2026-10-29)

**Widget set** (one subtask per widget):
1. **Today**: resizable from 2x1 to 4x2. Covers the original acceptance criterion.
2. **Month calendar**: about 4x4, a month grid.
3. **Zmanim list**: resizable list of today's zmanim.

**Style**
- Material 3 widget surface: launcher-matched rounded corners, light/dark following the system.
- Use the same colors as the app, **including the dynamic holiday color scheme**. The widgets must not have their own palette or holiday logic. They use the code the app uses, so changing the holiday scheme or its resolution rules changes the widgets too. This covers the `dynamicHolidayTheme` setting and the developer forced-theme override.
- Widgets follow the app language and RTL layout.

**Content rules**
- **Primary date is always Hebrew** (large line). The Gregorian date is secondary. The calendar-mode setting is ignored here.
- **Next zman**: the next upcoming time among the zmanim the user selected for day details (`selectedZmanim`). No widget-specific selection.
- **Shabbat/Yom Tov row**: always shows **both candle lighting and havdalah** of the current or upcoming Shabbat/Yom Tov sequence, with day names (e.g. `Fri 16:21 · Sat 17:18`). A sequence stays on display through the end of the civil day of its havdalah (Saturday inclusive for a regular Shabbat), then rolls to the next sequence. Consecutive Shabbat/Yom Tov days use the same consecutive-transition logic as reminders (candle lighting of the first day, havdalah of the last).
- **Location**: the active location name. The app always resolves a location (the time-zone default city or Jerusalem), and the widget treats it like the app does: no special prompt.

**Tap actions (split tap areas)**
- Date area: opens the app on the month calendar with today selected (existing `EXTRA_DATE_EPOCH_DAY` deep link → `CalendarRoute`; day details are inline there).
- Zman, candle/havdalah and zmanim-list rows: open the full **Zmanim screen** (`ZmanimRoute`) for today, through a new launch extra handled next to the existing deep link.
- Month widget day cell: opens the calendar with that day selected.

**Unavailable data**
- Dates always render (they need no location).
- No "Set location" prompt. The app always has a location (see Location above), so the widget matches the app (decision 2026-10-29).
- One zman can't be computed (e.g. polar day): show "—" for it. A next zman that doesn't exist today rolls to tomorrow's first selected zman. If no selected zman exists in the next two days, show "—".
- Candle lighting or havdalah not computable (no nightfall at high latitude): show "—" for that time.

## Technical design (shared by all subtasks)
- **Framework:** Jetpack Glance (`androidx.glance:glance-appwidget` + `glance-material3`). Compose-based, so it stays consistent with the Compose/M3 commitment.
- **Shared colors (no duplication):**
  - Move the color-scheme selection in `ui/theme/Theme.kt` `LevanaTheme` into a plain function, e.g. `levanaColorScheme(dark: Boolean, holidayTheme: HolidayTheme?)`. `LevanaTheme` calls it.
  - Move the active-holiday resolution in `MainActivity.kt` (~L137-150: dev force override → `dynamicHolidayTheme` → `HolidayThemeResolver.resolve(date)`) into a shared function in `ui/theme`, e.g. `activeHolidayTheme(prefs, date)`. `MainActivity` and the widgets both call it.
  - Glance gets `ColorProviders(light = levanaColorScheme(false, h), dark = levanaColorScheme(true, h))`.
- **Data:** a widget-agnostic `WidgetSnapshotProvider` (pure, unit-testable) built on `ZmanimRepository`, `PreferencesRepository` and `TodayProvider`. It produces an immutable snapshot (Hebrew/Gregorian date, location name or null, next zman or unavailable, Shabbat sequence candle/havdalah instants, holiday theme). Formatting is shared with existing app formatters where they exist.
- **Refresh triggers:**
  - Date change: reuse the existing `DateChangeReceiver` and `TimeChangeReceiver`, adding the widget update call.
  - Time zone, locale or location change, and preference change (selected zmanim, holiday theme, location).
  - An exact-ish alarm (or a WorkManager one-off) at the next zman instant, so "next zman" advances. Must respect Android scheduling limits. Also run at the end of a Shabbat sequence's display window.
- **Testing:** unit tests for the snapshot provider (next-zman rollover, sequence window Sun–Sat, consecutive Yom Tov, no location, polar). Glance UI is checked with the `android-debug` skill on an emulator (Hebrew and English, light and dark, holiday forced theme, each size).

## Subtasks
- TASK-9.1: Shared foundation + Today widget (do first)
- TASK-9.2: Month calendar widget (depends on 9.1)
- TASK-9.3: Zmanim list widget (depends on 9.1)

Source: specs/TASKS.md original backlog #16. Related: specs/rfes/quick-tile-hebrew-date.md, specs/rfes/dynamic-launcher-icon.md.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Today, Month calendar and Zmanim list widgets are available in the launcher widget picker (delivered through subtasks 9.1-9.3)
- [ ] #2 Widget colors come from the same code as the app's theme, including the holiday scheme; no duplicated palette or holiday-resolution logic
- [ ] #3 Widgets refresh on date, time zone, location, preference change and when the displayed next zman or Shabbat window passes
- [ ] #4 Defined behavior for uncomputable times: dates always shown; — for missing zman/candle/havdalah; no location prompt (app always has a location)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
TASK-5 owner decision (2026-10-29): the widget's 'Location' and its next-zman and candle lighting/havdalah rows use the device position (current location or the time zone's fallback city), not the selected city; the selected city remains a Zmanim-screen/day-panel lens only. Implemented on branch task-5-device-context (TodayWidgetSnapshots.build reads DeviceNow.position).
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-crt",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:original-16",
  "metadata": {
    "import_key": "original-16",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #16 (Open)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 2
}
```
