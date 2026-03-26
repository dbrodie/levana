# RFE: Android Devcontainer for Levana

## Goal

Provide a reproducible development container so any developer (or AI agent) can
compile, lint, and run unit/snapshot tests without a local Android SDK or JDK
installation. The emulator stays on the host; the container connects to it via
ADB over TCP.

## Constraints

| Item | Value |
|---|---|
| JVM target | Java 17 |
| Gradle | 8.11.1 (wrapper) |
| AGP | 8.7.3 |
| Kotlin | 2.1.0 |
| compileSdk / targetSdk | 35 |
| Build tools | 35.0.0 |
| Unit test runner | Robolectric 4.14.1 + Roborazzi 1.59.0 |
| Robolectric graphics mode | `NATIVE` (requires native GL libs on Linux) |
| JVM heap | `-Xmx2048m` (gradle.properties) |

## Platform constraint

Android SDK build tools do not ship `linux/arm64` binaries, so the container
must run as `linux/amd64`. On Apple Silicon this requires Colima to be started
with an x86_64 VM:

```bash
colima start --arch x86_64 --cpu 4 --memory 8 --disk 60
```

## Files

- `.devcontainer/Dockerfile` — two-stage build (`claude-base` + `android-dev`)
- `.devcontainer/devcontainer.json` — VS Code devcontainer configuration

## Dockerfile stages

### Stage 1 — `claude-base`

Mirrors Anthropic's reference Dockerfile (`node:20` base):
- apt tools: git, zsh, fzf, gh, iptables, ipset, iproute2, dnsutils, jq, nano,
  vim, etc.
- npm global dir owned by `node` user
- bash history persistence
- `DEVCONTAINER=true` env var
- git-delta
- zsh-in-docker (Powerline10k)
- Claude Code npm package

### Stage 2 — `android-dev`

Adds Android tooling on top of `claude-base`:
- `openjdk-17-jdk` (Debian Bookworm)
- `libgl1 libegl1 libgles2` — native GL libs required by Robolectric `NATIVE`
  graphics mode
- Android SDK at `/opt/android-sdk`:
  - cmdline-tools 11.0 (build 11076708)
  - `platform-tools`, `platforms;android-35`, `build-tools;35.0.0`
- Env vars: `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `JAVA_HOME`, `PATH` extensions
- `/opt/android-sdk` owned by `node` user

**Upgrade path**: when Anthropic updates their Dockerfile, update only the
`claude-base` stage. The `android-dev` stage is independent.

## Workspace / worktree mount strategy

Mounting a git worktree directory directly as the devcontainer workspace breaks
git inside the container: a worktree's `.git` is a *file* containing a relative
path back to the main repo's `.git`, which doesn't resolve inside the container.

**Correct approach**: mount the entire repo root into the container. All
worktrees live inside that mount, so the relative `.git` path resolves
correctly.

```
Host:  /Users/daniel/projects/levana/      ← repo root → /workspace
         .git/                              ← main git dir, always accessible
         .devcontainer/
         .claude/worktrees/feature-a/       ← .git file resolves correctly
         .claude/worktrees/feature-b/
```

VS Code opens the repo root → finds `.devcontainer/` → mounts
`${localWorkspaceFolder}` at `/workspace`.

## devcontainer.json highlights

- `build.target`: `android-dev`
- `build.options`: `["--platform=linux/amd64"]`
- `runArgs`: `["--add-host=host.docker.internal:host-gateway"]` (ADB wireless)
- `remoteUser`: `node`
- `workspaceMount`: repo root → `/workspace`
- Named volumes:
  - `levana-gradle-cache` → `/home/node/.gradle` (avoid re-downloading deps)
  - `levana-android-home` → `/home/node/.android` (persist debug keystore)
- `postCreateCommand`: generate debug keystore if absent, then
  `./gradlew --version` to warm Gradle

## ADB wireless workflow

From inside the container:

```bash
# Emulator (default ADB port 5555)
adb connect host.docker.internal:5555

# Physical device — first run on host: adb tcpip 5555
adb connect host.docker.internal:5555

adb devices
```

## Verification checklist

- [x] `./gradlew assembleDebug` — no SDK-not-found errors
- [x] `./gradlew ktlintCheck`
- [x] `./gradlew testDebugUnitTest` — Roborazzi NATIVE mode works (no
      `UnsatisfiedLinkError`)
- [x] `adb connect host.docker.internal:5555` + `adb devices` shows host
      emulator/device
- [x] `./gradlew installDebug` deploys APK to ADB target
