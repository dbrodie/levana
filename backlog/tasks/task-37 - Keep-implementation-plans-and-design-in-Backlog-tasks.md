---
id: TASK-37
title: Keep implementation plans and design in Backlog tasks
status: In Progress
assignee: []
created_date: '2026-10-29T10:37:00Z'
updated_date: '2026-10-29T10:39:00Z'
labels: []
dependencies: []
type: docs
ordinal: 37000
---

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Remove the per-task RFE requirement from AGENTS.md and describe parent tasks and subtasks for larger changes.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Updated AGENTS.md to keep requirements, design and implementation plans in Backlog.md, using parent tasks and subtasks for larger changes. Existing specs remain optional references. Verified git diff --check, backlog doctor, assembleDebug, testDebugUnitTest and lintDebug; all passed. Awaiting integration.

Simplified the wording further: removed RFE workflow guidance, leaving only a short historical-reference note in the project section. Required build, tests and lint passed again.
<!-- SECTION:NOTES:END -->
