---
id: TASK-52.1
title: >-
  Foldable test setup: Fold 8 / Fold 8 Ultra device profiles and screenshot
  sizes
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T10:31:00Z'
updated_date: '2026-10-30T11:50:00Z'
labels: []
dependencies: []
parent_task_id: TASK-52
priority: medium
type: chore
ordinal: 56000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Establish how foldables are tested, before any audit.
- Look up the official display specs (resolution, density, size; cover and inner) of the Galaxy Z Fold 8 and Fold 8 Ultra from Samsung's sources, and record them with links. Don't guess.
- Create emulator AVD profiles for both (cover and inner displays) or the closest equivalent, plus the stock Android foldable/resizable emulator for fold/unfold events. Document the commands in the android-debug skill or ARCHITECTURE_SPEC.
- Add Roborazzi device qualifiers for the cover, inner (portrait and landscape) and a split-screen half width, so later subtasks can add large-screen baselines.
- If real devices are available, document how to test on them over wireless ADB.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Fold 8 and Fold 8 Ultra display specs are recorded with sources; emulator profiles for cover and inner displays and a fold/unfold-capable emulator are documented and working
- [x] #2 Roborazzi supports foldable size qualifiers, with one sample baseline per size proving it
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Specs: find Samsung's official display specs for Galaxy Z Fold 8 and Fold 8 Ultra (cover + inner: inches, px, density); record with source links; label anything not officially published; convert to dp per display/orientation.
2. Emulators: create hardware profiles + AVDs (API 36.1 image already installed) for Fold 8 cover/inner and Fold 8 Ultra cover/inner; set up the stock foldable AVD (7.6 Fold-in with outer display) for fold/unfold. Install + launch the debug build on each, screenshot, trigger fold/unfold (emulator console 'fold'/'unfold', posture, adb wm size/density) and prove the activity receives the configuration change (logcat / dumpsys). Save evidence outside the worktree with a README.
3. Document AVD creation and fold/unfold commands in the android-debug skill (new reference file) and point to it from ARCHITECTURE_SPEC.
4. Roborazzi: let ScreenshotTest take an optional device qualifier (default DEVICE, unchanged); add FoldableScreenshotTest parameterized over foldable sizes (cover, inner portrait, inner landscape, split-screen half) for Fold 8 / Fold 8 Ultra where they differ; record one Month calendar EN light baseline per size. Existing baselines untouched.
5. Verify: assembleDebug, testDebugUnitTest, lintDebug, verifyRoborazziDebug --rerun-tasks from clean test-results; note layout problems seen in samples for 52.2; commit.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Display specs (official Samsung figures)

Sources (read 2026-10-30):
- UK support, Fold8 Ultra vs Fold8 (resolution, ppi, ratios; updated 29 Oct 2026): https://www.samsung.com/uk/support/mobile-devices/what-is-the-difference-between-the-galaxy-z-fold8-ultra-and-z-fold8/
- US compare page (sizes, measurement footnotes): https://www.samsung.com/us/smartphones/galaxy-z-fold8/compare/
- Canada support specs (sizes, Fold8 4:3): https://www.samsung.com/ca/support/mobile-devices/galaxy-z-fold8-ultra-galaxy-z-fold8-and-galaxy-z-flip8-specifications-and-features/
- Global newsroom (not readable by fetch here, timed out; search excerpts agree with the UK figures): https://news.samsung.com/global/samsung-galaxy-z-fold8-ultra-fold8-and-flip8foldables-perfected-for-every-way-of-living

| Device / display | Size | Resolution as published | ppi | Ratio |
|---|---|---|---|---|
| Fold8 cover | 5.5" (5.4" with rounded corners) | 1972 x 1248 (UK: "HD+"; newsroom excerpt: "WUXGA+ 1,248 x 1,972") | 428 | 16:10 |
| Fold8 inner | 7.6" | 2448 x 1848 QXGA+ | 403 | 4:3, "wider than it is tall" when unfolded |
| Fold8 Ultra cover | 6.5" | 2520 x 1080 FHD+ | 422 | not stated (21:9 by arithmetic) |
| Fold8 Ultra inner | 8.0" | 2504 x 2256 QXGA+ | 422 | not stated |

Labelled inferences (not published by Samsung):
- Orientation: Fold8 cover is portrait 1248 wide x 1972 tall (a phone held upright; newsroom writes 1,248 x 1,972). Fold8 inner is natively landscape 2448 x 1848 ("wider than it is tall"). Fold8 Ultra cover portrait 1080 x 2520. Fold8 Ultra inner is taken as portrait 2256 x 2504, like the Fold7 inner it matches in size (Samsung: "same as Galaxy Z Fold7"); this orientation is an assumption.
- Android density: Samsung publishes no dpi/dp figures. We ASSUME 420 dpi (dp = px / 2.625), the default density of Samsung's FHD+ phones; check with `adb shell wm density` on a real device and adjust the qualifiers if it differs. Window insets (status/navigation bars, cutout) are ignored: these are full-display sizes.

dp at the assumed 420 dpi (width x height, rounded):
| Display | Portrait | Landscape |
|---|---|---|
| Fold8 cover | 475 x 751 | 751 x 475 |
| Fold8 inner | 704 x 933 | 933 x 704 (natural) |
| Fold8 Ultra cover | 411 x 960 | 960 x 411 |
| Fold8 Ultra inner | 859 x 954 (assumed natural) | 954 x 859 |
| Split-screen half, Fold8 inner landscape | 466 x 704 | |
| Split-screen half, Fold8 Ultra inner landscape | 477 x 859 | |
Window size classes: both covers compact width; Fold8 inner portrait medium (600-839), landscape expanded (>=840); Ultra inner expanded in both orientations; split halves compact.

## Emulators (done)

Script: `.agents/skills/android-debug/scripts/foldable_avds.py create`. Docs: `.agents/skills/android-debug/references/foldables.md`, linked from SKILL.md and ARCHITECTURE_SPEC. Two AVDs on the template's android-36.1 image:
- `Levana_Fold8_API_36_1`: inner 2448x1848 at 420 dpi, sw704dp w933dp h704dp. Cover display 1248x1972, w475dp h751dp.
- `Levana_Fold8_Ultra_API_36_1`: inner 2256x2504, sw859dp w859dp h954dp; rotated, w954dp h859dp. Cover 1080x2520, w411dp h960dp.
The configurations the app received match the dp table above exactly.

How fold works: the emulator gives only its Pixel Fold device names (`pixel_fold`, `pixel_9_pro_fold`) a second built-in display for the cover. The system moves the app to that display on fold, as on a real Samsung. So each AVD sets that device name with Samsung's sizes; this one AVD per device covers both the cover and the inner display. Tried first and rejected:
- A generic hinge AVD with `hw.displayRegion` (the mechanism of Studio's deprecated stock "7.6in Foldable", API ≤34). `emu fold` changes the device state to CLOSED, but the display stays the full inner panel, headless or windowed. It also clamps the region to the inner panel ("folded height 1972 is larger than lcd height 1848").
- The resizable emulator was not needed.

Commands proven:
- `adb emu fold` / `adb emu unfold` / `adb emu posture 1|2|3`, then `adb shell wm dismiss-keyguard`. Each fold or unfold shows Android's "Swipe up to continue" screen. `fold_lock_behavior_setting=stay_awake_on_fold_key` did not remove it.
- Evidence: `cmd device_state state`, `screencap -d <display-id>`, the dumpsys window config, and the events log.
- Rotation: `settings put system user_rotation 1`.
- HALF_OPENED via posture 2.
Each fold and unfold recreates MainActivity (`handleRelaunchActivity`), and the cover shows the month view at 475dp/411dp. Real devices: the wireless ADB pair/connect/attach steps are documented, but no real device was available here.

FINDING (already tracked, TASK-50): folding the Ultra crashed the app once (the first fold cycle after a fresh install): MainActivity was relaunched twice within 80 ms, then `ClassCastException: DrawerValue cannot be cast to Bundle` in NavControllerSaver at LevanaApp.kt:36. That was 1 of 4 clean cycles on the Ultra and 0 of 1 on the Fold8. A note was appended to TASK-50; TASK-52.3 needs that fix.

Evidence: `/tmp/levana-task52.1-evidence/` (README.md, emulator/fold8, emulator/fold8ultra with screenshots, lifecycle logs, the crash trace and fold-check.sh).

## Roborazzi (done)

- `ScreenshotTest(variant, device = null)`: an optional full Robolectric qualifier string, set before the variant's `+` qualifiers. The default is unchanged (class `@Config` DEVICE).
- `FoldableSize`: FOLD8_COVER 475x751, FOLD8_INNER_PORTRAIT 704x933, FOLD8_INNER_LANDSCAPE 933x704, FOLD8_SPLIT_HALF 466x704, FOLD8_ULTRA_COVER 411x960, FOLD8_ULTRA_INNER_PORTRAIT 859x954, FOLD8_ULTRA_INNER_LANDSCAPE 954x859, FOLD8_ULTRA_SPLIT_HALF 477x859. All dp at mdpi, with qualifiers `w..dp-h..dp-port|land-mdpi-keyshidden-nonav`.
- `FoldableScreenshotTest`: 8 baselines, `foldable_calendar_<size>_en_light.png`; each image is the window in dp. No existing baseline changed: the git diff adds files only.
- Limitations: full-display sizes (no system bars, cutout or hinge); split-screen half ignores the divider; 420 dpi is assumed.

## What the samples show (for TASK-52.2, not fixed here)
- Inner display (all four inner sizes): the day panel bottom sheet is capped at ~640dp and centred, with empty gutters, while the month grid stretches across the full width. On landscape inner the cells are very wide and mostly empty; text and chips stay phone-sized.
- Ultra inner portrait (859x954): the grid takes ~800dp of height and the sheet peeks only zmanim; a large blank area sits below the last week.
- Cover and split sizes (411-477dp wide) look like the phone layout: tall cells, and event chips truncated ("Vayishla…", "Chayei …"), the same as on the 393dp phone.
- Emulator, Fold8 landscape inner: the same capped, centred sheet. Cover: fine.

## Verification (tested commit 2e9d008)
`rm -rf app/build/test-results && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks`: BUILD SUCCESSFUL. 1033 tests, 0 failures, 0 errors, 0 skipped (8 in FoldableScreenshotTest). Roborazzi verify passed, and git status was clean afterwards. Lint: 0 errors, 83 warnings; this change touches only tests, the skill and docs. Log: /tmp/levana-task52.1-evidence/gradle-verify.log. Emulator runs used the debug build of this branch; its app code is identical to master 91ec9d5.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Foldable test setup for the Galaxy Z Fold8 and Fold8 Ultra.
- Specs: Samsung's official figures are recorded with links. Android density is not published, so 420 dpi is assumed and labelled.
- Emulators: foldable_avds.py creates a two-display AVD per device, with Samsung's inner and cover sizes. Fold/unfold, posture, rotation and evidence commands are documented in the android-debug skill's references/foldables.md.
- Roborazzi: ScreenshotTest takes an optional device qualifier, and FoldableSize holds 8 window sizes (cover, inner portrait and landscape, and split half per device). FoldableScreenshotTest records one month-calendar sample per size. Existing baselines are unchanged.
- Verified: full build, unit tests, lint and Roborazzi verify are green. On both AVDs the app installs and launches, and fold/unfold moves and recreates MainActivity at the expected dp sizes.
- Finding: folding on the Ultra hit TASK-50's restore crash once in 4 clean cycles (note appended to TASK-50; 52.3 needs that fix). Layout observations for 52.2 are in the notes.
- Not done: no testing on a real device.
<!-- SECTION:FINAL_SUMMARY:END -->
