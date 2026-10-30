---
id: TASK-52.1
title: >-
  Foldable test setup: Fold 8 / Fold 8 Ultra device profiles and screenshot
  sizes
status: To Do
assignee: []
created_date: '2026-10-30T10:31:00Z'
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
