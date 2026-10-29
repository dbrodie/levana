---
id: TASK-5
title: Device context owns dates and notifications; selected city is display-only
status: Done
assignee:
  - '@claude'
created_date: '2026-10-21T11:19:29Z'
updated_date: '2026-10-29T20:19:00Z'
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
- [x] #1 One injectable device-context abstraction (clock incl. dev override, zone, position) is the only source of now/today/zone/position for dates, notifications and Israel/Diaspora; grep finds no direct LocalDate.now()/YearMonth.now()/HebrewYearMonth.now()/ZoneId.systemDefault()/inline override reads outside it
- [x] #2 The selected city is read only by the Zmanim screen and day-panel times, chosen only from the sidebar; it changes no notification timing or text, holiday date or Israel/Diaspora result
- [x] #3 All 13 rows of the case table behave as specified, with unit tests using a fixed clock, device zone different from the selected city (New York vs Jerusalem), midnight/sunset boundaries, a DST day and the dev override on/off
- [x] #4 PROJECT_SPEC.md states the device-vs-display-city rule and the developer-override semantics; ARCHITECTURE_SPEC.md names the abstraction and where each concept may be used
- [x] #5 Build, unit tests and lint pass; emulator checks cover a device time-zone change, crossing midnight with the app backgrounded, dev override plus reboot, and selecting a different-zone city (only Zmanim/day-panel times change)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Done in order on branch task-5-device-context, one commit (or more) per subtask: 5.1 device context + notifications + specs; 5.2 refresh today; 5.3 Israel/Diaspora three-way; 5.4 sidebar-only city selection + zone label. See each subtask's plan. Interpretation recorded for review: the developer override becomes a whole-day shift of the device clock (stored as a day count), so the as-if clock keeps running (time of day and midnight roll-over are real, only the date is shifted); AlarmManager triggers are mapped back to real time by the same shift.

Owner follow-ups (2026-10-29, after coordinator review):
F1 Israel auto-detect / fallback: resolveIsInIsrael(mode, deviceZone) - AUTO is Israel only when the device's time zone has Asia/Jerusalem's rules (covers Asia/Tel_Aviv, Israel aliases); the device zone wins over a GPS position (a GPS fix carries the device zone anyway, case 10). CityRepository.defaultForTimeZone(zone, now): same zone or alias (same rules) first, else the bundled city with the closest current UTC offset; ties -> the city whose offset is at or behind the device's (west), then by name. Tests: Dublin/Perth/Dubai without GPS = Diaspora with nearest-offset cities; Asia/Jerusalem and aliases = Israel; Europe/Kiev alias.
F2 Widget: next-zman and Shabbat/havdalah rows and the place name use the device position (device.position); widgetTimesLocation() seam removed; WidgetSettings keys on the device position; widget tests and TASK-9/9.1 wording updated.
F3 Developer override is a fixed as-if date: stored as dev_date_override (UserPreferences.devDateOverride, read only by DeviceContext); now = that date at the real time of day; DeviceNow.devDayShift is the current offset (override - real date) used by toSystemInstant. The date never advances; todayUpdates waits for the real midnight (no spin, no roll); DateRollover stays on the real midnight and, with the override on, also re-plans the alarms so each real day gets the as-if day's reminders. With the override on the plan covers only the as-if day (no lookahead), since later as-if days never come. The day-shift key/migration from 5.1 is removed (never released). Specs and tests updated.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Verification (task agent):
Unit/build: set -a && . ./.env && set +a && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug on 7074935 (rebased on origin/master 8c05211): BUILD SUCCESSFUL, 722 tests, 0 failures; lint 0 errors, no new warnings in changed files.

Emulator (own AVD instance: Levana_Agent_Template_API_36_1 -read-only, emulator-5584; location permission denied so the device position is the zone-fallback city; reminders: candle lighting morning 08:00). Evidence: /tmp/levana-task5-evidence/*.png (screenshots), helper scripts /tmp/levana-task5-tools/. Alarm/icon state read from dumpsys alarm / dumpsys package.
Builds 17666c8 (+ the fallback-position fix, later a1bb194):
- Launch in Jerusalem 29 Oct 21:27: today highlight 29 Oct, icon MainActivityDay18, candle alarm 30 Oct 08:00 IST (01).
- Settings: Location row without current-city text; Israel / Diaspora 'Auto-detect (Israel)' (02).
- Added New York in Manage cities: active city stayed Jerusalem. Selected New York in the drawer: candle alarm unchanged (08:00 Jerusalem), date unchanged; day panel shows New York times with 'New York time' (03); Zmanim screen same label + NY zmanim (04).
- Zone change to Asia/Tokyo with the app in the background: icon -> Day19, candle alarm -> 08:00 JST (23:00 UTC); on resume today highlight moved to 30 Oct, selection stayed 29 (05, 06). Found the drawer current-location entry stale (Jerusalem) -> fixed (a1bb194); after the fix a live zone change Tokyo -> New York updated the open drawer, icon Day18, candle 08:00 EDT.
- Israel/Diaspora in New York: 'Auto-detect (Diaspora)'; dialog Auto-detect/Israel/Diaspora, rows checkable with checked state (same structure as the App language dialog); Israel persisted, back to Auto (07). Hebrew UI: 'זיהוי אוטומטי (חוץ לארץ)' (08); day panel label 'שעון New York' with the device back in Jerusalem (09); no label while device and selected city were both New York.
Build 7074935 (final):
- Crossing midnight in the background (clock set to 23:59 IST, app backgrounded): icon did NOT change -> Android skips manifest DATE_CHANGED for stopped apps (pre-existing). Resume updated icon Day19 and highlight 30 Oct (10). Fix DateRollover (7074935): with the app process killed, icon turned to Day20 at 00:00:21 on 31 Oct; next roll-over set for 1 Nov.
- Dev override 25 Cheshvan (5 Nov, +5 days): picker opened on the device date; icon Day25 at once; candle alarm at real 1 Nov 08:00 = as-if Fri 6 Nov 08:00. Reboot: boot worker re-planned the same alarm, roll-over alarm restored, icon Day25, calendar opens on 5 Nov (11). Cleared: icon Day20, candle alarm real Fri 6 Nov 08:00.
- Quick tile: '20 Cheshvan, 5787' (12).
- Backgrounded zone change to America/Los_Angeles: icon Day19 (30 Oct there), roll-over at LA midnight, candle 6 Nov 08:00 local.

Limitations: TalkBack was not run with speech; semantics checked from the accessibility tree. Widget not placed on a home screen (unit tests only). City names in the zone label come from the bundled English city list (pre-existing). The zone label is refreshed when the day/zmanim reload, not instantly on a zone change.

Open questions for the coordinator/owner:
1. Widget next-zman/Shabbat rows: selected city or device position? Left as the selected city behind UserPreferences.widgetTimesLocation() (widget/TodayWidgetSnapshot.kt).
2. Device zones without a bundled city fall back to Jerusalem (pre-existing CityRepository.defaultForTimeZone), so Auto-detect then reads Israel. Not a regression (the old auto value used the same fallback when no city was saved) but worth a follow-up: e.g. a neutral fallback or Auto-detect by the device zone.

Owner follow-ups implemented (82706a4):
F1 resolveIsInIsrael(mode, deviceZone): AUTO = device zone has Asia/Jerusalem's rules (Asia/Tel_Aviv and Israel aliases included). Choice: the device zone wins over a GPS position (a fix carries the device zone anyway, case 10); a fallback city can never make a device Israeli. CityRepository.defaultForTimeZone(zone, now) -> fallbackCity: a bundled city in the zone, else one in an alias (same rules, e.g. Europe/Kiev -> Kyiv), else the closest current UTC offset; ties: outside Israel first, then at/west of the device offset, then by name. Tests (data/IsraelModeTest): Dublin -> London, Perth -> Hong Kong, Dubai -> Moscow, Cairo not Jerusalem, Kiev alias, all Diaspora; Asia/Jerusalem, Asia/Tel_Aviv, Israel = Israel; a Jerusalem position in a New York zone stays Diaspora. Israel tests now pin the JVM zone.
F2 Widget: TodayWidgetSnapshots.build uses device.position for next zman, Shabbat/havdalah and the place name; widgetTimesLocation() removed; WidgetSettings keys on the device position. Tests updated (selected city changes neither snapshot nor settings). Decision noted on TASK-9 and TASK-9.3.
F3 Override = fixed date: UserPreferences.devDateOverride (key dev_date_override, the original storage; the unreleased day-shift key/migration removed). DeviceContext.now: that date at the real time of day; DeviceNow.devOverride + devDayShift (current distance, used by toSystemInstant: same time of day on the real day). Plans with the override cover only that date; DateChangeReceiver (DATE_CHANGED / DateRollover at the real midnight) re-plans alarms while it is on. todayUpdates waits for the real midnight and emits nothing there (distinct). Specs updated. Tests: DeviceContextTest (fixed date across a real midnight, mapping on a DST date, override equal to today), DeviceTodayUpdatesTest (no roll at midnight; timer on the real midnight with a DST override date), CalendarTodayRefreshTest, DeviceVsDisplayCityTest row 12 (window = the fixed date; next real day plans it again for that day).
Remaining devDateOverride reads outside DeviceContext are change-detection keys (AlarmSettings, DailyCategory, WidgetSettings) and the Settings display, not date calculations.
Limitation: with the override on, an Omer notice already delivered for that date is not repeated on later real days (delivery dedupe by date) - test tool only.

Verification on 82706a4: assembleDebug + testDebugUnitTest (723 tests, 0 failures) + lintDebug passed.
Emulator (fresh read-only session, emulator-5584, location denied; evidence /tmp/levana-task5-evidence/13-18):
- Asia/Dubai: current location 'Moscow' (13), Israel 'Auto-detect (Diaspora)' (14). Europe/Dublin live: Diaspora; Asia/Tel_Aviv live: 'Auto-detect (Israel)'.
- Widget placed on the Pixel launcher home screen (Levana · Today 4x2), device in Dublin (fallback London), New York selected in the sidebar: '18 Cheshvan, 5787 / Thursday, October 29, 2026 · London', candle lighting Fri 4:19 PM, havdalah Sat 5:26 PM (London's times) (16).
- Override 26 Cheshvan (Fri 6 Nov) with morning candle reminders: icon Day26; the past as-if 08:00 had no pending alarm and the real-Friday alarm was cancelled. Clock set to 23:59 (Dublin), app backgrounded: after the real midnight the icon stayed Day26, the candle alarm was re-planned for real Fri 30 Oct 08:00, the next roll-over set for 31 Oct; the widget still showed 26 Cheshvan / Friday, November 6 (17) and the app's today stayed 6 Nov (18).
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-29T20:19:00Z

---

Coordinator integration 2026-10-29: reviewed (incl. owner follow-ups: zone-only Israel auto-detect + nearest-offset fallback city, widget on device position, fixed-date dev override); merged onto master as 6afbf08. Combined :app:assembleDebug :app:testDebugUnitTest :app:lintDebug passed, 723 tests, 0 failures/errors. Emulator evidence in /tmp/levana-task5-evidence/ per agent notes.

---
<!-- COMMENTS:END -->

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
