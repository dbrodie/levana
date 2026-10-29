---
id: TASK-5
title: Device context owns dates and notifications; selected city is display-only
status: To Do
assignee: []
created_date: '2026-10-21T11:19:29Z'
updated_date: '2026-10-29T18:47:00Z'
labels:
  - curated-import
  - source-specs
dependencies: []
references:
  - levana-wvk
priority: high
type: chore
ordinal: 5000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Product rule (agreed with the owner 2026-10-29): **the device decides everything; the selected city is a display lens only.**

- **Device context** = device clock (with the developer override), device time zone and device position (current/GPS location, or the time-zone-derived fallback city when location permission is off). It decides the current date everywhere, every date calculation, when every notification fires, every time shown in notification text, and Israel/Diaspora auto-detection.
- **Selected (display) city** = chosen only from the sidebar/drawer. It changes ONLY the zmanim shown on the Zmanim screen and the day panel's times. Those use the device date and are shown in the city's own local clock, with an optional zone label (e.g. "Jerusalem time") when its zone differs from the device zone. It never affects notification timing or text, holiday dates, Israel/Diaspora, or any date calculation.
- **Developer date override** = an "as if" device clock for testing only: it shifts the app's notion of `now` (not just the date) consistently everywhere, and is clearly marked test-only in code.

The code must make this explicit: one injectable device-context/clock abstraction that every date, notification and Israel/Diaspora decision reads, and one clearly named display-city input read only by the Zmanim screen and day-panel times. No inline `devDateOverride ?: LocalDate.now()` copies, no `LocalDate.now()`/`YearMonth.now()`/`HebrewYearMonth.now()`/`ZoneId.systemDefault()` reads outside the abstraction. Mixing the two concepts is a review-blocking defect.

Scoping review (2026-10-29), current state:
- `data/TodayProvider.kt` exists but only AddEditEvent/ContactBirthday ViewModels use it; ~10 call sites duplicate `prefs.devDateOverride ?: LocalDate.now()` inline (CalendarViewModel, ZmanimViewModel, MainActivity theme/icon, HebrewDateTileService, DailyNotificationWorker, CalendarScreen pager start).
- Calendar `state.today` only updates in loadMonth; it goes stale if the app stays alive past midnight. The icon is not refreshed on TIMEZONE_CHANGED/TIME_SET. `DateChangeReceiver`/`BootReceiver` ignore the dev override.
- Alarm planning currently uses the selected city: candle-lighting and Omer-nightfall triggers from its zmanim (`ReminderAlarmPlan.kt`), candle "morning" mode in the city's zone (fires 02:00 in New York for Jerusalem), and a dual device-date/location-date planning window (`NotificationAlarmRebuilder.kt:121-125`). Fast notices show the selected city's times.
- `isInIsrael` auto-derives from the selected city's zone (`PreferencesRepository.kt:127`). The Settings "In Israel" switch makes the choice manual permanently, with no way back to automatic.
- Adding a city silently makes it the active city (`PreferencesRepository.kt:205`). The Settings main Location row shows the active city as secondary text (`SettingsScreen.kt:347`).

Case table (example: device in New York, selected city Jerusalem):
| # | Case | New behavior |
|---|---|---|
| 1 | Candle lighting "X hours before" | Device position's candle lighting − X; text shows the device-position time |
| 2 | Candle lighting "morning" mode | Morning time in the device zone |
| 3 | Omer evening at nightfall | Device position's nightfall |
| 4 | Omer morning | Unchanged (device zone) |
| 5 | Alarm planning window | Device date only; remove the location-today concept |
| 6 | Israel/Diaspora | Three-way setting: Auto-detect (default, from device position/zone) / Israel / Diaspora. Existing manual choices migrate as-is; untouched users get Auto-detect |
| 7 | Fast notice the day before | Device date; times from the device position |
| 8 | Holiday/personal/birthday notices | Unchanged (device date) |
| 9 | Zmanim screen + day panel times | The only consumer of the selected city: device date, the city's zmanim in the city's own clock, plus a zone label when the zones differ |
| 10 | GPS location zone | Keep the device zone (consistent with the rule); not a bug |
| 11 | Times past midnight | Unchanged semantics, computed for the device position |
| 12 | Developer override | Full "as if" device clock, test-only |
| 13 | Theme, tile, icon, calendar highlight | Device date; refresh on resume, at midnight/date change and on time-zone/time change |

Out of scope: rolling the Hebrew date over at sunset; system-calendar instance zones; next-day display context for late zmanim (TASK-20); the stale-alarm lateness guards (TASK-33/34), unless they become trivial within this work.

Implementation is split into subtasks TASK-5.1 to TASK-5.4, done in order on one branch by one agent. PROJECT_SPEC.md (and ARCHITECTURE_SPEC.md where relevant) must state the rule.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 One injectable device-context abstraction (clock incl. dev override, zone, position) is the only source of now/today/zone/position for dates, notifications and Israel/Diaspora; grep finds no direct LocalDate.now()/YearMonth.now()/HebrewYearMonth.now()/ZoneId.systemDefault()/inline override reads outside it
- [ ] #2 The selected city is read only by the Zmanim screen and day-panel times, chosen only from the sidebar; it changes no notification timing or text, holiday date or Israel/Diaspora result
- [ ] #3 All 13 rows of the case table behave as specified, with unit tests using a fixed clock, device zone different from the selected city (New York vs Jerusalem), midnight/sunset boundaries, a DST day and the dev override on/off
- [ ] #4 PROJECT_SPEC.md states the device-vs-display-city rule and the developer-override semantics; ARCHITECTURE_SPEC.md names the abstraction and where each concept may be used
- [ ] #5 Build, unit tests and lint pass; emulator checks cover a device time-zone change, crossing midnight with the app backgrounded, dev override plus reboot, and selecting a different-zone city (only Zmanim/day-panel times change)
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-wvk",
  "spec_id": "specs/TASKS.md",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "external_ref": "specs-import:original-11",
  "metadata": {
    "import_key": "original-11",
    "review_basis": "documents-only",
    "import_source": "specs/TASKS.md — original backlog #11 (Partial)"
  },
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 2
}
```
