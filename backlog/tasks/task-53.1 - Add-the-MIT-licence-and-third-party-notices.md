---
id: TASK-53.1
title: Add the MIT licence and third-party notices
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T12:24:00Z'
updated_date: '2026-10-30T12:37:00Z'
labels: []
dependencies: []
parent_task_id: TASK-53
priority: high
type: chore
ordinal: 62000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner decision 2026-10-30: Levana is licensed **MIT**. Add a LICENSE file (MIT, copyright holder: Daniel Brodie, with the years of the project), mention the licence in the README if one exists (or create a minimal README with a description, licence and links), and add third-party notices listing the bundled libraries and their licences, e.g. KosherJava (LGPL-2.1) and the AndroidX, Kotlin and Material libraries. Generate the list from the actual dependency graph rather than by hand. Optionally show the licences in About. Confirm that GitHub then detects the licence.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 LICENSE (MIT) is committed and GitHub shows MIT; third-party notices are generated from the real dependency graph and include KosherJava's LGPL-2.1
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. LICENSE at repo root: verbatim MIT text, 'Copyright (c) 2025-2026 Daniel Brodie' (first commit 2025-12-30).
2. README.md (none exists): short factual description from specs/PROJECT_SPEC.md, requirements, install links (GitHub Releases), licence section pointing to LICENSE and THIRD_PARTY_NOTICES.md.
3. scripts/generate_third_party_notices.py: runs ./gradlew :app:dependencies --configuration releaseRuntimeClasspath (the real release graph), parses resolved coordinates, reads each POM (following parent POMs) from the Gradle cache for name, URL and licence, and writes THIRD_PARTY_NOTICES.md. No build-script changes, so no conflict with TASK-53.2; rerun after 53.2 drops play-services-location. Google Play Services / Firebase artifacts are flagged as proprietary, to be removed by TASK-53.2.
4. Commit the generated THIRD_PARTY_NOTICES.md; check that KosherJava shows LGPL-2.1.
5. About: add an 'Open-source licences' ListItem (EN + HE strings) that opens THIRD_PARTY_NOTICES.md on GitHub via ACTION_VIEW. Re-record the settings_about Roborazzi baselines and justify the changed images.
6. Verify: assembleDebug, testDebugUnitTest, lintDebug, verifyRoborazziDebug --rerun-tasks from a clean app/build/test-results. Check that the LICENSE matches GitHub's MIT template (licensee-compatible). Record the results in the notes.
<!-- SECTION:PLAN:END -->
