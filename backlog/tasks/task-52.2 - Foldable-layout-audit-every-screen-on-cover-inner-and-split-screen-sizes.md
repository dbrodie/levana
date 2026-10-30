---
id: TASK-52.2
title: 'Foldable layout audit: every screen on cover, inner and split-screen sizes'
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T10:31:00Z'
updated_date: '2026-10-30T11:56:00Z'
labels: []
dependencies:
  - TASK-52.1
parent_task_id: TASK-52
priority: medium
type: task
ordinal: 57000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Audit every screen and dialog on the Fold 8 and Fold 8 Ultra cover display, inner display (portrait and landscape) and split-screen or freeform widths, in English/Hebrew (RTL) and light/dark:
- calendar (Month, both modes) and the day panel;
- Agenda and search;
- Zmanim;
- My Events, the event and birthday editors;
- the date picker;
- Settings and all sub-screens and dialogs;
- the drawer;
- the walkthrough;
- the time-zone mismatch dialog and the permission prompts.

Look for: stretched or awkwardly wide content, clipped or overlapping text, the calendar grid and day cells scaling badly, dialogs or pickers at full tablet width (cap the width), bottom sheets, touch targets, the hinge and display cutouts, and 200% text on the cover screen. Fix the issues that need no design decision. File or defer anything needing a new adaptive layout to the adaptive-layout subtask. Add Roborazzi baselines for key screens at the foldable sizes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Every screen is audited on all foldable sizes with screenshots and recorded findings; non-design issues are fixed; design-level items are moved to the adaptive-layout subtask
- [ ] #2 Roborazzi foldable baselines for the key screens; existing baselines unchanged except for justified fixes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Audit capture: add an opt-in audit mode to ScreenshotTest (Gradle properties -Plevana.audit.size=<FoldableSize|all>, optional font scale, output dir outside the worktree) so every existing screenshot test (all screens, dialogs, drawer, walkthrough, picker, editors, settings) renders at each FoldableSize in EN/HE x light/dark. Default runs unchanged.
2. Capture the 'before' set for all 8 sizes + 200% font on the covers into /tmp/levana-task52.2-evidence/before; review every screen; record findings per screen/size.
3. Emulator spot checks on Levana_Fold8 / Levana_Fold8_Ultra AVDs: inner and cover display separately (avoid fold mid-test because of TASK-50), insets, cutouts, landscape, 200% font on the cover.
4. Fix non-design issues with width caps / max content widths that only take effect above phone width (e.g. widthIn(max) + centring for dialogs, editors, walkthrough, month grid content; text clipping; touch targets; insets). Phone baselines must stay pixel-identical: verifyRoborazzi on all existing baselines after each fix; stop and report if a fix changes them.
5. Capture the 'after' set; build before/after pairs per issue with a README in the evidence dir.
6. Add FoldableScreenshotTest baselines for key screens (calendar, day panel, agenda, zmanim, events, editor, settings, a dialog, date picker, walkthrough, drawer) at the foldable sizes, keeping the image count reasonable.
7. Record design-level findings (two-pane, nav rail, using extra width) in TASK-52.4 notes.
8. Verify: clean app/build/test-results, assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks; record evidence; commit. No merge/push/Done.
<!-- SECTION:PLAN:END -->
