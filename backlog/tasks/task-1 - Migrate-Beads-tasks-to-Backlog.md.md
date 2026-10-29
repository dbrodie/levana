---
id: TASK-1
title: Migrate Beads tasks to Backlog.md
status: In Progress
assignee: []
created_date: '2026-10-29T10:23:00Z'
updated_date: '2026-10-29T10:25:00Z'
labels: []
dependencies: []
documentation:
  - specs/rfes/migrate-beads-to-backlog.md
type: chore
ordinal: 1000
---

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Import existing tasks, simplify AGENTS.md, remove tracked Beads setup, and verify.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented in 0b72d6b on migrate-beads-to-backlog-md, workspace juicy-alpacka. Imported all 35 Beads tasks (21 To Do, 14 Done), preserving descriptions, criteria, notes, comments, timestamps and historical metadata. Three discovered-from links are references rather than blockers. Verified imported content against the fresh export at /tmp/levana-beads-migration-research.jsonl and ran backlog doctor successfully. Required assembleDebug, testDebugUnitTest and lintDebug passed. No device check needed: task-tracking files only. Awaiting coordinator integration and push. At integration, remove the shared core.hooksPath if it still points to the removed .beads/hooks directory; it was left intact here to avoid affecting other worktrees before merge. Original shared Beads database remains untouched.
<!-- SECTION:NOTES:END -->
