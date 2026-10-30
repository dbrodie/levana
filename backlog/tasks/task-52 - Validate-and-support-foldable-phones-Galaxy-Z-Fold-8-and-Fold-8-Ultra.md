---
id: TASK-52
title: Validate and support foldable phones (Galaxy Z Fold 8 and Fold 8 Ultra)
status: To Do
assignee: []
created_date: '2026-10-30T10:30:00Z'
labels: []
dependencies: []
priority: medium
type: task
ordinal: 55000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner request 2026-10-30: make sure Levana works well on foldable phones, explicitly the Samsung Galaxy Z Fold 8 and Fold 8 Ultra, on both the cover and the inner display, and across fold/unfold.

Current state (coordinator check, master): targetSdk 36; no screenOrientation, resizeableActivity or configChanges overrides in the manifest; no WindowSizeClass or adaptive-layout code. Every screen is a single phone-width layout. On Android 16+ with targetSdk 36, large screens (smallest width ≥ 600dp) ignore orientation and resizability restrictions anyway, so the app must render and keep its state at any window size.

Subtasks:
1. Foldable test setup: device profiles and screenshot sizes.
2. Layout audit of every screen on cover, inner and split-screen sizes, with fixes.
3. Fold/unfold continuity: same screen, state kept, layout adapts.
4. Large-screen adaptive layouts: use the inner display's width (needs a product/design decision).
5. Postures and the outer surfaces: half-folded (Flex/tabletop) behaviour, the cover screen, widgets, the Quick Settings tile and the launcher icon.

Other relevant points, folded into the subtasks:
- multi-window and split-screen at arbitrary widths;
- rotation on the inner display (landscape and portrait), which uses the same state-preservation path as folding;
- display cutouts and the hinge;
- text scaling at large widths;
- dialogs and the date picker kept to a sensible max width;
- keyboard/mouse use on large screens;
- Play large-screen app quality guidelines (tier 2 or 3 as a target).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All subtasks are done; the app is usable and correct on Fold 8 and Fold 8 Ultra cover and inner displays and across fold/unfold, with evidence recorded
<!-- AC:END -->
