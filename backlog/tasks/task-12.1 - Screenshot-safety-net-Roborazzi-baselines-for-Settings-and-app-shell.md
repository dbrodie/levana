---
id: TASK-12.1
title: 'Screenshot safety net: Roborazzi baselines for Settings and app shell'
status: Done
assignee:
  - '@claude'
created_date: '2026-10-29T18:50:00Z'
updated_date: '2026-10-30T00:32:00Z'
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
- [x] #1 recordRoborazziDebug produces committed, deterministic baselines (two consecutive verifyRoborazziDebug runs pass)
- [x] #2 Coverage list above is met in both themes and both languages
- [x] #3 ARCHITECTURE_SPEC documents the screenshot workflow accurately
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

Revision during implementation:
- The fixed date moved from Chanukah to 7 Kislev 5787 (2026-11-17). MainActivity's holiday theme arrives on a later composition and was occasionally missing from captures.
- The Developer date-override dialog is not captured. It is GoToDateDialog (ui.calendar, outside the split, under redesign in TASK-45) and never idles under Robolectric.
- Launches that never receive their first preferences value are retried (see notes).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Verification (tested commit 7347a2d)
- `set -a && . ./.env && set +a && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks`: BUILD SUCCESSFUL, 808 unit tests, 0 failures, lint clean.
- `verifyRoborazziDebug` run again right after: pass.
- Over the final harness, 8 consecutive screenshot verify runs passed (6 before the commit, 2 on 7347a2d), 0 diffs.
- ktlintTestSourceSetCheck: clean.
- Note: the record and verify tasks are up-to-date-cached; add `:app:testDebugUnitTest --rerun` to force a real run.

## What was built
- `app/build.gradle.kts`: `roborazzi { outputDir = src/test/screenshots }`.
- `app/src/test/.../screenshots/`:
  - ScreenshotHarness (base class);
  - AppShellScreenshotTest;
  - SettingsScreenshotTest;
  - SettingsStateScreenshotTests (hours-before mode, Locations empty, Locations without permission).
- Each test launches the real MainActivity and navigates by UI clicks, or by the widget deep link for Zmanim. It runs on the production Koin graph with these overrides:
  - in-memory Room;
  - a DeviceContext fixed at noon 2026-11-17 America/New_York;
  - empty system-time changes;
  - no-op EventDataChanges.
- Preferences: the shared DataStore is cleared before and after each test, then seeded with current location New York and saved cities Jerusalem and London (Jerusalem selected).
- The About version and installer are pinned via ShadowPackageManager, so git tags don't change the image.
- The new-birthday editor starts on the real Hebrew year (`ContactBirthdayState` default via `DeviceContext.realToday()`). The test steps it to the fixed date's year, so that image doesn't age.
- Device qualifier: `w393dp-h851dp-normal-long-notround-port-mdpi-keyshidden-nonav`. Variants are en/iw × notnight/night.
- No production code changed; no test seams added.
- Test-only workaround: the preferences DataStore is reached by reflection (private delegate in PreferencesRepository.kt) to clear it.

## Baselines: 116 PNGs, 3.2 MB
29 screens × 4 variants (`<name>_{en_light,en_dark,he_light,he_dark}.png`):
- **app:** calendar, drawer, drawer_cities, events_birthdays, events_events (top-bar overflow action slot), add_event, edit_event, contact_birthday, zmanim.
- **settings main (dev group shown; candle morning, holidays, Omer + morning on):** main_1_top, main_2_middle, main_3_notifications, main_4_bottom.
- **settings, other states:** notifications_hours_before.
- **dialogs:** israel_mode, candle_offset, yahrzeit_leap, yahrzeit_first_year, yahrzeit_missing_day, app_language, candle_mode.
- **settings sub-screens:** halachic_times, developer, about, locations, locations_empty, locations_no_permission, city_picker, system_calendars.
- **Top bar per route:** every route except Calendar (which has its own header) is captured with its title and navigation icon.

Images inspected by eye, all real content and no blank or error screens:
- app_calendar en_light and he_light, app_drawer en_light, app_drawer_cities he_dark;
- settings_main_1_top he_light, settings_main_2_middle en_light, settings_main_3_notifications en_light, settings_main_4_bottom en_dark;
- settings_dialog_candle_offset he_dark;
- settings_developer he_dark, settings_about en_light;
- settings_locations_no_permission en_dark, settings_city_picker he_light, settings_notifications_hours_before en_light, settings_system_calendars he_dark;
- app_add_event en_dark, app_contact_birthday en_light, app_events_events he_light, app_zmanim he_dark.
Hebrew renders right-to-left throughout.

## Limitations and findings
- **Launch stall (Robolectric + Compose test dispatcher):**
  - Symptom: a launch occasionally (about 1–6 of 64 per run) never delivers the first preferences value to MainActivity's and LevanaApp's `collectAsState`. LevanaApp stays empty (ComposeView 0×0).
  - What doesn't revive it: later DataStore emissions, Snapshot apply notifications, or advancing either clock.
  - Ruled out: DataStore itself delivers to fresh collectors, and the Recomposer is alive.
  - Workaround: the harness closes such a launch and relaunches, up to 5 attempts. An empty launch cannot be mistaken for a real image. No test needed more than one relaunch over the last 8 runs.
  - Tried and abandoned: a StandardTestDispatcher effect context driven frame by frame. Composition then needs clock ticks, and the Settings flows ran out of memory.
- **Holiday theme not covered:** MainActivity's holiday theme isn't in the images (non-holiday date). Theme selection logic stays covered by HolidayTheme/widget theme unit tests, but not the MainActivity wiring.
- **Not captured:** the GoToDateDialog from Developer, and the Force Holiday Theme dropdown expanded.
- **Orphaned tracked baselines:** `app/src/test/screenshots/dayOfWeekHeader_{gregorian_ltr,hebrew_rtl}.png` belong to a test removed in 48c03af. The plugin also re-syncs them from build/intermediates. They are left untouched; deleting them is recommended but needs approval.
- **Platform scope:** Robolectric renders only; device behavior (permissions dialogs, edge-to-edge insets) is outside these images.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-30T00:32:00Z

---

Coordinator integration 2026-10-30: reviewed harness (launch-readiness retry only, no production changes) and sample images; merged as f00f915. Combined assembleDebug + testDebugUnitTest + lintDebug + verifyRoborazziDebug (--rerun-tasks) passed: 808 tests, 0 failures; Roborazzi 116 unchanged. Open: two stale baselines from a removed test (dayOfWeekHeader_*.png) await owner decision.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Recorded 116 Roborazzi baselines from current code. They cover 29 screens in en/he × light/dark, at mdpi, 3.2 MB in app/src/test/screenshots. Tests launch the real MainActivity under a fixed-clock Koin graph and navigate by UI, so the images cover the top bar per route, the drawer (cities collapsed and expanded, selected city), Settings main and its dialogs, and the sub-screens and Locations states. No production changes. verifyRoborazziDebug passed in 8 consecutive runs; the full build, tests and lint pass on 7347a2d. A Robolectric launch-stall is handled by relaunching. The holiday theme and GoToDateDialog are not captured. Orphaned dayOfWeekHeader baselines are flagged for deletion. Details in notes.
<!-- SECTION:FINAL_SUMMARY:END -->
