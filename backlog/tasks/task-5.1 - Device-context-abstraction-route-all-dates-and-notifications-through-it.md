---
id: TASK-5.1
title: Device context abstraction; route all dates and notifications through it
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:47:00Z'
updated_date: '2026-10-29T18:53:00Z'
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
- [ ] #1 All date/now/zone reads outside the abstraction are gone (grep evidence recorded)
- [ ] #2 Notification triggers and text use the device position and zone; the selected city has no effect (tests with New York device vs Jerusalem city)
- [ ] #3 Dev override behaves as an as-if device clock everywhere, including receivers and alarm triggers
- [ ] #4 PROJECT_SPEC.md and ARCHITECTURE_SPEC.md updated
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
