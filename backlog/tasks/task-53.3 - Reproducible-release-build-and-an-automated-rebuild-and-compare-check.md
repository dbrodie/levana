---
id: TASK-53.3
title: Reproducible release build and an automated rebuild-and-compare check
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T12:25:00Z'
updated_date: '2026-10-31T18:58:00Z'
labels: []
dependencies:
  - TASK-53.2
parent_task_id: TASK-53
priority: high
type: chore
ordinal: 64000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
- Pin the build: the exact JDK (Temurin 17.0.x) in release.yml and documented for F-Droid; Gradle, AGP, SDK and build-tools versions, the Kotlin compiler and dependencies (consider Gradle dependency verification or locking).
- Remove nondeterminism: check baseline profiles (assets/dexopt/baseline.prof and .profm), R8 output, resources, timestamps, file order and embedded paths.
- Add a **verification job** that runs without any secrets: check out the exact tag, build the unsigned release APK in a clean environment (ideally F-Droid's buildserver image or an equivalent Docker image), download the signed `levana-v<ver>.apk` from the GitHub Release and run `apksigcopier compare --unsigned`. On a mismatch, run diffoscope and upload the report as an artifact. Failures must be visible on the release. Also check the certificate SHA-256 (it must equal the Play app-signing certificate; record it), versionCode and package ID.
- Keep signing secrets only in the tag-triggered release job; never give secrets to PR builds.
- Prove it on a real tag: either a pre-release tag (owner authorization required before pushing any tag) or by re-verifying the existing v0.4.2 APK if it is already reproducible.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An independent unsigned rebuild of a release tag matches the signed GitHub APK under apksigcopier compare; the CI job reports pass/fail and attaches diffoscope output on failure
- [ ] #2 The signing certificate SHA-256 is recorded and equals the Play app-signing certificate; no secrets are reachable from non-release workflows
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Tooling outside the repo: venv (/tmp/levana-task-53-3/venv) with apksigcopier + diffoscope (+ brew actionlint). Evidence dir /tmp/levana-task-53-3 with README.
2. Baseline: download the v0.4.2 GitHub APK; record cert SHA-256, package, versionCode (aapt2/apksigner). Rebuild the v0.4.2 tag unsigned (detached worktree outside the repo, VERSION_NAME=0.4.2, no keystore) and run apksigcopier compare + diffoscope; explain any mismatch (expected: dependency-info block, possibly other content).
3. Fallback signing: when no release keystore is configured, the release build must produce an UNSIGNED APK (F-Droid style), not debug-signed. Keep the debug fallback only behind an explicit opt-in if needed for local installs (check current users).
4. Pin the toolchain: Gradle wrapper distributionSha256Sum; explicit buildToolsVersion; AGP/Kotlin/compileSdk already pinned in the catalog; exact Temurin 17.0.x in release.yml via setup-java; consider gradle/verification-metadata.xml (sha256) for dependencies.
5. Reproducibility of master: build release twice (different dirs, times; local JBR vs Temurin 17) and once in a Debian bookworm container with Temurin 17 + SDK (F-Droid-like). Compare with apksigcopier compare --unsigned / diffoscope; fix nondeterminism (baseline.prof/.profm, VCS info META-INF/version-control-info.textproto, timestamps, paths, R8 map ids).
6. Signature-aware proof: sign one build with a throwaway key (apksigner), then apksigcopier compare signed vs unsigned rebuild.
7. CI: new .github/workflows/verify-release.yml, no secrets, permissions contents: read (+ actions write only if needed for artifacts); triggers workflow_run on Release completed (tag) and workflow_dispatch(tag). Checkout the tag, pinned JDK, assembleRelease unsigned, download levana-v<ver>.apk via gh, check package/versionCode/versionName/cert SHA-256 (expected value recorded in repo), apksigcopier compare; on mismatch diffoscope and upload artifact, fail. Pin release.yml JDK too. Validate with actionlint.
8. Docs: docs/fdroid/README.md (how it works, manual verification, pinned toolchain, key backup/recovery without credentials); update specs/rfes/release-process.md.
9. Verify: assembleDebug assembleRelease testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks; record evidence and owner steps for a real tag proof in the notes; commit. No push/tag/release.
<!-- SECTION:PLAN:END -->
