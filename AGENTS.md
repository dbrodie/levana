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

- **One task → one agent, branch and worktree/workspace**, created through Paseo. Group only with explicit user instruction; sequence dependent tasks or queue them when concurrency is limited. Do not nest worktrees.
- **Task agent:** claim, plan, commit `specs/rfes/<task>.md` inside its worktree before implementation, then implement, verify and commit on the task branch. Leave merging, pushing and completion to the coordinator.
- **Verification:** the build and test commands above, plus runtime checks with the `android-debug` skill when the change is visible on a device. Each task uses its own emulator. Record the tested commit, results, limitations and evidence in the task notes; keep larger artifacts outside the worktree and reference their paths.
- **Coordinator:** review changes/evidence, integrate sequentially and test the combined result. With authorization: merge to `master` → push/confirm → mark the task Done and push the task update → archive workspace/agent → delete merged branch. Do not request approval already granted. Retain the workspace if blocked or awaiting approval.

## Task Tracking

Use Backlog.md (`backlog`) for all task tracking. Tasks live in `backlog/` and are committed with the work. Install the CLI with `npm install -g backlog.md`, or use `npx backlog.md`.

- Run `backlog instructions overview` for usage guidance; this file governs the project workflow.
- Find work with `backlog task list --plain` and read it with `backlog task <id> --plain`.
- Assign the task and mark it `In Progress` when starting. Keep plans, progress, verification evidence and handoff notes in the task; link its RFE in `specs/rfes/`.
- File new discoveries as tasks, using dependencies only for work that actually blocks them.
- Only the coordinator marks tasks `Done`, after verification, integration and a confirmed push. Agent completion alone does not mark a task Done.
