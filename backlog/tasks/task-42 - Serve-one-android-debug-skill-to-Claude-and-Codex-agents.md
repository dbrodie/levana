---
id: TASK-42
title: Serve one android-debug skill to Claude and Codex agents
status: Done
assignee:
  - '@claude'
created_date: '2026-10-29T20:54:00Z'
updated_date: '2026-10-29T20:59:00Z'
labels:
  - tooling
dependencies: []
ordinal: 48000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Claude Code agents load a stale, untracked copy at the main checkout's .claude/skills/android-debug/SKILL.md (March 2026, package com.levana.app) that says 'Never take screenshots'. Codex agents load the tracked .agents/skills/android-debug, which supports screenshots and UI XML. Agents therefore report that the skill forbids screenshots.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Claude Code and Codex load the same tracked android-debug skill
- [x] #2 The skill permits both screenshots and UI XML dumps
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Track .claude/skills/android-debug as a relative symlink to ../../.agents/skills/android-debug; narrow .gitignore so only that path under .claude/ is tracked. Main checkout's untracked stale copy must be removed (owner approval) before merging, or checkout will refuse to overwrite it.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Verified: fresh 'claude -p' session in this worktree lists only the current android-debug skill (description mentions screenshots). Symlink resolves SKILL.md, scripts/, references/. Gradle build/test not run: no app code changed.
Before merge: the main checkout (/Users/daniel/projects/levana) still has the untracked stale .claude/skills/android-debug/SKILL.md. It must be removed (owner approval) or 'git merge' there will refuse to overwrite it, and Claude agents started from the main checkout keep seeing 'Never take screenshots'.

2026-10-29: owner removed the stale copy from the main checkout; .claude/skills there is now empty, so the merge can add the symlink.

Merged to master as 9575adb and pushed.
<!-- SECTION:NOTES:END -->
