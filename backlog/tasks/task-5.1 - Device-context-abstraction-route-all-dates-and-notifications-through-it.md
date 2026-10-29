---
id: TASK-5.1
title: Device context abstraction; route all dates and notifications through it
status: Done
assignee:
  - '@claude'
created_date: '2026-10-29T18:47:00Z'
updated_date: '2026-10-29T20:19:00Z'
labels: []
dependencies: []
parent_task_id: TASK-5
priority: high
type: chore
ordinal: 40000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Per TASK-5's rule and case table rows 1-5, 7, 8, 11, 12. Introduce the single injectable device context (clock + dev override as an 'as if' clock, device zone, device position = current/GPS or zone-fallback location), replacing TodayProvider and every inline override/now read. Make the alarm planner, daily worker, fast notices, tile, icon, theme and calendar use it. Remove the location-today planning window and use the device position (not the selected city) for candle-lighting/Omer triggers and notification text; candle 'morning' mode uses the device zone. Update existing time-zone/alarm tests that encode the old selected-city behavior, explaining each change in the notes. Update PROJECT_SPEC.md and ARCHITECTURE_SPEC.md with the rule.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 All date/now/zone reads outside the abstraction are gone (grep evidence recorded)
- [x] #2 Notification triggers and text use the device position and zone; the selected city has no effect (tests with New York device vs Jerusalem city)
- [x] #3 Dev override behaves as an as-if device clock everywhere, including receivers and alarm triggers
- [x] #4 PROJECT_SPEC.md and ARCHITECTURE_SPEC.md updated
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Abstraction (new package ninja.doskey.app.levana.device, file DeviceContext.kt):
- data class DeviceNow(instant: Instant, zone: ZoneId, position: Location, devDayShift: Long = 0): the device's own moment. Derived: dateTime, today, clock (fixed), toSystemInstant(deviceInstant) (maps an as-if trigger back to the real instant AlarmManager needs; identity when the shift is 0).
- class DeviceContext(preferences: Flow<UserPreferences>, systemClock: () -> Clock = DeviceContext::systemClock): suspend now(): DeviceNow and now(prefs) for callers that already hold preferences. Companion systemClock()/systemZone() are the ONLY place that reads the system clock/zone (Clock.systemDefaultZone()). Device position = UserPreferences.currentLocation (GPS fix, or the zone-fallback city). Replaces TodayProvider (deleted).
- Dev override (test-only): stored as a day shift (key dev_clock_shift_days); UserPreferences.devDateOverride -> devDayShift: Long (0 = off). DeviceContext.devDayShiftFor(date) converts the Settings picker date. DataMigration converts an existing dev_date_override date into a shift relative to the migration day.
- Selected/display city: UserPreferences.activeLocation renamed to displayCity, KDoc'd as readable only by the Zmanim screen, day-panel times and the city pickers.

Call sites moved to DeviceContext: CalendarViewModel (today, LoadToday/GoToToday, selection), CalendarState/ZmanimState/AddEditEventState/ContactBirthdayState defaults (no now() defaults; set by the VM), CalendarScreen pager base month (from state.today), ZmanimViewModel default date, MainActivity theme + icon, HolidayThemeResolver (no default param), DynamicIconManager.update(context, today), HebrewDateTileService, DateChangeReceiver/BootReceiver (goAsync + DeviceContext, so the override is honoured), DailyNotificationWorker, AddEditEvent/ContactBirthday VMs, PreferencesRepository fallback city zone, LocationService fix zone, SystemCalendarRepository zone, NotificationAlarmScheduler (schedule by Instant; legacy test-facing helpers take an explicit zone/date).

Notifications (case rows 1-5, 7, 8, 11, 12):
- planReminderAlarms(device: DeviceNow, prefs, zmanim, calendar, lookahead): planning window = device today .. +7 only (locationToday removed); candle lighting / Omer nightfall from device.position; candle 'morning' and Omer morning in device.zone; candleLightingTime text in device.zone. Post-midnight handling (previousEvening) unchanged.
- NotificationAlarmRebuilder takes a DeviceContext (secondary ctor with a Clock lambda kept for tests); maps plan triggers with toSystemInstant before scheduling. Unused candleLightingTimes() removed.
- AlarmSettings.location = device position (selected city no longer triggers a rebuild); devDayShift replaces devDateOverride.
- DailyNotificationWorker: today from DeviceContext; fast notice times from device position; DailyCategory FASTS settings key on position.
- isInIsrael auto value derived from the device position's zone instead of the selected city (three-way setting comes in 5.3).

Tests: convert tests that used devDateOverride=X with the real clock to an injected fixed clock at X (same expectations); TodayProvider fakes -> DeviceContext with a fixed clock. Deliberate expectation changes (each noted in task notes): TimeZoneChangeReminderTest, VerifyTimeZoneCrossing/SingletonTest, ShiftedWindowReconciliationTest, ReminderAlarmPlanTest, NotificationAlarmRebuilderTest, PostMidnight*Test, OmerNightfallRollover*Test where they assert the selected city's zone/times or the dual device/location window. New DeviceContextTest + DeviceVsDisplayCityTest: NY device vs Jerusalem city (rows 1,2,3,4,5,7), midnight boundary, sunset/nightfall after midnight (row 11), DST day, override on/off incl. trigger mapping (row 12).

Specs: PROJECT_SPEC.md (Calendar/Locations/Reminders: device-vs-display-city rule, override semantics); ARCHITECTURE_SPEC.md (Dates and Notifications: DeviceContext/DeviceNow, displayCity and where each may be used).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implementation (5.1):
- New device/DeviceContext.kt: DeviceNow (instant, zone, position, devDayShift; today, dateTime, toSystemInstant) and DeviceContext (now(), now(prefs), today(), devDayShiftFor(date); companion realClock/realZone/realToday are the only real-clock reads). Koin single. TodayProvider deleted.
- Dev override is now UserPreferences.devDayShift (key dev_clock_shift_days); DevDateOverrideMigration converts an old dev_date_override date into a shift from the migration day. Settings picker stores devDayShiftFor(date).
- UserPreferences.activeLocation renamed displayCity (KDoc: display only). Readers: ZmanimViewModel, DayDetailViewModel (day panel times), drawer/Settings/Calendar names.
- planReminderAlarms(device: DeviceNow, ...) – window = device today..+7 (locationToday removed); candle/nightfall from device.position; morning modes and candle text in device.zone; ReminderAlarmPlan.onSystemClock(device) maps triggers through the override before scheduling. Rebuilder takes a DeviceContext (secondary ctor with a Clock lambda kept for tests); unused candleLightingTimes() removed. AlarmSettings keys on devicePosition (currentLocation), not the selected city.
- DailyNotificationWorker: device today; fast times from device.position; DailyCategory FASTS keyed on currentLocation. isInIsrael auto now from the device position's zone (three-way comes in 5.3).
- Moved to DeviceContext: CalendarViewModel (+ state.today nullable; selection starts on device today; screen waits for it; pager anchor from state.today), ZmanimViewModel/State, MainActivity theme + resume icon, HolidayThemeResolver (no default), DynamicIconManager.update(context, today), HebrewDateTileService, DateChangeReceiver + BootReceiver (goAsync, override honoured), AddEdit/ContactBirthday VMs (state placeholders use realToday), LocationService/PreferencesRepository/SystemCalendarRepository zone, NotificationAlarmScheduler (schedules by Instant; real clock via DeviceContext.realClock). HebrewYearMonth.now() removed.

Grep evidence (AC1), app/src/main, pattern LocalDate.now(|YearMonth.now(|HebrewYearMonth.now(|ZoneId.systemDefault(|TimeZone.getDefault(|Clock.systemDefaultZone(|System.currentTimeMillis|devDateOverride ?:|activeLocation: only device/DeviceContext.kt (devDayShiftFor uses the injected clock; realClock/realToday/realZone).

Changed test expectations (old selected-city behaviour -> device rule):
- AlarmSettingsTest: selecting a saved city no longer rebuilds alarms; moving the device position does (was: active city rebuilds, current location ignored while a city is active).
- DailyCategorySettingsTest: selecting a city no longer refreshes FASTS; a device-position change does.
- DailyNotificationWorkerTest: 'fasts follow the active location' -> addSavedLocation leaves the fast notice, updateCurrentLocation refreshes it; fixed device clock replaces the date override. New fastNoticeTimesAreTheDevicePositionsNotTheSelectedCitys (row 7).
- NotificationAlarmRebuilderTest: changingTheActiveLocationMovesTheAlarm -> selectingACityLeavesTheAlarmAndMovingTheDeviceMovesIt; fixed Jerusalem device clock replaces the date override.
- TimeZoneChangeReminderTest: morning candle notice now moves to 09:30 Moscow on a zone change (was: stayed 09:30 New York, the city's zone); Koin singleton test uses an overridden DeviceContext with a fixed instant reading the JVM zone instead of the date override.
- VerifyTimeZoneSingletonTest: same rule (candle morning follows the device zone); window starts on the New York device date; fixed instant instead of the date override.
- VerifyLookaheadPlanTest: 'candles in location zone' -> wall-clock times in the device zone, nightfall at the device position; LA/Jerusalem late-run limitation replaced: Friday's 09:30 device-time notice is scheduled on Thursday's late run.
- VerifyReschedulingEndToEndTest: saved/selected city changes no longer move the candle alarm; GPS updates do (also while a city is selected); device clock fixed in New York; Omer morning asserted in the device zone (New York) instead of the JVM zone.
- Renamed only (expectations unchanged, location now means device position): ReminderAlarmPlanTest omer morning/tzait, PostMidnightCandleLightingTest DST, OmerNightfallRolloverTest DST. ShiftedWindowReconciliationTest, PostMidnight*, OmerNightfallRollover*, VerifyTimeZoneCrossingTest, OmerTravelDeliveryTest pass unchanged: their location was already the device's position and zone.
- Mechanical: TodayProvider fakes -> deviceContextOn(date); planReminderAlarms(today, now, zone, position, ...) test overload (PlanReminderAlarmsAt.kt) requires today == device date; CalendarViewModel tests pass a DeviceContext.
New tests: device/DeviceContextTest (system clock, zone change, midnight, as-if shift incl. DST, off, devDayShiftFor, migration); notifications/DeviceVsDisplayCityTest (NY device vs Jerusalem city: rows 1-5, 11, 12, DST day, midnight boundary).
Verification: assembleDebug + testDebugUnitTest (675 tests, 0 failures) + lintDebug (0 errors; no warnings in changed files).

Rebased onto origin/master 8c05211 (TASK-19, TASK-9.1). Conflicts resolved in MainActivity (master's activeHolidayTheme now gets the device date), LocationService (master's off-main-thread geocoding kept; fix zone from DeviceContext.realZone), DateChangeReceiver/TimeChangeReceiver (WidgetUpdater.requestUpdate kept alongside icon/tile refresh).
Today widget routed through the device context: TodayWidget.snapshot = snapshots.build(prefs, deviceContext.now(prefs)) (no inline override/LocalDate.now/LocalTime.now/Instant.now); TodayWidgetSnapshots.build(prefs, device: DeviceNow) uses the device date/instant/zone and returns refreshAt on the real clock (device.toSystemInstant), so the refresh alarm is right with the override on. WidgetSettings keys on devDayShift.
OPEN QUESTION (owner, via coordinator): widget next-zman and Shabbat rows - selected city or device position? Left unchanged (selected city) behind the single seam UserPreferences.widgetTimesLocation() in widget/TodayWidgetSnapshot.kt, used by nextZman, shabbatTimes, locationName and WidgetSettings.
Widget tests: build call sites updated; new cases for the device date in New York at 23:30, override shift with a real-clock refresh, and the seam.
Grep after rebase (main): only device/DeviceContext.kt.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-29T20:19:00Z

---

Coordinator integration 2026-10-29: reviewed (incl. owner follow-ups: zone-only Israel auto-detect + nearest-offset fallback city, widget on device position, fixed-date dev override); merged onto master as 6afbf08. Combined :app:assembleDebug :app:testDebugUnitTest :app:lintDebug passed, 723 tests, 0 failures/errors. Emulator evidence in /tmp/levana-task5-evidence/ per agent notes.

---
<!-- COMMENTS:END -->
