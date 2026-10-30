---
id: TASK-53.3
title: Reproducible release build and an automated rebuild-and-compare check
status: To Do
assignee: []
created_date: '2026-10-30T12:25:00Z'
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
