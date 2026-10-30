---
id: TASK-53.5
title: fdroiddata recipe, local F-Droid validation and docs/fdroid
status: To Do
assignee: []
created_date: '2026-10-30T12:25:00Z'
labels: []
dependencies:
  - TASK-53.1
  - TASK-53.2
  - TASK-53.3
  - TASK-53.4
parent_task_id: TASK-53
priority: medium
type: task
ordinal: 66000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Write `metadata/ninja.doskey.app.levana.yml` for fdroiddata:
- the source repo and the build recipe for the release tag;
- `Binaries: https://github.com/dbrodie/levana/releases/download/v%v/levana-v%v.apk`;
- `AllowedAPKSigningKeys` set to the verified SHA-256;
- licence MIT, a category, and anti-features if any;
- `AutoUpdateMode: Version` and `UpdateCheckMode: Tags` with the right `UpdateCheckData` or version handling. Resolve how versionCode is derived when versions exist only in git tags; prefer no change to tag-driven releases.

Validate locally with fdroidserver, in Docker or a pinned venv, in a fdroiddata fork checkout: `fdroid readmeta`, `fdroid rewritemeta`, `fdroid lint`, `fdroid checkupdates`, `fdroid build` (buildserver image), and the signature-aware comparison against the GitHub APK.

Write `docs/fdroid/README.md` (release and verification flow, key backup and recovery requirements without credentials) and a compliance report: licences, dependencies, anti-features, test results and remaining blockers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 fdroid lint, rewritemeta and checkupdates pass, and fdroid build reproduces and accepts the GitHub APK locally; docs/fdroid/README.md and the compliance report are committed
<!-- AC:END -->
