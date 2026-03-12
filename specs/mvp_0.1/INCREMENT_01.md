# Increment 01: Project Skeleton & Hello World

## Summary

Bootstrap the Android project with Gradle Kotlin DSL, version catalog, Koin dependency injection, Material 3 theming, and a minimal "Hello World" screen. Establish the package structure and code quality tooling (ktlint).

## What Will Be Built

### Gradle Build System
- **Root `build.gradle.kts`** — plugin declarations (Android, Kotlin, ktlint)
- **`app/build.gradle.kts`** — application module with Compose, Koin, Material 3 dependencies
- **`settings.gradle.kts`** — project name, repository configuration, version catalog
- **`gradle/libs.versions.toml`** — centralized dependency versions

### Application Class
- `LevanaApplication` extending `Application`
- Koin initialization with an empty app module (ready for future DI declarations)
- Koin Android logger for debug builds

### Main Activity & UI
- `MainActivity` as single entry point with `setContent { }`
- `LevanaTheme` composable wrapping Material 3 `MaterialTheme`
- Dynamic color support on API 31+ with fallback color scheme
- Light and dark theme support following system setting
- Simple screen showing "Levana" title centered, demonstrating the theme

### Package Structure
```
com.levana.app/
├── LevanaApplication.kt
├── MainActivity.kt
├── data/          # (empty — future data layer)
├── domain/        # (empty — future domain layer)
└── ui/
    └── theme/
        ├── Theme.kt
        ├── Color.kt
        └── Type.kt
```

### Code Quality
- ktlint configured via `jlleitschuh/ktlint-gradle` plugin
- `.editorconfig` for Kotlin style settings

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| DI framework | Koin | Per ARCHITECTURE_SPEC — no annotation processing, fast builds |
| Architecture | MVI (foundation) | Per ARCHITECTURE_SPEC — full MVI triad starts in Increment 2 |
| Compose BOM | Latest stable | Single BOM controls all Compose library versions |
| Min SDK | 34 | Per PROJECT_SPEC — Android 14 |
| Target SDK | 35 | Latest stable target |
| ktlint plugin | jlleitschuh/ktlint-gradle | Well-maintained, Gradle integration |
| Package name | `com.levana.app` | Short, memorable, matches project identity |

## Dependencies (libs.versions.toml)

| Library | Purpose |
|---------|---------|
| Kotlin + Kotlin Compose compiler | Language & compiler plugin |
| Compose BOM | UI framework (Material 3, Foundation, UI) |
| Compose Activity | `setContent` integration |
| Koin (core, android, compose) | Dependency injection |
| AndroidX Core KTX | Kotlin extensions |
| Material 3 | Design system |

## Acceptance Criteria

- [ ] App installs on device/emulator running API 34+
- [ ] Shows a screen with "Levana" title text
- [ ] Light and dark themes work (follow system setting)
- [ ] `./gradlew build` passes with no errors
- [ ] `./gradlew ktlintCheck` passes
- [ ] Koin initializes without crash (visible in logcat)
