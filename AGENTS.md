# Levana — AI Agent Guide

## Project

Levana is an open-source Android Hebrew calendar app for Orthodox Jewish users (Diaspora & Israel). It operates fully offline and provides zmanim, Jewish holidays, Torah readings, personal events, and notifications.

- Full product vision: `specs/PROJECT_SPEC.md`
- Technical decisions: `specs/ARCHITECTURE_SPEC.md`

## Critical Frameworks

Do not substitute these — they are architectural commitments.

| Area | Framework |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVI (State / Intent / ViewModel / Screen per screen) |
| DI | Koin |
| Hebrew calendar engine | KosherJava |
| Storage | Room (structured data) + DataStore (preferences) |
| Async | Kotlin Coroutines + Flow |
| Notifications | WorkManager + AlarmManager |
| UI snapshot testing | Roborazzi |

## File Structure

```
levana/
├── app/src/main/java/ninja/doskey/app/levana/
│   ├── ui/                  # Compose screens — each screen has State, Intent, ViewModel, Screen files
│   ├── domain/model/        # Domain models (DayInfo, HebrewDay, Holiday, ZmanTime, etc.)
│   ├── data/                # Repositories, Room DB (LevanaDatabase, PersonalEventDao), HolidayMapper
│   ├── di/                  # Koin modules
│   └── notifications/       # WorkManager workers, AlarmManager scheduler, receivers
├── specs/
│   ├── PROJECT_SPEC.md               # Full product specification
│   ├── ARCHITECTURE_SPEC.md          # Tech stack and architectural decisions
│   ├── MVP_0.1_IMPLEMENTATION_PLAN.md
│   ├── mvp_0.1/                      # Completed increment specs (INCREMENT_01…INCREMENT_15)
│   └── rfes/                         # Feature specs (one file per feature)
└── gradle/libs.versions.toml         # Centralized dependency versions
```

## Feature Development Workflow

1. **Plan** — work in plan mode; research the codebase and design the approach
2. **Create worktree** — `git worktree add .claude/worktrees/<feature-name> -b <feature-name>`
3. **Save spec** — write the spec to `specs/rfes/<feature-name>.md` **inside the worktree** (`.claude/worktrees/<feature-name>/specs/rfes/<feature-name>.md`), never in the main repo
4. **Commit spec** — commit the spec file to the branch before any code
5. **Develop** — implement the feature on the branch inside the worktree; all work happens inside `.claude/worktrees/<feature-name>/`
6. **Approve & merge** — once the feature is approved, merge to `master`
7. **Delete worktree** — `git worktree remove .claude/worktrees/<feature-name>`

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:46cd31e7 -->
## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See https://github.com/gastownhall/beads/blob/main/docs/core-concepts/sync-concepts.md for details and anti-patterns.

## Agent Context Profiles

The managed Beads block is task-tracking guidance, not permission to override repository, user, or orchestrator instructions.

- **Conservative (default)**: Use `bd` for task tracking. Do not run git commits, git pushes, or Dolt remote sync unless explicitly asked. At handoff, report changed files, validation, and suggested next commands.
- **Minimal**: Keep tool instruction files as pointers to `bd prime`; use the same conservative git policy unless active instructions say otherwise.
- **Team-maintainer**: Only when the repository explicitly opts in, agents may close beads, run quality gates, commit, and push as part of session close. A current "do not commit" or "do not push" instruction still wins.

## Session Completion

This protocol applies when ending a Beads implementation workflow. It is subordinate to explicit user, repository, and orchestrator instructions.

1. **File issues for remaining work** - Create beads for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **Handle git/sync by active profile**:
   ```bash
   # Conservative/minimal/default: report status and proposed commands; wait for approval.
   git status

   # Team-maintainer opt-in only, unless current instructions forbid it:
   git pull --rebase
   bd dolt push
   git push
   git status
   ```
5. **Hand off** - Summarize changes, validation, issue status, and any blocked sync/commit/push step

**Critical rules:**
- Explicit user or orchestrator instructions override this Beads block.
- Do not commit or push without clear authority from the active profile or the current user request.
- If a required sync or push is blocked, stop and report the exact command and error.
<!-- END BEADS INTEGRATION -->

<!-- BEGIN BEADS CODEX SETUP: generated by bd setup codex -->
## Beads Issue Tracker

Use Beads (`bd`) for durable task tracking in repositories that include it. Use the `beads` skill at `.agents/skills/beads/SKILL.md` (project install) or `~/.agents/skills/beads/SKILL.md` (global install) for Beads workflow guidance, then use the `bd` CLI for issue operations.

### Quick Reference

```bash
bd ready                # Find available work
bd show <id>            # View issue details
bd update <id> --claim  # Claim work
bd close <id>           # Complete work
bd prime                # Refresh Beads context
```

### Rules

- Use `bd` for all task tracking; do not create markdown TODO lists.
- Run `bd prime` when Beads context is missing or stale. Codex 0.129.0+ can load Beads context automatically through native hooks; use `/hooks` to inspect or toggle them.
- Keep persistent project memory in Beads via `bd remember`; do not create ad hoc memory files.

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See https://github.com/gastownhall/beads/blob/main/docs/core-concepts/sync-concepts.md for details and anti-patterns.
<!-- END BEADS CODEX SETUP -->
