---
id: TASK-12.1
title: 'Screenshot safety net: Roborazzi baselines for Settings and app shell'
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T18:50:00Z'
updated_date: '2026-10-29T21:42:00Z'
labels: []
dependencies:
  - TASK-5
  - TASK-9
parent_task_id: TASK-12
priority: low
type: chore
ordinal: 44000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Record from current master after TASK-5 and TASK-9 merge, before any split. Configure roborazzi { outputDir = file("src/test/screenshots") } so baselines are committed, with a fixed device qualifier. Cover light/dark × English/Hebrew-RTL for: Settings main (notification sub-rows expanded, developer group shown), each Settings dialog, Locations (empty, with saved cities, without permission), Halachic Times, Developer, About, the LevanaApp drawer (cities collapsed and expanded) and the top bar for each route. Use stub repositories and a fixed clock so images are deterministic. Update ARCHITECTURE_SPEC so its Roborazzi claim matches reality and document the record/verify commands.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 recordRoborazziDebug produces committed, deterministic baselines (two consecutive verifyRoborazziDebug runs pass)
- [ ] #2 Coverage list above is met in both themes and both languages
- [ ] #3 ARCHITECTURE_SPEC documents the screenshot workflow accurately
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Re-survey (2026-10-29, master 0671863): SettingsScreen.kt is 1373 lines, MainActivity.kt 594. Public entry points stay as listed in TASK-12. PreferencesRepository, LocationService and UpdateChecker are final classes without interfaces; screens get them from Koin.

Approach: drive the real app. Tests launch the real MainActivity, with its theme resolution and LevanaApp, under a test Koin graph. Navigation is by UI clicks, plus the existing notification/widget deep link for Zmanim. Every image therefore shows the real top bar, NavHost wiring, screen wrappers and SettingsContent. No production code changes; no seams are planned.

1. Gradle: roborazzi { outputDir = file("src/test/screenshots") }. A fixed device qualifier for all images, set via @Config.
2. Shared harness (app/src/test/.../screenshots/):
   - @Config(application = Application::class). Koin is started per test with allModules plus overrides: in-memory Room, a fixed DeviceContext clock (noon 2026-12-08, Chanukah day 4, so MainActivity's holiday-theme resolution is in every image), empty system-time changes and no-op EventDataChanges.
   - The default time zone is fixed to America/New_York.
   - Preferences are reset before and after each test by clearing the app's preferences DataStore (reached by reflection, test-only), then seeded through PreferencesRepository: current location New York, saved Jerusalem and London, Jerusalem selected.
   - The package versionName and install source are pinned via ShadowPackageManager, so About does not change with git tags.
   - Location permission is granted, except in the no-permission case.
   - Variants come from ParameterizedRobolectricTestRunner: en/iw × light/night, applied with RuntimeEnvironment.setQualifiers before launch.
   - Dialogs and the drawer are captured with captureScreenRoboImage, so all windows are included.
3. Coverage, each in 4 variants:
   - Calendar (start route).
   - Drawer collapsed, and expanded with Jerusalem selected.
   - My Events, Add event, Edit event, Contact birthday, Zmanim (via deep link).
   - Settings main: top, middle and bottom scroll positions, with dev group shown, candle lighting on in morning mode, holidays on, and Omer on with the morning reminder on. One extra notifications image in hours_before mode.
   - The 7 Settings dialogs, plus the Developer date-override dialog.
   - Halachic Times, Developer, About.
   - Locations with saved cities, empty, and without permission.
   - City picker and System calendars.
4. Record, then run verifyRoborazziDebug twice. Inspect a sample of PNGs, report the baseline size, and tune density for size.
5. Update ARCHITECTURE_SPEC: an accurate Roborazzi section with record/verify commands.
6. Full verification command; notes with tested commit, results, limitations and the image list; commit.
<!-- SECTION:PLAN:END -->
