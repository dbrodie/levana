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
├── app/src/main/java/com/levana/app/
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
2. **Save spec** — write the spec to `specs/rfes/<feature-name>.md`
3. **Create worktree** — `git worktree add .claude/worktrees/<feature-name> -b <feature-name>`
4. **Work in worktree** — all development happens inside `.claude/worktrees/<feature-name>/`
5. **Commit spec** — commit the spec file to the branch before any code
6. **Develop** — implement the feature on the branch inside the worktree
7. **Approve & merge** — once the feature is approved, commit and merge to `master`
8. **Delete worktree** — `git worktree remove .claude/worktrees/<feature-name>`
