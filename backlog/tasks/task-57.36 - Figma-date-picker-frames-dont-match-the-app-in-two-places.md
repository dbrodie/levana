---
id: TASK-57.36
title: 'Figma: date picker frames don''t match the app in two places'
status: To Do
assignee: []
created_date: '2026-11-12T22:12:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: low
type: chore
ordinal: 108000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Two mismatches between the Figma date picker frames and the app, found in TASK-57.14 and TASK-57.28:
- The Gregorian-led October 2026 frames show no holiday dot on 23 Tishrei (4 Oct). That is correct for Israel, but the app defaults to the Diaspora, where Simchat Torah is that day and the app shows a dot. Decide which location the frames represent and make them consistent (label the frame, or add the dot).
- The Hebrew (RTL) Hebrew-led picker frame (node 44:2712) has the "Gregorian" segment ticked while it shows the Hebrew-led grid. The selected segment should match the grid shown.
Figma file: see specs/FIGMA.md. Figma-only; no code change.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The October picker frames state or match the location they represent (Israel or Diaspora)
- [ ] #2 The Hebrew-led RTL picker frame shows the Hebrew segment selected
<!-- AC:END -->
