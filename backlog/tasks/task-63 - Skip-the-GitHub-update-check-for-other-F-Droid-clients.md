---
id: TASK-63
title: Skip the GitHub update check for other F-Droid clients
status: To Do
assignee: []
created_date: '2026-11-03T13:27:00Z'
labels:
  - fdroid
dependencies: []
priority: low
ordinal: 105000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found in TASK-53.5. UpdateSource.fromInstaller only treats org.fdroid.fdroid (and Play) as self-updating. Installs through other F-Droid-repo clients (F-Droid Basic org.fdroid.basic, Droid-ify com.looker.droidify, Neo Store com.machiav3lli.fdroid, and similar) are classified as GitHub and run the daily GitHub Releases API check and update notification, although their client updates the app. Not an F-Droid anti-feature, but a needless network request and a confusing duplicate update prompt. Add the known F-Droid client package names (keep the list small and documented) with unit tests in UpdateSource tests, and update docs/fdroid/COMPLIANCE.md.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Installs by the common F-Droid clients (at least org.fdroid.basic, com.looker.droidify, com.machiav3lli.fdroid) skip the GitHub update check; unit tests cover each
<!-- AC:END -->
