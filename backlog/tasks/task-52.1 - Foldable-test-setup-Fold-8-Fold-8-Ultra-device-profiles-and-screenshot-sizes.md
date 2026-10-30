---
id: TASK-52.1
title: >-
  Foldable test setup: Fold 8 / Fold 8 Ultra device profiles and screenshot
  sizes
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T10:31:00Z'
updated_date: '2026-10-30T10:40:00Z'
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
