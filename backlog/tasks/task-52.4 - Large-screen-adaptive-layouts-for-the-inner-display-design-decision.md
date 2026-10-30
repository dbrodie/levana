---
id: TASK-52.4
title: Large-screen adaptive layouts for the inner display (design decision)
status: To Do
assignee: []
created_date: '2026-10-30T10:31:00Z'
labels: []
dependencies:
  - TASK-52.2
parent_task_id: TASK-52
priority: low
type: feature
ordinal: 59000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Decide how Levana should use the inner display's width, then implement it with WindowSizeClass / Compose adaptive APIs. Candidates:
- two-pane Month (calendar grid and day panel side by side);
- Agenda list with a detail pane;
- Settings list/detail;
- a navigation rail instead of the drawer at expanded width;
- wider but capped content in editors.

Start with a short proposal and mockups (Claude Design) for owner approval. Phones must stay unchanged, and every pane must keep the TASK-5 device/display-city rules.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Owner-approved adaptive design recorded; implemented for the agreed screens with phone layouts unchanged; baselines at compact, medium and expanded widths
<!-- AC:END -->
