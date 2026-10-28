# Agent Instructions Cleanup

Beads: `levana-uaa`

## Summary

Make `AGENTS.md` the single, agent-agnostic instruction file and make the Beads guidance agents receive match the project's task workflow.

## Motivation

- Instructions were spread across `CLAUDE.md`, `AGENTS.md`, generated Beads blocks and a generated `bv` block. The copies diverged and repeated each other.
- The generated Beads text and default `bd prime` output tell every agent to close issues and push at session end. The project workflow gives integration, closure and pushing to the coordinator.
- Claude Code reads `AGENTS.md` directly when no `CLAUDE.md` exists, so a separate Claude file is unnecessary.
- `specs/TASKS.md` duplicates Beads, and every open row is tracked there.

## Changes

- `AGENTS.md`: replace the generated Beads and `bv` blocks with a short project Beads section; add build and test commands; clarify verification evidence and the skill name; remove the file list.
- `.beads/PRIME.md`: override `bd prime` with a concise command reference that defers to `AGENTS.md` for workflow rules.
- Remove `specs/TASKS.md`.
- Remove `CLAUDE.md` once the owner confirms.

## Expected outcome

Every agent loads the same instructions, and the Beads context matches the project's completion rules.
