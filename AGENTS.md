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


## Build & Test

`.env` holds the machine paths Gradle needs (`JAVA_HOME`, `ANDROID_HOME`). Load it in the same command as Gradle and run from the worktree root for every change:

```bash
set -a && . ./.env && set +a && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## Task Workflow

- **One Beads task → one agent, branch and worktree/workspace**, created through Paseo. Group only with explicit user instruction; sequence dependent tasks or queue them when concurrency is limited. Do not nest worktrees.
- **Task agent:** claim, plan, commit `specs/rfes/<task>.md` inside its worktree before implementation, then implement, verify and commit on the task branch. Leave merging, pushing, closing and Beads sync to the coordinator.
- **Verification:** the build and test commands above, plus runtime checks with the `android-debug` skill when the change is visible on a device. Each task uses its own emulator. Record the tested commit, results, limitations and evidence in the Beads task notes; keep larger artifacts outside the worktree and reference their paths.
- **Coordinator:** review changes/evidence, integrate sequentially and test the combined result. With authorization: merge to `master` → push/confirm → close/sync Beads → archive workspace/agent → delete merged branch. Do not request approval already granted. Retain the workspace if blocked or awaiting approval.

## Task Tracking

Track all work in the shared project Beads database (`bd`), not in markdown or tool-specific todo lists. This section overrides generic Beads guidance, including the session-close steps printed by `bd prime`.

- Find work with `bd ready` and `bd show <id>`. Claim with `bd update <id> --claim`, then record agent/workspace IDs, branch and base commit.
- Add notes with `bd update <id> --append-notes` at phase changes, blockers and handoff: commits, verification evidence and pending work.
- `open`: queued; `in_progress`: implementation through integration; `blocked`: concrete blocker; `deferred`: postponed; `closed`: verified, merged and pushed. Agent completion alone does not close a task.
- File separate discoveries with `bd create` and link them with `bd dep add`; keep unmet acceptance criteria on the original task unless the user approves a scope change.
- Only the coordinator runs `bd close` and `bd dolt push`, after the merge is pushed, and records merge and cleanup outcomes.
