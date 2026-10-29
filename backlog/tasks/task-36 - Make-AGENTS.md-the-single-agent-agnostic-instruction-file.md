---
id: TASK-36
title: Make AGENTS.md the single agent-agnostic instruction file
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-28T20:25:43Z"
updated_date: "2026-10-29T08:44:52Z"
labels: []
dependencies: []
references:
  - levana-uaa
priority: medium
type: task
ordinal: 36000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Consolidate agent instructions into AGENTS.md: drop CLAUDE.md (Claude Code reads AGENTS.md natively), replace generated Beads/bv blocks with concise project Beads rules plus a .beads/PRIME.md override, add build/test commands, clarify task workflow wording, remove the file list and retire specs/TASKS.md.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 AGENTS.md is the only instruction file; no generated Beads blocks; bd prime output matches project rules; TASKS.md removed with all open rows tracked in Beads.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Worktree majestic-shark (Paseo), branch majestic-shark, base 120b80d.
Spec 2527185. Drafts uncommitted: AGENTS.md rewritten (1060->485 words), .beads/PRIME.md added (bd prime verified uses it), specs/TASKS.md removed (all open rows tracked in Beads). Pending owner: delete CLAUDE.md, memory policy (sidebar), lint step, main-checkout bv block.
Removed CLAUDE.md; ran bd setup cursor --remove and claude --remove (Cursor rules/hooks deleted). Codex integration still present on this branch (main checkout has uncommitted codex removal). Found: no tests use Roborazzi; 2 orphan screenshots.
Ran bd setup codex --remove. Timings (cold daemon): assemble+unit tests 71s pass; lintDebug 52s pass; ktlintCheck fails on ~6k pre-existing violations. Build section now: assemble+test+lint every change, env vars expected in shell.
Removed .beads/PRIME.md per owner. Added tracked .env (JAVA_HOME, ANDROID_HOME); verified documented command builds from a clean env.
Merged to master locally as eccb093 (no-ff). Main checkout's superseded uncommitted edits backed up to /tmp/levana-main-uncommitted-backup.patch and restored. Not pushed; task stays in_progress until push.
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-uaa",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-28T20:25:47Z",
  "closed_at": "2026-10-29T08:44:52Z",
  "close_reason": "Merged eccb093 to master and pushed. AGENTS.md is the single agent instruction file; generated Beads integrations removed; .env added.",
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 2
}
```
