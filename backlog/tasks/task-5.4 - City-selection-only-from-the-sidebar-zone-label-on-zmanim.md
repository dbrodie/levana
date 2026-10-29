---
id: TASK-5.4
title: City selection only from the sidebar; zone label on zmanim
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:47:00Z'
updated_date: '2026-10-29T19:51:00Z'
labels: []
dependencies: []
parent_task_id: TASK-5
priority: medium
type: feature
ordinal: 43000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-5 case table row 9 and the owner's Settings decisions. Adding a city in Settings → Locations no longer makes it active; choosing happens only in the sidebar/drawer. Keep the Settings wording as is, but remove the secondary text under the main Settings Location row that shows the current city (SettingsScreen.kt:347). The Zmanim screen and day-panel times show the selected city's zmanim in the city's own clock, with a zone label (e.g. 'Jerusalem time') only when the city's zone differs from the device zone.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Adding a city does not change the selected city; removing the selected city still falls back to Current location
- [x] #2 The Settings Location row no longer shows the current city as secondary text
- [x] #3 Zone label appears only when zones differ, in English and Hebrew; tests and emulator screenshots recorded
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
- PreferencesRepository.addSavedLocation no longer sets active_location_id; removal fallback unchanged. CityPicker/Locations settings unchanged otherwise; selection only via drawer.
- SettingsScreen main Location row: drop the secondary current-city text (and the now unused state field); other wording kept.
- Zone label: displayCityZoneLabel(displayCity, deviceZone): null when zones are equal, else "<city> time" (EN) / "שעון <city>" (HE). Shown on the Zmanim screen header and the day panel times section. ZmanimState/DayDetailState carry it; VMs compute it from DeviceContext.now().zone.
- Tests: add-city doesn't select; remove selected falls back; label null/non-null; VM sets the label; emulator screenshots EN/HE.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implementation (5.4):
- PreferencesRepository.addSavedLocation no longer sets active_location_id (CityPicker add = save only); removeSavedLocation fallback unchanged. Selection stays in the drawer (MainActivity) only.
- Settings main Location row: secondary current-city text removed (and the now unused SettingsState.locationName); other wording unchanged.
- ui/DisplayCityClock.kt otherClockCityName(displayCity, deviceZone): city name when the zone rules differ (aliases equal), else null. ZmanimState/DayDetailState.otherClockCityName set by ZmanimViewModel/DayDetailViewModel (DayDetailViewModel now takes DeviceContext). CityClockLabel ("%1$s time" / "שעון %1$s") shown at the top of the Zmanim list and above the day panel's quick times.
- PROJECT_SPEC.md selected-city bullet updated.
Tests: ui/zmanim/DisplayCityZmanimTest (adding doesn't select; removing the selected city falls back; label only when clocks differ incl. alias; Zmanim on the device date (NY Thursday 23:30) with Jerusalem's zmanim and label; no label for the current location; EN + HE strings). DayDetailViewModel test constructors pass a DeviceContext (mechanical).
Limitation: the label is computed when the day/zmanim load; a device zone change while the same day stays open updates it on the next load.
Verification: assembleDebug + testDebugUnitTest (697, 0 failures) + lintDebug. Emulator screenshots: see TASK-5 notes.
<!-- SECTION:NOTES:END -->
