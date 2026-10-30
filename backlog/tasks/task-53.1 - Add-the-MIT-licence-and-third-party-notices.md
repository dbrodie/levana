---
id: TASK-53.1
title: Add the MIT licence and third-party notices
status: To Do
assignee: []
created_date: '2026-10-30T12:24:00Z'
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
