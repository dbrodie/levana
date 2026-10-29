---
id: TASK-5.2
title: Refresh today on resume, date change and time-zone change
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:47:00Z'
updated_date: '2026-10-29T19:36:00Z'
labels: []
dependencies: []
parent_task_id: TASK-5
priority: medium
type: bug
ordinal: 41000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-5 case table row 13. The calendar today highlight goes stale past midnight (state.today is set only in loadMonth). The icon is not refreshed on TIMEZONE_CHANGED/TIME_SET. Make the calendar, icon, tile and holiday theme observe the device context and update on resume, date change and time/zone change.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 With the app alive across midnight or a zone change, the calendar highlight, icon, tile and theme all show the new device date after resume, without restart
- [ ] #2 Unit tests with a fake clock cover the midnight advance; emulator evidence recorded
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
- DeviceContext gains `today: Flow<LocalDate>` (distinct) that re-evaluates on: preference changes (dev shift/position), a system time signal flow (Android callbackFlow over ACTION_DATE_CHANGED / ACTION_TIME_CHANGED / ACTION_TIMEZONE_CHANGED, injected so tests can drive it), and a timer to the next device midnight.
- CalendarViewModel collects it and updates state.today (highlight) in place; Resume intent also re-reads now. Selection/browsed month are left alone (only the highlight moves).
- MainActivity theme collects the today flow; icon updated on resume and when the today flow changes.
- TimeChangeReceiver also refreshes the launcher icon (TIMEZONE_CHANGED/TIME_SET). Tile reads DeviceContext on every onStartListening (already per-show) and requests a listening state refresh via TileService.requestListeningState on date change.
- Tests: fake clock + signal flow cover the midnight advance in CalendarViewModel and DeviceContext.today; zone change moves the date.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implementation (5.2):
- DeviceContext.todayUpdates: Flow<LocalDate> (distinct) re-read on preference changes (override/position), on systemTimeChanges and after each device midnight (untilNextDay: to 1 s past the next device midnight, DST-aware). device/SystemTimeChanges.kt: runtime receiver for DATE_CHANGED / TIME_SET / TIMEZONE_CHANGED, wired in Koin.
- CalendarViewModel collects it (state.today only; the selection stays) and re-reads today on Resume (covers a frozen process across midnight).
- MainActivity: holiday theme from todayUpdates (collectAsStateWithLifecycle, so it also refreshes on start); launcher icon updated from todayUpdates under repeatOnLifecycle(STARTED) (replaces the resume-only update).
- TimeChangeReceiver (manifest; TIMEZONE_CHANGED/TIME_SET) now also updates the icon and asks the tile to refresh; DateChangeReceiver refreshes the tile too (requestHebrewDateTileRefresh -> TileService.requestListeningState). The tile already reads DeviceContext in every onStartListening.
Tests: device/DeviceTodayUpdatesTest (midnight signal, zone change, override on/off, no repeats, midnight delay incl. DST day); ui/calendar/CalendarTodayRefreshTest (opens on device date, midnight advance with fake clock, resume re-read, zone change, override on/off).
Verification: assembleDebug + testDebugUnitTest (685, 0 failures) + lintDebug (no new issues). Emulator evidence: see TASK-5 notes.

Emulator-found fix: the zone-fallback device position was computed inside PreferencesRepository.preferences' map, so long-lived collectors (drawer, view models) kept the old zone's city after a time-zone change (one-shot reads such as the alarm planner were already fresh). PreferencesRepository now combines DataStore with systemTimeChanges, so the fallback position, Israel auto-detect and alarm settings re-evaluate on DATE/TIME/TIMEZONE changes. Verified on the emulator (drawer current location Tokyo -> New York live). No unit test: the fallback needs a store without a GPS fix, and the JVM-wide DataStore singleton holds fixes saved by other tests.
<!-- SECTION:NOTES:END -->
