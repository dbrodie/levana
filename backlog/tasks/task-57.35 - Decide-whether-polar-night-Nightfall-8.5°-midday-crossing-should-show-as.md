---
id: TASK-57.35
title: >-
  Decide whether polar-night Nightfall (8.5° midday crossing) should show as
  --:--
status: Done
assignee:
  - '@claude'
created_date: '2026-11-04T14:16:00Z'
updated_date: '2026-11-08T11:33:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: low
ordinal: 105000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Follow-up from TASK-57.31. In the polar night (e.g. Longyearbyen Nov 2026) there is no sunrise or sunset, but the sun still crosses 8.5° below the horizon around midday, so KosherJava returns a Nightfall (tzais) time, e.g. 1:37 PM on Sat 2026-11-21. TASK-57.31 stopped deriving havdalah and candle lighting from it (both now --:--), but the plain Nightfall row in the day sheet / Zmanim screen, the day sheet TZEIT quick card on non-Friday days, the Today widget "Next: Nightfall", the fast end (getFastTimes) and the Omer nightfall reminder still use the crossing. Evidence: /Users/daniel/.paseo/evidence/task-57.31/impl/10-daysheet-longyearbyen-sat-nov21-2026-en.png, 12-today-widget-longyearbyen-polar-night-en.png.

Decision (owner, 2026-11-08): follow TASK-57.31's rule — the app picks no substitute; a time it can't properly calculate shows "--:--".
- On a polar day (no sunrise and/or no sunset on that date), the twilight times derived from depression-angle crossings are not shown: Nightfall, Alot HaShachar and Misheyakir show "--:--" everywhere (day sheet rows and quick cards, Zmanim screen, Agenda, widgets incl. "Next: …", fast end). Chatzot and Chatzot HaLaylah stay (they are always defined). Sunrise/sunset-based times are already "--:--".
- Reminders that depend on those times (Omer nightfall reminder, fast-end and similar) are simply not scheduled on such days, without crashing.
- White nights (sunrise and sunset exist, crossing not reached) are unchanged: already "--:--" via KosherJava returning null.
- The 57.31 city note already covers these locations; no new UI, strings or Figma changes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 On a polar-night or midnight-sun day, Nightfall, Alot HaShachar and Misheyakir show "--:--" in the day sheet, Zmanim screen, Agenda and widgets (including the widget's next-zman line); Chatzot and Chatzot HaLaylah are still shown
- [x] #2 Fast end and the Omer nightfall reminder use no polar crossing: the fast end shows "--:--" and the reminder is not scheduled, without crashing
- [x] #3 Unit tests pin Longyearbyen polar night (Nov 2026) and midnight sun (Jun 2027), and St. Petersburg and Jerusalem are unchanged
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Consumers of tzais / alos / misheyakir (all go through ZmanimRepository; no other KosherJava calls in app/src/main):
- getZmanim (Alot, Misheyakir, Nightfall rows) → DayDetailViewModel (day sheet rows + TZEIT quick card, DayDetailScreen:218), ZmanimViewModel (Zmanim screen), WidgetZmanim.selectedZmanimOn/nextSelectedZman → Today widget "Next: …" and Zmanim widget rows.
- getTzaitTime (tests only), getTzaitInstant → ReminderAlarmPlan Omer nightfall reminder (previous evening + window).
- getFastTimes (start = alos for minor fasts, end = tzais) → DailyNotificationWorker.handleFasts → fast notification (omits a null time).
- nightfallAfterSunset (candle lighting at nightfall, havdalah) → getShabbatInfo / getCandleLightingInstant / getObservanceEnd → Agenda, widgets, candle/havdalah reminders (already null on no-sunset days via 57.31).

Plan:
1. ZmanimRepository: one private rule — a "polar day" is a date with no sea-level sunrise and/or no sea-level sunset; on it the depression-angle twilight times (alos 16.1°, misheyakir 11°, tzais 8.5°) are null. Private extension accessors alot/misheyakir/nightfall on ComplexZmanimCalendar used by getZmanim, getTzaitTime, getTzaitInstant, getFastTimes and the existing nightfallAfterSunset (replaced). Chatzot / Chatzot HaLaylah untouched. No solar-dip / isAssurBemlacha calls.
2. No UI changes: screens/widgets already render null as MISSING_TIME; Omer planner skips a null tzait; fast notification omits null times.
3. Tests (ZmanimHighLatitudeTest): Longyearbyen Sat 2026-11-21 polar night (Alot/Misheyakir/Nightfall null although KosherJava has crossings; Chatzot/Chatzot HaLaylah present; getTzaitTime/Instant null); midnight sun Jun 2027; Tromsø 10 Tevet 5787 (2026-12-20) fast start/end null; Omer nightfall reminder in the Antarctic polar night (McMurdo, May 2027) not scheduled, no crash; Today widget next zman skips Nightfall; St. Petersburg June and Jerusalem unchanged (white night/normal values still from KosherJava).
4. Verify: assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug; emulator check (Longyearbyen GPS fix + date override) of day sheet, Zmanim screen, Agenda, Today widget, EN+HE; Jerusalem spot-check. Evidence in /Users/daniel/.paseo/evidence/task-57.35/.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation (2026-11-08, tested commit 98f19c0)

**Code** (`data/ZmanimRepository.kt` only): one rule — a polar day is a date with no sea-level sunrise and/or no sea-level sunset (`ComplexZmanimCalendar.isPolarDay`). Private accessors `alot` / `misheyakir` / `nightfall` return null on such a day and KosherJava's alos 16.1° / misheyakir 11° / tzais 8.5° otherwise. Used by getZmanim (Alot, Misheyakir, Nightfall rows), getTzaitTime, getTzaitInstant (Omer nightfall reminder), getFastTimes (minor-fast start and every fast end), candle lighting at nightfall and havdalah (replaces 57.31's sunset-only `nightfallAfterSunset`; havdalah/at-nightfall lighting now also need a sunrise that day, same rule). Chatzot / Chatzot HaLaylah untouched. No UI, strings or Figma changes: day sheet, Zmanim screen, Agenda and widgets already render null as "--:--"; the widget's next-zman line skips null times; the Omer planner skips a null tzait; the fast notification leaves out a null time. No solar-dip / isAssurBemlacha calls.

**Tests** (`ZmanimHighLatitudeTest`, +6, one 57.31 assertion moved from getTzaitTime to raw KosherJava): Longyearbyen Sat 2026-11-21 (raw crossings exist; Alot/Misheyakir/Nightfall null in getZmanim, getTzaitTime/Instant null; Chatzot/Chatzot HaLaylah present); midnight sun 2027-06-21; Today/Zmanim widget next zman (Nightfall selected → none; Chatzot selected → Chatzot); 10 Tevet 2026-12-20 in Tromsø polar night (raw alos/tzais exist) → fast start and end null; Omer in the Antarctic polar night (McMurdo 2027-05-05, raw tzais exists) → no OmerEvening alarm, morning reminder still scheduled, no crash; St. Petersburg June 2027 (white night) and Jerusalem all of 2027 equal raw KosherJava for Alot/Misheyakir/Nightfall/tzait instant, Jerusalem 17 Tammuz fast times present. Longyearbyen Omer isn't a polar-night case (midnight sun, tzais already null), hence McMurdo.

**Verification** (98f19c0): `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug` BUILD SUCCESSFUL; 1257 tests, 0 failures; lint 0 errors; Roborazzi verified, no baseline changes. Emulator (own headless Levana_Agent_Template_API_36_1, this build), Longyearbyen via GPS fix as current location, real date 2026-11-08 (already polar night, crossing exists), day sheet opened on Sat 2026-11-21: quick cards NETZ/SHKIA/TZEIT --:--; day sheet Nightfall --:-- (was 1:37 PM); Zmanim screen Alot/Misheyakir/Nightfall/Havdalah --:--, Chatzot 12:43 PM, Chatzot HaLaylah 12:43 AM; Agenda all candle/havdalah --:--; Today widget "Next zman --:--". Same in Hebrew. Jerusalem (selected city) Nov 8: 6:01 / 4:44 / 5:21, Alot 4:46, Misheyakir 5:11 — unchanged. No AndroidRuntime errors. Screenshots: /Users/daniel/.paseo/evidence/task-57.35/01–14-*.png.

**Limitations:** fast and Omer reminders verified by unit tests only (no notification triggered on the device). Zmanim widget not placed (shares getZmanim / NO_TIME with the Today widget). Store listing: no change.
<!-- SECTION:NOTES:END -->
