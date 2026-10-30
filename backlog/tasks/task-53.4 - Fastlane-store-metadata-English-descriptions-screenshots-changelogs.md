---
id: TASK-53.4
title: 'Fastlane store metadata (English): descriptions, screenshots, changelogs'
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T12:25:00Z'
updated_date: '2026-10-30T12:42:00Z'
labels: []
dependencies: []
parent_task_id: TASK-53
priority: medium
type: chore
ordinal: 65000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner decision: English only. Add `fastlane/metadata/android/en-US/` with title, short_description, full_description (accurate to PROJECT_SPEC; no invented claims), the icon, phone screenshots (and optionally 7- and 10-inch ones, or reuse foldable inner-display shots from TASK-52), and `changelogs/<versionCode>.txt`. Add a changelog step to the release process: each tag needs `changelogs/<versionCode>.txt`, enforced by a check in the release workflow. The owner approves the description text before submission.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Fastlane metadata follows fdroid conventions (lengths, paths); screenshots are real app captures; the release process requires a per-version changelog; the owner approved the listing text
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Source: Play listing export in /tmp/levana-play-listing (title/short/full text, icon, feature graphic, 3 phone screenshots). Check emoji code points and lengths.
2. Verify every claim in the full description against the current app code (master 2d5cec6) and specs; change only inaccurate wording (e.g. minhag selector was removed in TASK-4) and list each change with its reason. Suggest, but do not apply, additions for features missing from the text (Agenda, widgets, appearance).
3. Write fastlane/metadata/android/en-US/{title,short_description,full_description}.txt (title<=50, short<=80, full<=4000).
4. Copy icon.png (512) and featureGraphic.png (1024x500); check the feature graphic's tagline is accurate.
5. Screenshots: build debug, run on a dedicated phone emulator, compare with the Oct 18 Play shots. If outdated, capture a fresh consistent English light set (plus dark/Hebrew if useful) via android-debug, keeping the originals' order and intent; store in images/phoneScreenshots/ as 1.png, 2.png, ... Record which set and why; recommend whether to refresh Play.
6. Changelogs: changelogs/401.txt and 402.txt from git log/backlog (v0.4.2 has no app changes vs v0.4.1).
7. Release workflow: an early step that fails when fastlane/metadata/android/en-US/changelogs/<versionCode>.txt is missing, with a clear ::error message; document in specs/rfes/release-process.md. No tags pushed.
8. Validate against F-Droid fastlane conventions (paths, sizes, lengths); run fdroid lint only if cheaply available, else defer to TASK-53.5.
9. Run assembleDebug/testDebugUnitTest/lintDebug --rerun-tasks; record evidence, final text, diff from Play and the screenshot set in the notes for owner approval; commit on the branch.
<!-- SECTION:PLAN:END -->
