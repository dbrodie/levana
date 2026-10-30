---
id: TASK-52.1
title: >-
  Foldable test setup: Fold 8 / Fold 8 Ultra device profiles and screenshot
  sizes
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T10:31:00Z'
updated_date: '2026-10-30T10:52:00Z'
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
- [ ] #1 Fold 8 and Fold 8 Ultra display specs are recorded with sources; emulator profiles for cover and inner displays and a fold/unfold-capable emulator are documented and working
- [ ] #2 Roborazzi supports foldable size qualifiers, with one sample baseline per size proving it
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
<!-- SECTION:NOTES:END -->
