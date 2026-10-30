---
id: TASK-52.5
title: >-
  Foldable postures and outer surfaces: half-folded, cover screen, widgets,
  tile, icon
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T10:31:00Z'
updated_date: '2026-10-30T11:56:00Z'
labels: []
dependencies:
  - TASK-52.1
parent_task_id: TASK-52
priority: low
type: task
ordinal: 60000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Cover the remaining foldable-specific surfaces:
- **Half-folded (Flex/tabletop) posture:** nothing renders across the hinge in a broken way; decide whether any screen should adapt (e.g. Zmanim top/bottom), and make it optional.
- **Cover screen:** the app runs well on the narrow cover display, including continuing on the cover screen when the phone is folded with the app open.
- **Home-screen widgets** (Today, Month, Zmanim list) on both displays' launchers and at their resize limits.
- **The Quick Settings tile and the dynamic launcher icon** on both displays.
- **Notifications and their deep links** while folded and unfolded.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Each surface is verified on the cover and inner displays and in half-folded posture, with screenshots; issues are fixed or tracked
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Setup: build the debug APK; start Levana_Fold8 and Levana_Fold8_Ultra AVDs headless (own sessions); install; disable lock screen; get past onboarding.
2. Cover screen and fold continuity (both AVDs): open the app on the inner display on a few screens (month, zmanim, a day sheet, settings), fold, dismiss the keyguard, screenshot the cover, record the configuration and relaunches; unfold back. Note the TASK-50 crash if it hits and continue. Samsung-only behaviour (One UI 'continue apps on cover screen' setting, Flex Mode panel, Samsung launcher grids) is recorded as untested.
3. Half-folded (posture 2) on both AVDs and both rotations of the inner display: screenshot the main screens; check WindowLayoutInfo / folding feature bounds against what the app draws; nothing is broken across the hinge. Write a proposal (not built) for an optional tabletop/book adaptation.
4. Widgets: add Today, Month and Zmanim to the launcher on the inner and cover displays (Launcher3 UI via adb input/uiautomator), resize each to its min resize size and to large sizes; screenshot. Cross-check the Glance size buckets (COMPACT/FULL) and text overflow at the limits. Optionally add Roborazzi renders of the widgets at the limit sizes as new baselines (existing baselines untouched).
5. Quick Settings tile (add via cmd statusbar, check label/icon/subtitle, tap opens the app) and the dynamic launcher icon (day alias shown on both displays' launchers).
6. Notifications: post via the app's alarm receiver (root on emulator, or a near-time reminder), open from the shade folded and unfolded, confirm the deep link lands on the right screen/date.
7. Fix issues found within these surfaces only (widget sizes, breakpoints, overflow); keep the phone look; no in-app layout or state-restoration changes (52.2/52.3). File anything needing a decision as notes or tasks.
8. Save screenshots + README outside the worktree (/tmp/levana-task52.5-evidence/); run the full Gradle verify from clean test-results; record results in the notes; commit.
<!-- SECTION:PLAN:END -->
