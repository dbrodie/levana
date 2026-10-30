---
id: TASK-53.4
title: 'Fastlane store metadata (English): descriptions, screenshots, changelogs'
status: To Do
assignee: []
created_date: '2026-10-30T12:25:00Z'
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
