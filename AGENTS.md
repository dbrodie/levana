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

## Task Workflow

- **One Beads task → one agent, branch and worktree/workspace**, created through Paseo. Group only with explicit user instruction; sequence dependent tasks or queue them when concurrency is limited. Do not nest worktrees.
- **Task agent:** claim, plan, commit `specs/rfes/<task>.md` inside its worktree before implementation, then implement, verify and commit. The coordinator owns integration and closure.
- **Verification:** tests, lint/build checks and relevant runtime checks using the Android debugging skill. Each task owns its emulator. Record tested commits, results, limitations and evidence outside disposable worktrees.
- **Coordinator:** review changes/evidence, integrate sequentially and test the combined result. With authorization: merge to `master` → push/confirm → close/sync Beads → archive workspace/agent → delete merged branch. Do not request approval already granted. Retain the workspace if blocked or awaiting approval.

## Task Tracking

Use the shared project Beads database (`bd`); run `bd prime` for CLI guidance.

- Claim with `bd update <id> --claim`; record agent/workspace IDs, branch and base commit. Refresh active claims.
- Update status and append notes at phase changes, blockers and handoff, including commits, verification evidence and pending work.
- `open`: queued; `in_progress`: implementation through integration; `blocked`: concrete blocker; `deferred`: postponed; `closed`: verified, merged and pushed. Agent completion alone does not close a task.
- File separate discoveries as linked tasks; retain unmet acceptance criteria on the original task unless the user approves a scope change. Record merge and cleanup outcomes.
